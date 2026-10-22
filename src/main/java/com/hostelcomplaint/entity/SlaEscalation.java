
package com.hostelcomplaint.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "sla_escalations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sla_escalation_complaint",
            columnNames = "complaint_id"
        )
    }
)
public class SlaEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escalation_id")
    private Long escalationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "escalated_at", nullable = false)
    private LocalDateTime escalatedAt;

    @Column(name = "sla_deadline", nullable = false)
    private LocalDateTime slaDeadline;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "OPEN";

    public Long getEscalationId() {
        return escalationId;
    }

    public void setEscalationId(Long escalationId) {
        this.escalationId = escalationId;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(LocalDateTime slaDeadline) {
        this.slaDeadline = slaDeadline;
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
