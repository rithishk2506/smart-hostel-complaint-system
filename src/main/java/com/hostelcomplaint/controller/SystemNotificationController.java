package com.hostelcomplaint.controller;

import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.entity.SystemNotification;
import com.hostelcomplaint.repository.AppUserRepository;
import com.hostelcomplaint.service.SystemNotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/notifications")
public class SystemNotificationController {

    private final SystemNotificationService notificationService;
    private final AppUserRepository userRepository;

    public SystemNotificationController(
            SystemNotificationService notificationService,
            AppUserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasRole('STAFF')")
    public List<SystemNotification> getStaffNotifications(
            @PathVariable Long staffId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);
        requireOwnStaffId(user, staffId);

        return notificationService.getStaffNotifications(staffId);
    }

    @GetMapping("/staff/{staffId}/unread")
    @PreAuthorize("hasRole('STAFF')")
    public List<SystemNotification> getUnreadStaffNotifications(
            @PathVariable Long staffId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);
        requireOwnStaffId(user, staffId);

        return notificationService.getUnreadStaffNotifications(staffId);
    }

    @GetMapping("/warden")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public List<SystemNotification> getWardenNotifications() {
        return notificationService.getWardenNotifications();
    }

    @GetMapping("/warden/unread")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public List<SystemNotification> getUnreadWardenNotifications() {
        return notificationService.getUnreadWardenNotifications();
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('STAFF','WARDEN','ADMIN')")
    public SystemNotification markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestParam String recipientType,
            @RequestParam(required = false) Long recipientId,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        String type = recipientType == null
                ? ""
                : recipientType.trim().toUpperCase();

        if ("STAFF".equals(type)) {
            if (!"STAFF".equals(user.getRole())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only staff can mark their staff notifications as read");
            }

            if (recipientId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "recipientId is required for staff notifications");
            }

            requireOwnStaffId(user, recipientId);

            return notificationService.markAsRead(
                    notificationId, "STAFF", recipientId);
        }

        if ("WARDEN".equals(type)) {
            if (!"WARDEN".equals(user.getRole())
                    && !"ADMIN".equals(user.getRole())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Warden access is required");
            }

            if (recipientId != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Do not provide recipientId for warden notifications");
            }

            return notificationService.markAsRead(
                    notificationId, "WARDEN", null);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "recipientType must be STAFF or WARDEN");
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));
    }

    private void requireOwnStaffId(AppUser user, Long requestedStaffId) {
        if (user.getStaffId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This login is not linked to a staff profile");
        }

        if (!Objects.equals(user.getStaffId(), requestedStaffId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only access your own staff notifications");
        }
    }
}