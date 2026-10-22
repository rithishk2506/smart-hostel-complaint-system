
package com.hostelcomplaint.service;

import com.hostelcomplaint.dto.SlaEscalationResponse;
import com.hostelcomplaint.entity.Complaint;
import com.hostelcomplaint.entity.SlaEscalation;
import com.hostelcomplaint.repository.ComplaintRepository;
import com.hostelcomplaint.repository.SlaEscalationRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SlaMonitoringService {

    private final ComplaintRepository complaintRepository;
    private final SlaEscalationRepository escalationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SystemNotificationService systemNotificationService;

    public SlaMonitoringService(
            ComplaintRepository complaintRepository,
            SlaEscalationRepository escalationRepository,
            SimpMessagingTemplate messagingTemplate,
            SystemNotificationService systemNotificationService) {

        this.complaintRepository = complaintRepository;
        this.escalationRepository = escalationRepository;
        this.messagingTemplate = messagingTemplate;
        this.systemNotificationService = systemNotificationService;
    }

    /*
     * Check SLA deadlines every 60 seconds.
     * Each complaint is escalated at most once.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void monitorSlaDeadlines() {

        LocalDateTime now = LocalDateTime.now();

        List<Complaint> complaints = complaintRepository.findAll();

        for (Complaint complaint : complaints) {

            String currentStatus = complaint.getStatus();

            if (currentStatus == null) {
                continue;
            }

            String normalizedStatus =
                    currentStatus.trim().toUpperCase();

            // Do not escalate completed complaints.
            if (List.of("RESOLVED", "VERIFIED", "CLOSED")
                    .contains(normalizedStatus)) {
                continue;
            }

            if (complaint.getCreatedAt() == null) {
                continue;
            }

            Long complaintId = complaint.getComplaintId();

            // Prevent duplicate escalation records and breach notifications.
            if (escalationRepository
                    .existsByComplaint_ComplaintId(complaintId)) {
                continue;
            }

            // Calculate the deadline from the original complaint creation time.
            int slaHours = getSlaHours(complaint.getSeverity());

            LocalDateTime deadline =
                    complaint.getCreatedAt().plusHours(slaHours);

            // Do not escalate before the actual deadline.
            if (!now.isAfter(deadline)) {
                continue;
            }

            SlaEscalation escalation = new SlaEscalation();
            escalation.setComplaint(complaint);
            escalation.setSlaDeadline(deadline);
            escalation.setEscalatedAt(now);
            escalation.setStatus("OPEN");
            escalation.setReason(
                    "SLA breached. Severity: "
                    + complaint.getSeverity()
                    + "; deadline: "
                    + deadline);

            SlaEscalation saved =
                    escalationRepository.saveAndFlush(escalation);

            // Save a persistent, unread notification for the warden.
            systemNotificationService.saveWardenSlaNotification(saved);

            // Preserve the existing real-time WebSocket event.
            Map<String, Object> event = new LinkedHashMap<>();

            event.put("eventType", "SLA_ESCALATED");
            event.put("escalationId", saved.getEscalationId());
            event.put("complaintId", complaintId);
            event.put("title", complaint.getTitle());
            event.put("severity", complaint.getSeverity());
            event.put("complaintStatus", complaint.getStatus());
            event.put("slaDeadline", deadline.toString());
            event.put("escalatedAt", now.toString());
            event.put("escalationStatus", saved.getStatus());
            event.put("reason", saved.getReason());
            event.put(
                    "message",
                    "Complaint #" + complaintId
                    + " has breached its SLA and requires warden attention.");

            messagingTemplate.convertAndSend(
                    "/topic/complaints",
                    (Object) event);
        }
    }

    /*
     * SLA limits based on severity.
     */
    public int getSlaHours(String severity) {

        if (severity == null) {
            return 24;
        }

        return switch (severity.trim().toUpperCase()) {
            case "CRITICAL" -> 1;
            case "HIGH" -> 4;
            case "MEDIUM" -> 24;
            case "LOW" -> 72;
            default -> 24;
        };
    }

    /*
     * Return escalation records, newest first.
     */
    @Transactional(readOnly = true)
    public List<SlaEscalationResponse> getAllEscalations() {

        return escalationRepository
                .findAllByOrderByEscalatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private SlaEscalationResponse convertToResponse(
            SlaEscalation escalation) {

        Complaint complaint = escalation.getComplaint();

        SlaEscalationResponse response =
                new SlaEscalationResponse();

        response.setEscalationId(escalation.getEscalationId());
        response.setComplaintId(complaint.getComplaintId());
        response.setComplaintTitle(complaint.getTitle());
        response.setSeverity(complaint.getSeverity());
        response.setComplaintStatus(complaint.getStatus());
        response.setSlaDeadline(escalation.getSlaDeadline());
        response.setEscalatedAt(escalation.getEscalatedAt());
        response.setReason(escalation.getReason());
        response.setStatus(escalation.getStatus());

        return response;
    }

    /*
     * Update an escalation's status when the warden takes action.
     *
     * Allowed transitions:
     * OPEN -> ACKNOWLEDGED
     * OPEN -> HANDLED
     * ACKNOWLEDGED -> HANDLED
     *
     * Selecting the current status again is a no-op.
     * HANDLED cannot transition back to OPEN or ACKNOWLEDGED.
     */
    @Transactional
    public SlaEscalationResponse updateEscalationStatus(
            Long escalationId,
            String newStatus) {

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException(
                    "Escalation status is required.");
        }

        String normalizedStatus = newStatus.trim().toUpperCase();

        if (!List.of("OPEN", "ACKNOWLEDGED", "HANDLED")
                .contains(normalizedStatus)) {

            throw new IllegalArgumentException(
                    "Invalid status. Use OPEN, ACKNOWLEDGED, or HANDLED.");
        }

        SlaEscalation escalation = escalationRepository
                .findById(escalationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Escalation not found: " + escalationId));

        String previousStatus = escalation.getStatus() == null
                ? ""
                : escalation.getStatus().trim().toUpperCase();

        // Selecting the current status again must not create
        // another database notification.
        if (normalizedStatus.equals(previousStatus)) {
            return convertToResponse(escalation);
        }

        // Allow forward progress only.
        boolean validTransition =
                ("OPEN".equals(previousStatus)
                        && ("ACKNOWLEDGED".equals(normalizedStatus)
                            || "HANDLED".equals(normalizedStatus)))
                || ("ACKNOWLEDGED".equals(previousStatus)
                        && "HANDLED".equals(normalizedStatus));

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid escalation status transition: "
                    + previousStatus + " -> " + normalizedStatus
                    + ". Escalation statuses cannot move backwards.");
        }

        escalation.setStatus(normalizedStatus);

        SlaEscalation saved =
                escalationRepository.saveAndFlush(escalation);

        // Persist a warden notification for the genuine status change.
        systemNotificationService
                .saveWardenEscalationStatusNotification(
                        saved,
                        previousStatus);

        Complaint complaint = saved.getComplaint();

        Map<String, Object> event = new LinkedHashMap<>();

        event.put("eventType", "SLA_ESCALATION_STATUS_UPDATED");
        event.put("escalationId", saved.getEscalationId());
        event.put("complaintId", complaint.getComplaintId());
        event.put("title", complaint.getTitle());
        event.put("severity", complaint.getSeverity());
        event.put("complaintStatus", complaint.getStatus());
        event.put("escalationStatus", saved.getStatus());
        event.put(
                "message",
                "Escalation #" + saved.getEscalationId()
                + " is now " + saved.getStatus());

        // Continue broadcasting the live dashboard event.
        messagingTemplate.convertAndSend(
                "/topic/complaints",
                (Object) event);

        return convertToResponse(saved);
    }
}
