package com.examly.springapp.repository;

import com.examly.springapp.model.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByMembershipId(Long membershipId);
    Optional<EmployeeProfile> findByOrganizationIdAndEmployeeId(Long organizationId, String employeeId);
    Optional<EmployeeProfile> findByOrganizationIdAndWorkEmail(Long organizationId, String workEmail);
    List<EmployeeProfile> findByOrganizationId(Long organizationId);
    boolean existsByOrganizationIdAndEmployeeId(Long organizationId, String employeeId);
    boolean existsByOrganizationIdAndWorkEmail(Long organizationId, String workEmail);
}
