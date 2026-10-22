
package com.hostelcomplaint.dto;

import java.time.LocalDateTime;

public class ComplaintResponse {

    private Long complaintId;
    private String title;
    private String description;
    private String category;
    private String severity;
    private String priority;
    private String status;
    private LocalDateTime createdAt;

    public ComplaintResponse() {
    }

    public ComplaintResponse(
            Long complaintId,
            String title,
            String description,
            String category,
            String severity,
            String priority,
            String status,
            LocalDateTime createdAt) {

        this.complaintId = complaintId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
