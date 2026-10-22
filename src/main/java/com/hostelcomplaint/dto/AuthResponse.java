package com.hostelcomplaint.dto;

import com.hostelcomplaint.entity.AppUser;

public class AuthResponse {

    private Long userId;
    private String username;
    private String fullName;
    private String role;
    private Long staffId;

    public AuthResponse() {
    }

    public AuthResponse(AppUser user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.staffId = user.getStaffId();
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public Long getStaffId() { return staffId; }
}