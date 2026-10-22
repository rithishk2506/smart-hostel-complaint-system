package com.hostelcomplaint.repository;

import com.hostelcomplaint.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<Assignment, Long> {

    List<Assignment> findByComplaint_ComplaintId(Long complaintId);

    List<Assignment> findByStaff_StaffId(Long staffId);
}