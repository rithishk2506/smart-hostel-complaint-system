
package com.hostelcomplaint.service;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ComplaintEventListener {

    private final ComplaintService complaintService;
    private final ComplaintNotificationService notificationService;

    public ComplaintEventListener(
            ComplaintService complaintService,
            ComplaintNotificationService notificationService) {
        this.complaintService = complaintService;
        this.notificationService = notificationService;
    }

    @JmsListener(destination = "complaint.events")
    public void receiveComplaintEvent(String message) {

        System.out.println("[JMS LISTENER] Received: " + message);

        try {
            String[] parts = message.split("\\|", 2);

            if (parts.length != 2 || !parts[1].startsWith("complaintId=")) {
                System.err.println("[JMS LISTENER] Invalid event format: " + message);
                return;
            }

            String eventType = parts[0];
            Long complaintId = Long.valueOf(
                    parts[1].substring("complaintId=".length()));

            // Load the latest complaint state from the database.
            var complaint = complaintService.getComplaintById(complaintId);

            // Forward the update to WebSocket subscribers.
            notificationService.sendComplaintUpdate(eventType, complaint);

            System.out.println(
                    "[JMS LISTENER] WebSocket update sent for complaint "
                    + complaintId);

        } catch (Exception e) {
            System.err.println(
                    "[JMS LISTENER] Failed to process event: " + message);
            e.printStackTrace();
            throw e;
        }
    }
}
