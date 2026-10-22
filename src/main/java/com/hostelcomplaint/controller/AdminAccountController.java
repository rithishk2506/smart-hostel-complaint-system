package com.hostelcomplaint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.hostelcomplaint.dto.AuthResponse;
import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.repository.AppUserRepository;
import com.hostelcomplaint.repository.StaffProfileRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    private final AppUserRepository userRepository;
    private final StaffProfileRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountController(
            AppUserRepository userRepository,
            StaffProfileRepository staffRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/wardens")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createWarden(@RequestBody CreateAccountRequest body) {
        return createAccount(body, "WARDEN");
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createStaff(@RequestBody CreateAccountRequest body) {

        if (body.getStaffId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "staffId is required for a staff account");
        }

        if (!staffRepository.existsById(body.getStaffId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Staff profile not found: " + body.getStaffId());
        }

        if (userRepository.existsByStaffId(body.getStaffId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This staff profile already has a login account");
        }

        return createAccount(body, "STAFF");
    }

    private AuthResponse createAccount(
            CreateAccountRequest body, String role) {

        if (body.getUsername() == null
                || body.getUsername().isBlank()
                || body.getFullName() == null
                || body.getFullName().isBlank()
                || body.getPassword() == null
                || body.getPassword().length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name, username, and a password of at least 8 characters are required");
        }

        String username = body.getUsername().trim().toLowerCase();

        if (!username.matches("[a-z0-9._-]{3,80}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid username format");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username is already in use");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(body.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        user.setRole(role);
        user.setEnabled(true);

        if ("STAFF".equals(role)) {
            user.setStaffId(body.getStaffId());
        }

        return new AuthResponse(userRepository.save(user));
    }

    public static class CreateAccountRequest {
        private String username;
        private String fullName;
        private String password;
        private Long staffId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Long getStaffId() { return staffId; }
        public void setStaffId(Long staffId) { this.staffId = staffId; }
    }
}