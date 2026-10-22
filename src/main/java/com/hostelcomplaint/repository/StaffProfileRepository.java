
package com.hostelcomplaint.repository;

import com.hostelcomplaint.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffProfileRepository
        extends JpaRepository<StaffProfile, Long> {

    List<StaffProfile> findByAvailabilityStatusIgnoreCase(
            String availabilityStatus);

    List<StaffProfile> findBySpecializationIgnoreCaseAndAvailabilityStatusIgnoreCase(
            String specialization,
            String availabilityStatus);
}
