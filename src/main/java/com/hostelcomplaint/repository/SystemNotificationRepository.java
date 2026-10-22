
package com.hostelcomplaint.repository;

import com.hostelcomplaint.entity.SystemNotification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNotificationRepository
        extends JpaRepository<SystemNotification, Long> {

    // Retrieve all notifications for a recipient type,
    // newest notifications first.
    List<SystemNotification> findByRecipientTypeOrderByCreatedAtDesc(
            String recipientType);

    // Retrieve notifications for a specific staff member.
    List<SystemNotification>
            findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
                    String recipientType,
                    Long recipientId);

    // Retrieve unread notifications for a specific recipient.
    @Query("""
            SELECT n
            FROM SystemNotification n
            WHERE n.recipientType = :recipientType
              AND n.recipientId = :recipientId
              AND n.isRead = false
            ORDER BY n.createdAt DESC
            """)
    List<SystemNotification> findUnreadForRecipient(
            @Param("recipientType") String recipientType,
            @Param("recipientId") Long recipientId);

    // Retrieve unread notifications for a recipient type,
    // such as general warden notifications.
    @Query("""
            SELECT n
            FROM SystemNotification n
            WHERE n.recipientType = :recipientType
              AND n.isRead = false
            ORDER BY n.createdAt DESC
            """)
    List<SystemNotification> findUnreadByRecipientType(
            @Param("recipientType") String recipientType);
}
