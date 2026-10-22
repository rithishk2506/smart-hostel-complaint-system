
package com.hostelcomplaint.repository;

import com.hostelcomplaint.entity.SlaEscalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlaEscalationRepository
        extends JpaRepository<SlaEscalation, Long> {

    boolean existsByComplaint_ComplaintId(Long complaintId);

    List<SlaEscalation> findAllByOrderByEscalatedAtDesc();
}
