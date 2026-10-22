
package com.hostelcomplaint.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_profiles")
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(nullable = false)
    private String staffName;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private String availabilityStatus = "AVAILABLE";

    @Column(nullable = false)
    private int currentWorkload = 0;

    public StaffProfile() {
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public int getCurrentWorkload() {
        return currentWorkload;
    }

    public void setCurrentWorkload(int currentWorkload) {
        this.currentWorkload = currentWorkload;
    }
}
