package com.hostelcomplaint.controller;

import com.hostelcomplaint.dto.SlaEscalationResponse;
import com.hostelcomplaint.service.SlaMonitoringService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/escalations")
public class SlaEscalationController {

    private final SlaMonitoringService slaMonitoringService;

    public SlaEscalationController(
            SlaMonitoringService slaMonitoringService) {
        this.slaMonitoringService = slaMonitoringService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public List<SlaEscalationResponse> getAllEscalations() {
        return slaMonitoringService.getAllEscalations();
    }

    @PatchMapping("/{escalationId}/status")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public SlaEscalationResponse updateEscalationStatus(
            @PathVariable("escalationId") Long escalationId,
            @RequestParam("status") String status) {

        return slaMonitoringService.updateEscalationStatus(
                escalationId, status);
    }
}