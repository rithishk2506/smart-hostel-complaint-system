
package com.hostelcomplaint.service;

import com.hostelcomplaint.entity.Assignment;
import com.hostelcomplaint.entity.Complaint;
import com.hostelcomplaint.entity.StaffProfile;
import com.hostelcomplaint.repository.AssignmentRepository;
import com.hostelcomplaint.repository.ComplaintRepository;
import com.hostelcomplaint.repository.StaffProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final StaffProfileRepository staffRepository;
    private final ComplaintNotificationService notificationService;
    private final SystemNotificationService systemNotificationService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ComplaintRepository complaintRepository,
            StaffProfileRepository staffRepository,
            ComplaintNotificationService notificationService,
            SystemNotificationService systemNotificationService) {

        this.assignmentRepository = assignmentRepository;
        this.complaintRepository = complaintRepository;
        this.staffRepository = staffRepository;
        this.notificationService = notificationService;
        this.systemNotificationService = systemNotificationService;
    }

    // Automatically assign a complaint to eligible available staff.
    @Transactional
    public Assignment assignComplaintAutomatically(Long complaintId) {

        Complaint complaint = getComplaintById(complaintId);

        List<Assignment> previousAssignments =
                assignmentRepository.findByComplaint_ComplaintId(complaintId);

        boolean activeAssignmentExists = previousAssignments.stream()
                .anyMatch(this::isActiveAssignment);

        if (activeAssignmentExists) {
            throw new IllegalStateException(
                    "This complaint already has an active assignment.");
        }

        StaffProfile selectedStaff =
                selectStaffForComplaint(complaint, previousAssignments);

        return createAssignment(complaint, selectedStaff, false);
    }

    // Warden action: reassign a complaint that still requires work.
    @Transactional
    public Assignment reassignComplaintAutomatically(Long complaintId) {

        Complaint complaint = getComplaintById(complaintId);

        String complaintStatus = complaint.getStatus() == null
                ? ""
                : complaint.getStatus().trim().toUpperCase();

        if (!List.of(
                "SUBMITTED",
                "ASSIGNED",
                "IN_PROGRESS",
                "REOPENED"
        ).contains(complaintStatus)) {

            throw new IllegalStateException(
                    "Only an unresolved complaint can be reassigned. "
                    + "Current status: " + complaintStatus);
        }

        List<Assignment> previousAssignments =
                assignmentRepository.findByComplaint_ComplaintId(complaintId);

        List<Assignment> activeAssignments = previousAssignments.stream()
                .filter(this::isActiveAssignment)
                .toList();

        if (activeAssignments.size() > 1) {
            throw new IllegalStateException(
                    "Multiple active assignments were found for complaint "
                    + complaintId
                    + ". Resolve duplicate assignments before reassigning.");
        }

        // Find replacement staff before retiring the current assignment.
        // If no eligible staff are available, the existing assignment remains.
        StaffProfile selectedStaff =
                selectStaffForComplaint(complaint, previousAssignments);

        // Preserve the old assignment record and release its workload.
        for (Assignment previousAssignment : activeAssignments) {

            previousAssignment.setStatus("REASSIGNED");
            assignmentRepository.save(previousAssignment);

            StaffProfile previousStaff = previousAssignment.getStaff();

            if (previousStaff != null
                    && previousStaff.getCurrentWorkload() > 0) {

                previousStaff.setCurrentWorkload(
                        previousStaff.getCurrentWorkload() - 1);

                staffRepository.save(previousStaff);
            }
        }

        // Create the new assignment and persist its notification.
        return createAssignment(complaint, selectedStaff, true);
    }

    // Completed, reassigned and cancelled assignments are historical.
    private boolean isActiveAssignment(Assignment assignment) {

        String status = assignment.getStatus() == null
                ? ""
                : assignment.getStatus().trim().toUpperCase();

        return !List.of(
                "COMPLETED",
                "REASSIGNED",
                "CANCELLED"
        ).contains(status);
    }

    // Select available staff matching the complaint category.
    private StaffProfile selectStaffForComplaint(
            Complaint complaint,
            List<Assignment> previousAssignments) {

        List<StaffProfile> eligibleStaff =
                staffRepository
                        .findBySpecializationIgnoreCaseAndAvailabilityStatusIgnoreCase(
                                complaint.getCategory(),
                                "AVAILABLE");

        if (eligibleStaff.isEmpty()) {
            throw new IllegalStateException(
                    "No available staff member matches category: "
                    + complaint.getCategory());
        }

        Set<Long> previousStaffIds = new HashSet<>();

        for (Assignment previousAssignment : previousAssignments) {

            if (previousAssignment.getStaff() != null
                    && previousAssignment.getStaff().getStaffId() != null) {

                previousStaffIds.add(
                        previousAssignment.getStaff().getStaffId());
            }
        }

        // Prefer staff who have not previously handled this complaint.
        List<StaffProfile> alternativeStaff = eligibleStaff.stream()
                .filter(staff ->
                        !previousStaffIds.contains(staff.getStaffId()))
                .toList();

        // Reuse an eligible previous staff member if no alternative exists.
        List<StaffProfile> candidates = alternativeStaff.isEmpty()
                ? eligibleStaff
                : alternativeStaff;

        return candidates.stream()
                .min(Comparator.comparingInt(
                        staff -> staff.getCurrentWorkload()))
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No eligible staff member is available."));
    }

    // Save assignment, update complaint/workload, persist a notification,
    // and publish the existing real-time notification event.
    private Assignment createAssignment(
            Complaint complaint,
            StaffProfile selectedStaff,
            boolean reassignment) {

        Assignment assignment = new Assignment();
        assignment.setComplaint(complaint);
        assignment.setStaff(selectedStaff);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus("ASSIGNED");

        Assignment savedAssignment =
                assignmentRepository.save(assignment);

        complaint.setStatus("ASSIGNED");
        complaintRepository.save(complaint);

        selectedStaff.setCurrentWorkload(
                selectedStaff.getCurrentWorkload() + 1);

        staffRepository.save(selectedStaff);

        String eventType = reassignment
                ? "COMPLAINT_REASSIGNED"
                : "COMPLAINT_ASSIGNED";

        // Save a persistent notification for the assigned technician.
        systemNotificationService.saveStaffAssignmentNotification(
                savedAssignment,
                eventType);

        // Preserve real-time notifications for dashboards.
        notificationService.sendAssignmentUpdate(
                eventType,
                savedAssignment);

        return savedAssignment;
    }

    // Start an assigned task.
    @Transactional
    public Assignment startAssignment(Long assignmentId) {

        Assignment assignment = getAssignmentById(assignmentId);

        if (!"ASSIGNED".equalsIgnoreCase(assignment.getStatus())) {
            throw new IllegalStateException(
                    "Only an ASSIGNED task can be started. Current status: "
                    + assignment.getStatus());
        }

        assignment.setStatus("IN_PROGRESS");

        Complaint complaint = assignment.getComplaint();
        complaint.setStatus("IN_PROGRESS");

        Complaint savedComplaint = complaintRepository.save(complaint);
        Assignment savedAssignment = assignmentRepository.save(assignment);

        notificationService.sendComplaintUpdate(
                "COMPLAINT_IN_PROGRESS",
                savedComplaint);

        return savedAssignment;
    }

    // Complete an in-progress task and release its workload.
    @Transactional
    public Assignment completeAssignment(Long assignmentId) {

        Assignment assignment = getAssignmentById(assignmentId);

        if (!"IN_PROGRESS".equalsIgnoreCase(assignment.getStatus())) {
            throw new IllegalStateException(
                    "Only an IN_PROGRESS task can be completed. Current status: "
                    + assignment.getStatus());
        }

        assignment.setStatus("COMPLETED");
        assignment.setCompletedAt(LocalDateTime.now());

        Complaint complaint = assignment.getComplaint();
        complaint.setStatus("RESOLVED");

        Complaint savedComplaint = complaintRepository.save(complaint);

        StaffProfile staff = assignment.getStaff();

        if (staff != null && staff.getCurrentWorkload() > 0) {
            staff.setCurrentWorkload(
                    staff.getCurrentWorkload() - 1);

            staffRepository.save(staff);
        }

        Assignment savedAssignment = assignmentRepository.save(assignment);

        notificationService.sendComplaintUpdate(
                "COMPLAINT_RESOLVED",
                savedComplaint);

        return savedAssignment;
    }

    // Find an assignment by ID.
    private Assignment getAssignmentById(Long assignmentId) {

        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Assignment not found: " + assignmentId));
    }

    // Find a complaint by ID.
    private Complaint getComplaintById(Long complaintId) {

        return complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Complaint not found: " + complaintId));
    }

    // Get all assignments for a complaint, including its history.
    public List<Assignment> getAssignmentsForComplaint(Long complaintId) {

        return assignmentRepository
                .findByComplaint_ComplaintId(complaintId);
    }

    // Get all assignments for a staff member, including reassigned history.
    public List<Assignment> getAssignmentsForStaff(Long staffId) {

        return assignmentRepository.findByStaff_StaffId(staffId);
    }
}
