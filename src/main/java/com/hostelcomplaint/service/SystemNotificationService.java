
package com.hostelcomplaint.service;

import com.hostelcomplaint.entity.Assignment;
import com.hostelcomplaint.entity.Complaint;
import com.hostelcomplaint.entity.SlaEscalation;
import com.hostelcomplaint.entity.StaffProfile;
import com.hostelcomplaint.entity.SystemNotification;
import com.hostelcomplaint.repository.SystemNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SystemNotificationService {

    private static final String STAFF = "STAFF";
    private static final String WARDEN = "WARDEN";

    private final SystemNotificationRepository notificationRepository;

    public SystemNotificationService(
            SystemNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Persist a new assignment or reassignment notification for staff.
    @Transactional
    public SystemNotification saveStaffAssignmentNotification(
            Assignment assignment,
            String eventType) {

        if (assignment == null || assignment.getComplaint() == null) {
            throw new IllegalArgumentException(
                    "Assignment and complaint are required.");
        }

        StaffProfile staff = assignment.getStaff();

        if (staff == null || staff.getStaffId() == null) {
            throw new IllegalArgumentException(
                    "A staff member must be assigned before notifying them.");
        }

        Complaint complaint = assignment.getComplaint();

        boolean reassigned =
                "COMPLAINT_REASSIGNED".equalsIgnoreCase(eventType);

        SystemNotification notification = new SystemNotification();

        notification.setRecipientType(STAFF);
        notification.setRecipientId(staff.getStaffId());
        notification.setEventType(eventType);
        notification.setTitle(
                reassigned ? "Work reassigned to you" : "New work assigned");

        notification.setMessage(
                reassigned
                        ? "Complaint #" + complaint.getComplaintId()
                                + " has been reassigned to you: "
                                + complaint.getTitle()
                        : "Complaint #" + complaint.getComplaintId()
                                + " has been assigned to you: "
                                + complaint.getTitle());

        notification.setComplaintId(complaint.getComplaintId());
        notification.setAssignmentId(assignment.getAssignmentId());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // Persist a new SLA breach notification for the warden.
    @Transactional
    public SystemNotification saveWardenSlaNotification(
            SlaEscalation escalation) {

        if (escalation == null || escalation.getComplaint() == null) {
            throw new IllegalArgumentException(
                    "An escalation with a complaint is required.");
        }

        Complaint complaint = escalation.getComplaint();

        SystemNotification notification = new SystemNotification();
        notification.setRecipientType(WARDEN);
        notification.setRecipientId(null);
        notification.setEventType("SLA_ESCALATED");
        notification.setTitle("SLA breach - Action required");
        notification.setMessage(
                "Complaint #" + complaint.getComplaintId()
                        + " has breached its SLA and requires warden attention. "
                        + (escalation.getReason() == null
                                ? ""
                                : escalation.getReason()));

        notification.setComplaintId(complaint.getComplaintId());
        notification.setAssignmentId(null);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // Persist a warden notification when an escalation's status changes.
    @Transactional
    public SystemNotification saveWardenEscalationStatusNotification(
            SlaEscalation escalation,
            String previousStatus) {

        if (escalation == null || escalation.getComplaint() == null) {
            throw new IllegalArgumentException(
                    "An escalation with a complaint is required.");
        }

        Complaint complaint = escalation.getComplaint();
        String currentStatus = escalation.getStatus();

        SystemNotification notification = new SystemNotification();
        notification.setRecipientType(WARDEN);
        notification.setRecipientId(null);
        notification.setEventType("SLA_ESCALATION_STATUS_UPDATED");
        notification.setTitle("Escalation status updated");
        notification.setMessage(
                "Escalation #" + escalation.getEscalationId()
                        + " for complaint #" + complaint.getComplaintId()
                        + " changed from "
                        + (previousStatus == null ? "UNKNOWN" : previousStatus)
                        + " to " + currentStatus + ".");

        notification.setComplaintId(complaint.getComplaintId());
        notification.setAssignmentId(null);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // Get all saved notifications for a staff member, newest first.
    @Transactional(readOnly = true)
    public List<SystemNotification> getStaffNotifications(Long staffId) {
        validateStaffId(staffId);

        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
                        STAFF, staffId);
    }

    // Get unread notifications for a staff member.
    @Transactional(readOnly = true)
    public List<SystemNotification> getUnreadStaffNotifications(
            Long staffId) {
        validateStaffId(staffId);

        return notificationRepository.findUnreadForRecipient(STAFF, staffId);
    }

    // Get all saved warden notifications, newest first.
    @Transactional(readOnly = true)
    public List<SystemNotification> getWardenNotifications() {
        return notificationRepository
                .findByRecipientTypeOrderByCreatedAtDesc(WARDEN);
    }

    // Get unread warden notifications.
    @Transactional(readOnly = true)
    public List<SystemNotification> getUnreadWardenNotifications() {
        return notificationRepository
                .findUnreadByRecipientType(WARDEN);
    }

    // Mark a notification as read after verifying its recipient.
    @Transactional
    public SystemNotification markAsRead(
            Long notificationId,
            String recipientType,
            Long recipientId) {

        if (notificationId == null) {
            throw new IllegalArgumentException(
                    "Notification ID is required.");
        }

        if (recipientType == null || recipientType.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient type is required.");
        }

        String normalizedType = recipientType.trim().toUpperCase();

        if (!STAFF.equals(normalizedType)
                && !WARDEN.equals(normalizedType)) {
            throw new IllegalArgumentException(
                    "Recipient type must be STAFF or WARDEN.");
        }

        if (STAFF.equals(normalizedType)) {
            validateStaffId(recipientId);
        } else if (recipientId != null) {
            throw new IllegalArgumentException(
                    "Warden notifications do not use a recipient ID.");
        }

        SystemNotification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId));

        boolean sameRecipientType = normalizedType.equalsIgnoreCase(
                notification.getRecipientType());

        boolean sameRecipientId = Objects.equals(
                notification.getRecipientId(), recipientId);

        if (!sameRecipientType || !sameRecipientId) {
            throw new IllegalStateException(
                    "This notification does not belong to the specified recipient.");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    private void validateStaffId(Long staffId) {
        if (staffId == null || staffId <= 0) {
            throw new IllegalArgumentException(
                    "A valid staff ID is required.");
        }
    }
}
