
package com.hostelcomplaint.dto;

import java.time.LocalDateTime;

public class SlaEscalationResponse {

    private Long escalationId;
    private Long complaintId;
    private String complaintTitle;
    private String severity;
    private String complaintStatus;
    private LocalDateTime slaDeadline;
    private LocalDateTime escalatedAt;
    private String reason;
    private String status;

    public Long getEscalationId() {
        return escalationId;
    }

    public void setEscalationId(Long escalationId) {
        this.escalationId = escalationId;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public String getComplaintTitle() {
        return complaintTitle;
    }

    public void setComplaintTitle(String complaintTitle) {
        this.complaintTitle = complaintTitle;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getComplaintStatus() {
        return complaintStatus;
    }

    public void setComplaintStatus(String complaintStatus) {
        this.complaintStatus = complaintStatus;
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(LocalDateTime slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
