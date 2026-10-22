
package com.hostelcomplaint.dto;

import java.time.LocalDateTime;

public class ComplaintUpdateMessage {

    private String eventType;
    private Long complaintId;
    private String title;
    private String status;
    private String priority;
    private LocalDateTime timestamp;

    public ComplaintUpdateMessage() {
    }

    public ComplaintUpdateMessage(
            String eventType,
            Long complaintId,
            String title,
            String status,
            String priority,
            LocalDateTime timestamp) {
        this.eventType = eventType;
        this.complaintId = complaintId;
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
