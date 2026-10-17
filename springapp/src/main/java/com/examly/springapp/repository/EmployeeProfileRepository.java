package com.examly.springapp.repository;

import com.examly.springapp.model.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT p FROM EmployeeProfile p WHERE p.organization.id = :organizationId AND LOWER(p.membership.user.username) = LOWER(:username)")
    Optional<EmployeeProfile> findByOrganizationIdAndMembershipUserUsername(@Param("organizationId") Long organizationId, @Param("username") String username);

    @Query("SELECT p FROM EmployeeProfile p WHERE p.departmentRef.id = :departmentId")
    List<EmployeeProfile> findByDepartmentRefId(@Param("departmentId") Long departmentId);

    @Query("SELECT p FROM EmployeeProfile p WHERE p.organization.id = :organizationId AND p.departmentRef.id = :departmentId")
    List<EmployeeProfile> findByOrganizationIdAndDepartmentRefId(@Param("organizationId") Long organizationId, @Param("departmentId") Long departmentId);
}
