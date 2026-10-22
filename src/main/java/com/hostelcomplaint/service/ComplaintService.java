package com.hostelcomplaint.service;

import com.hostelcomplaint.dto.ComplaintRequest;
import com.hostelcomplaint.dto.ComplaintResponse;
import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.entity.Assignment;
import com.hostelcomplaint.entity.Complaint;
import com.hostelcomplaint.repository.AppUserRepository;
import com.hostelcomplaint.repository.ComplaintRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AppUserRepository userRepository;
    private final ComplaintNotificationService notificationService;
    private final ComplaintEventPublisher eventPublisher;
    private final AssignmentService assignmentService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            AppUserRepository userRepository,
            ComplaintNotificationService notificationService,
            ComplaintEventPublisher eventPublisher,
            AssignmentService assignmentService) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.assignmentService = assignmentService;
    }

    // Create a complaint owned by the authenticated student.
    public ComplaintResponse createComplaint(
            ComplaintRequest request,
            Authentication authentication) {

        AppUser student = getCurrentUser(authentication);

        if (!"STUDENT".equals(student.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can submit complaints");
        }

        Complaint complaint = new Complaint();
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setCategory(request.getCategory());

        if (request.getSeverity() == null
                || request.getSeverity().isBlank()) {
            throw new IllegalArgumentException(
                    "Severity must be LOW, MEDIUM, HIGH or CRITICAL.");
        }

        String severity = request.getSeverity().trim().toUpperCase();

        complaint.setSeverity(severity);
        complaint.setPriority(calculatePriority(severity));
        complaint.setStatus("SUBMITTED");
        complaint.setStudent(student);

        // Preserve the existing creation and assignment workflow.
        Complaint savedComplaint = complaintRepository.save(complaint);

        notificationService.sendComplaintUpdate(
                "COMPLAINT_CREATED", savedComplaint);

        eventPublisher.publishComplaintEvent(
                "COMPLAINT_CREATED", savedComplaint.getComplaintId());

        assignmentService.assignComplaintAutomatically(
                savedComplaint.getComplaintId());

        Complaint updatedComplaint = complaintRepository
                .findById(savedComplaint.getComplaintId())
                .orElseThrow(() -> new RuntimeException(
                        "Complaint not found after creation: "
                                + savedComplaint.getComplaintId()));

        return convertToResponse(updatedComplaint);
    }

    private String calculatePriority(String severity) {
        switch (severity) {
            case "CRITICAL":
                return "CRITICAL";
            case "HIGH":
                return "HIGH";
            case "MEDIUM":
                return "MEDIUM";
            case "LOW":
                return "LOW";
            default:
                throw new IllegalArgumentException(
                        "Invalid severity. Use LOW, MEDIUM, HIGH or CRITICAL.");
        }
    }

    private ComplaintResponse convertToResponse(Complaint complaint) {
        ComplaintResponse response = new ComplaintResponse();

        response.setComplaintId(complaint.getComplaintId());
        response.setTitle(complaint.getTitle());
        response.setDescription(complaint.getDescription());
        response.setCategory(complaint.getCategory());
        response.setSeverity(complaint.getSeverity());
        response.setPriority(complaint.getPriority());
        response.setStatus(complaint.getStatus());
        response.setCreatedAt(complaint.getCreatedAt());

        return response;
    }

    // Role-filtered complaint listing for the API.
    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaints(
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        switch (user.getRole()) {
            case "ADMIN":
            case "WARDEN":
                return complaintRepository.findAll();

            case "STUDENT":
                return complaintRepository.findAllByStudent_Id(user.getId());

            case "STAFF":
                if (user.getStaffId() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "This account is not linked to a staff profile");
                }

                return assignmentService
                        .getAssignmentsForStaff(user.getStaffId())
                        .stream()
                        .map(Assignment::getComplaint)
                        .filter(Objects::nonNull)
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        Complaint::getComplaintId,
                                        complaint -> complaint,
                                        (first, duplicate) -> first,
                                        java.util.LinkedHashMap::new))
                        .values()
                        .stream()
                        .toList();

            default:
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "This role cannot view complaints");
        }
    }

    // Keep this method for internal service compatibility.
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // A complaint can only be retrieved by an authorized role.
    @Transactional(readOnly = true)
    public Complaint getComplaintById(
            Long id,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);
        Complaint complaint = findComplaint(id);

        switch (user.getRole()) {
            case "ADMIN":
            case "WARDEN":
                return complaint;

            case "STUDENT":
                if (complaint.getStudent() != null
                        && Objects.equals(
                                complaint.getStudent().getId(),
                                user.getId())) {
                    return complaint;
                }
                break;

            case "STAFF":
                if (user.getStaffId() != null) {
                    boolean assignedToStaff = assignmentService
                            .getAssignmentsForComplaint(id)
                            .stream()
                            .anyMatch(assignment ->
                                    assignment.getStaff() != null
                                    && Objects.equals(
                                            assignment.getStaff().getStaffId(),
                                            user.getStaffId()));

                    if (assignedToStaff) {
                        return complaint;
                    }
                }
                break;

            default:
                break;
        }

        // Return 404 to avoid revealing another student's complaint.
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Complaint not found");
    }

    // Keep this method for internal service compatibility.
    public Complaint getComplaintById(Long id) {
        return findComplaint(id);
    }

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Complaint not found: " + id));
    }

    // Restrict general status changes at the controller as well.
    @Transactional
    public Complaint updateComplaintStatus(Long id, String status) {

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Status must not be empty.");
        }

        String normalizedStatus = status.trim().toUpperCase();

        if (!List.of(
                "SUBMITTED",
                "ASSIGNED",
                "IN_PROGRESS",
                "RESOLVED",
                "VERIFIED",
                "CLOSED",
                "REOPENED"
        ).contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status: " + status);
        }

        Complaint complaint = findComplaint(id);
        complaint.setStatus(normalizedStatus);

        Complaint savedComplaint = complaintRepository.save(complaint);

        notificationService.sendComplaintUpdate(
                "COMPLAINT_STATUS_UPDATED", savedComplaint);

        eventPublisher.publishComplaintEvent(
                "COMPLAINT_STATUS_UPDATED",
                savedComplaint.getComplaintId());

        return savedComplaint;
    }

    // Only the student who owns this complaint can verify it.
    @Transactional
    public Complaint verifyComplaint(
            Long id,
            Authentication authentication) {

        requireStudentOwner(id, authentication);
        return verifyComplaint(id);
    }

    // Retained for internal service compatibility.
    @Transactional
    public Complaint verifyComplaint(Long id) {

        Complaint complaint = findComplaint(id);

        if (!"RESOLVED".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalStateException(
                    "Only a RESOLVED complaint can be verified. "
                            + "Current status: " + complaint.getStatus());
        }

        complaint.setStatus("VERIFIED");
        Complaint savedComplaint = complaintRepository.save(complaint);

        notificationService.sendComplaintUpdate(
                "COMPLAINT_VERIFIED", savedComplaint);

        eventPublisher.publishComplaintEvent(
                "COMPLAINT_VERIFIED", savedComplaint.getComplaintId());

        return savedComplaint;
    }

    // Only the student who owns this complaint can reopen it.
    @Transactional
    public Complaint reopenComplaint(
            Long id,
            Authentication authentication) {

        requireStudentOwner(id, authentication);
        return reopenComplaint(id);
    }

    // Retained for internal service compatibility.
    @Transactional
    public Complaint reopenComplaint(Long id) {

        Complaint complaint = findComplaint(id);
        String currentStatus = complaint.getStatus();

        if (!"RESOLVED".equalsIgnoreCase(currentStatus)
                && !"VERIFIED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException(
                    "Only a RESOLVED or VERIFIED complaint can be reopened. "
                            + "Current status: " + currentStatus);
        }

        complaint.setStatus("REOPENED");
        Complaint savedComplaint = complaintRepository.save(complaint);

        notificationService.sendComplaintUpdate(
                "COMPLAINT_REOPENED", savedComplaint);

        eventPublisher.publishComplaintEvent(
                "COMPLAINT_REOPENED", savedComplaint.getComplaintId());

        return savedComplaint;
    }

    private void requireStudentOwner(
            Long complaintId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        if (!"STUDENT".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can verify or reopen complaints");
        }

        Complaint complaint = findComplaint(complaintId);

        if (complaint.getStudent() == null
                || !Objects.equals(
                        complaint.getStudent().getId(), user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Complaint not found");
        }
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));
    }
}