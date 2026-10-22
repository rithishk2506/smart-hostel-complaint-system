package com.hostelcomplaint.service;

import com.hostelcomplaint.entity.StaffProfile;
import com.hostelcomplaint.repository.StaffProfileRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffProfileService {

    private final StaffProfileRepository staffRepository;

    public StaffProfileService(StaffProfileRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    // Register a new maintenance staff member
    public StaffProfile createStaff(StaffProfile staff) {

        if (staff.getStaffName() == null
                || staff.getStaffName().isBlank()) {
            throw new IllegalArgumentException(
                    "Staff name is required.");
        }

        if (staff.getSpecialization() == null
                || staff.getSpecialization().isBlank()) {
            throw new IllegalArgumentException(
                    "Staff specialization is required.");
        }

        staff.setStaffName(staff.getStaffName().trim());
        staff.setSpecialization(staff.getSpecialization().trim());

        if (staff.getAvailabilityStatus() == null
                || staff.getAvailabilityStatus().isBlank()) {
            staff.setAvailabilityStatus("AVAILABLE");
        } else {
            staff.setAvailabilityStatus(
                    staff.getAvailabilityStatus().trim().toUpperCase());
        }

        if (staff.getCurrentWorkload() < 0) {
            throw new IllegalArgumentException(
                    "Workload cannot be negative.");
        }

        return staffRepository.save(staff);
    }

    // Get all staff members
    public List<StaffProfile> getAllStaff() {
        return staffRepository.findAll();
    }

    // Get only available staff members
    public List<StaffProfile> getAvailableStaff() {
        return staffRepository.findByAvailabilityStatusIgnoreCase(
                "AVAILABLE");
    }

    // Get staff by specialization
    public List<StaffProfile> getStaffBySpecialization(
            String specialization) {

        return staffRepository
                .findBySpecializationIgnoreCaseAndAvailabilityStatusIgnoreCase(
                        specialization, "AVAILABLE");
    }

    // Get staff member by ID
    public StaffProfile getStaffById(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Staff member not found: " + id));
    }
}