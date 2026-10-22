package com.hostelcomplaint.repository;

import com.hostelcomplaint.entity.Complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository
        extends JpaRepository<Complaint, Long> {

    List<Complaint> findAllByStudent_Id(Long studentId);
}