
package com.hostelcomplaint.service;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class ComplaintEventPublisher {

    private static final String DESTINATION = "complaint.events";

    private final JmsTemplate jmsTemplate;

    public ComplaintEventPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }


public void publishComplaintEvent(String eventType, Long complaintId) {
    String message = eventType + "|complaintId=" + complaintId;

    System.out.println("[JMS TEST] About to publish: " + message);

    try {
        jmsTemplate.convertAndSend(DESTINATION, message);
        System.out.println("[JMS TEST] Message published successfully.");
    } catch (Exception e) {
        System.err.println("[JMS TEST] Publishing failed:");
        e.printStackTrace();
        throw e;
    }
}

}
