package com.hostelcomplaint.controller;

import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.entity.StaffProfile;
import com.hostelcomplaint.repository.AppUserRepository;
import com.hostelcomplaint.service.StaffProfileService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/staff")
public class StaffProfileController {

    private final StaffProfileService staffService;
    private final AppUserRepository userRepository;

    public StaffProfileController(
            StaffProfileService staffService,
            AppUserRepository userRepository) {
        this.staffService = staffService;
        this.userRepository = userRepository;
    }

    // Only admins may create staff profiles.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StaffProfile createStaff(@RequestBody StaffProfile staff) {
        return staffService.createStaff(staff);
    }

    // The staff directory is restricted to admins and wardens.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<StaffProfile> getAllStaff() {
        return staffService.getAllStaff();
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<StaffProfile> getAvailableStaff() {
        return staffService.getAvailableStaff();
    }

    // Staff can retrieve their own profile; admins and wardens
    // may retrieve any staff profile.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN','STAFF')")
    public StaffProfile getStaffById(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser user = getCurrentUser(authentication);

        if ("STAFF".equals(user.getRole())) {
            if (user.getStaffId() == null
                    || !Objects.equals(user.getStaffId(), id)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only view your own staff profile");
            }
        }

        return staffService.getStaffById(id);
    }

    @GetMapping("/specialization/{specialization}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<StaffProfile> getStaffBySpecialization(
            @PathVariable String specialization) {

        return staffService.getStaffBySpecialization(specialization);
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
}