package com.hostelcomplaint.controller;

import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.entity.Assignment;
import com.hostelcomplaint.repository.AppUserRepository;
import com.hostelcomplaint.repository.AssignmentRepository;
import com.hostelcomplaint.service.AssignmentService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentRepository assignmentRepository;
    private final AppUserRepository userRepository;

    public AssignmentController(
            AssignmentService assignmentService,
            AssignmentRepository assignmentRepository,
            AppUserRepository userRepository) {
        this.assignmentService = assignmentService;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/complaint/{complaintId}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN','STAFF')")
    public List<Assignment> getAssignmentsForComplaint(
            @PathVariable Long complaintId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        List<Assignment> assignments =
                assignmentService.getAssignmentsForComplaint(complaintId);

        if ("STAFF".equals(user.getRole())) {
            requireStaffProfile(user);

            return assignments.stream()
                    .filter(a -> a.getStaff() != null
                            && Objects.equals(
                                    a.getStaff().getStaffId(),
                                    user.getStaffId()))
                    .toList();
        }

        return assignments;
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN','STAFF')")
    public List<Assignment> getAssignmentsForStaff(
            @PathVariable Long staffId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        if ("STAFF".equals(user.getRole())) {
            requireStaffProfile(user);

            if (!Objects.equals(user.getStaffId(), staffId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only view your own assignments");
            }
        }

        return assignmentService.getAssignmentsForStaff(staffId);
    }

    @PostMapping("/complaint/{complaintId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public Assignment assignComplaint(
            @PathVariable Long complaintId) {

        return assignmentService
                .assignComplaintAutomatically(complaintId);
    }

    @PostMapping("/complaint/{complaintId}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public Assignment reassignComplaint(
            @PathVariable Long complaintId) {

        return assignmentService
                .reassignComplaintAutomatically(complaintId);
    }

    @PatchMapping("/{assignmentId}/start")
    @PreAuthorize("hasRole('STAFF')")
    public Assignment startAssignment(
            @PathVariable Long assignmentId,
            Authentication authentication) {

        requireAssignmentOwner(assignmentId, authentication);

        return assignmentService.startAssignment(assignmentId);
    }

    @PatchMapping("/{assignmentId}/complete")
    @PreAuthorize("hasRole('STAFF')")
    public Assignment completeAssignment(
            @PathVariable Long assignmentId,
            Authentication authentication) {

        requireAssignmentOwner(assignmentId, authentication);

        return assignmentService.completeAssignment(assignmentId);
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));
    }

    private void requireStaffProfile(AppUser user) {
        if (user.getStaffId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This staff account is not linked to a staff profile");
        }
    }

    private void requireAssignmentOwner(
            Long assignmentId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);
        requireStaffProfile(user);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        if (assignment.getStaff() == null
                || !Objects.equals(
                        assignment.getStaff().getStaffId(),
                        user.getStaffId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only update your own assignments");
        }
    }
}