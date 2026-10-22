package com.hostelcomplaint.controller;

import com.hostelcomplaint.dto.ComplaintRequest;
import com.hostelcomplaint.dto.ComplaintResponse;
import com.hostelcomplaint.entity.Complaint;
import com.hostelcomplaint.service.ComplaintService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Submit a complaint as the authenticated student.
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse createComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Authentication authentication) {

        return complaintService.createComplaint(request, authentication);
    }

    // The service filters results according to the logged-in user's role.
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','WARDEN','ADMIN')")
    public List<Complaint> getAllComplaints(
            Authentication authentication) {

        return complaintService.getAllComplaints(authentication);
    }

    // Prevent access to complaints the logged-in user is not authorized to view.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','WARDEN','ADMIN')")
    public Complaint getComplaintById(
            @PathVariable Long id,
            Authentication authentication) {

        return complaintService.getComplaintById(id, authentication);
    }

    // General status override is reserved for wardens and admins.
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public Complaint updateComplaintStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        return complaintService.updateComplaintStatus(id, status);
    }

    // Only the owning student can verify a resolved complaint.
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public Complaint verifyComplaint(
            @PathVariable Long id,
            Authentication authentication) {

        return complaintService.verifyComplaint(id, authentication);
    }

    // Only the owning student can reopen their complaint.
    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasRole('STUDENT')")
    public Complaint reopenComplaint(
            @PathVariable Long id,
            Authentication authentication) {

        return complaintService.reopenComplaint(id, authentication);
    }
}