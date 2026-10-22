
package com.hostelcomplaint.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hostelcomplaint.dto.ComplaintUpdateMessage;
import com.hostelcomplaint.entity.Assignment;
import com.hostelcomplaint.entity.Complaint;
import com.hostelcomplaint.entity.StaffProfile;

@Service
public class ComplaintNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public ComplaintNotificationService(
            SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    // Send a general complaint update to subscribed dashboards.
    public void sendComplaintUpdate(
            String eventType,
            Complaint complaint) {

        if (complaint == null) {
            throw new IllegalArgumentException(
                    "Complaint is required for notification.");
        }

        ComplaintUpdateMessage message =
                new ComplaintUpdateMessage(
                        eventType,
                        complaint.getComplaintId(),
                        complaint.getTitle(),
                        complaint.getStatus() == null
                                ? null
                                : complaint.getStatus().toString(),
                        complaint.getPriority() == null
                                ? null
                                : complaint.getPriority().toString(),
                        LocalDateTime.now()
                );

        messagingTemplate.convertAndSend(
                "/topic/complaints",
                (Object) message
        );
    }

    // Notify dashboards when a complaint is assigned or reassigned.
    public void sendAssignmentUpdate(
            String eventType,
            Assignment assignment) {

        if (assignment == null || assignment.getComplaint() == null) {
            throw new IllegalArgumentException(
                    "Assignment and complaint are required for notification.");
        }

        Complaint complaint = assignment.getComplaint();
        StaffProfile staff = assignment.getStaff();

        Map<String, Object> event = new LinkedHashMap<>();

        event.put("eventType", eventType);
        event.put("assignmentId", assignment.getAssignmentId());
        event.put("complaintId", complaint.getComplaintId());
        event.put("title", complaint.getTitle());
        event.put("description", complaint.getDescription());
        event.put("category", complaint.getCategory());
        event.put("severity", complaint.getSeverity());
        event.put("priority", complaint.getPriority());
        event.put("complaintStatus", complaint.getStatus());
        event.put("assignmentStatus", assignment.getStatus());
        event.put(
                "assignedAt",
                assignment.getAssignedAt() == null
                        ? null
                        : assignment.getAssignedAt().toString()
        );

        event.put(
                "staffId",
                staff == null ? null : staff.getStaffId()
        );

        event.put(
                "staffName",
                staff == null ? null : staff.getStaffName()
        );

        event.put("occurredAt", LocalDateTime.now().toString());

        String messageText;

        if ("COMPLAINT_REASSIGNED".equalsIgnoreCase(eventType)) {

            messageText = "Complaint #"
                    + complaint.getComplaintId()
                    + " has been reassigned to "
                    + (staff == null
                            ? "a staff member"
                            : staff.getStaffName())
                    + ".";

        } else {

            messageText = "Complaint #"
                    + complaint.getComplaintId()
                    + " has been assigned to you.";
        }

        event.put("message", messageText);

        // Broadcast the assignment event to the warden dashboard
        // and other listeners of the general complaints topic.
        messagingTemplate.convertAndSend(
                "/topic/complaints",
                (Object) event
        );

        // Send the event to the selected technician's topic.
        // The staff dashboard must subscribe to this destination
        // to receive the real-time notification.
        if (staff != null && staff.getStaffId() != null) {

            messagingTemplate.convertAndSend(
                    "/topic/staff/" + staff.getStaffId(),
                    (Object) event
            );
        }
    }
}
