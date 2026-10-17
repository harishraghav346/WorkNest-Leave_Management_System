package com.examly.springapp.repository;

import com.examly.springapp.model.EmployeeInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitation, Long> {

    Optional<EmployeeInvitation> findByTokenHash(String tokenHash);

    @Query("SELECT i FROM EmployeeInvitation i WHERE i.organization.id = :organizationId")
    List<EmployeeInvitation> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM EmployeeInvitation i WHERE i.organization.id = :organizationId AND i.employeeId = :employeeId AND i.status = :status")
    boolean existsByOrganizationIdAndEmployeeIdAndStatus(@Param("organizationId") Long organizationId, @Param("employeeId") String employeeId, @Param("status") String status);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM EmployeeInvitation i WHERE i.organization.id = :organizationId AND LOWER(i.email) = LOWER(:email) AND i.status = :status")
    boolean existsByOrganizationIdAndEmailIgnoreCaseAndStatus(@Param("organizationId") Long organizationId, @Param("email") String email, @Param("status") String status);

    @Query("SELECT i FROM EmployeeInvitation i WHERE i.organization.id = :organizationId AND i.employeeId = :employeeId")
    Optional<EmployeeInvitation> findByOrganizationIdAndEmployeeId(@Param("organizationId") Long organizationId, @Param("employeeId") String employeeId);

    @Modifying
    @Query("UPDATE EmployeeInvitation i SET i.status = 'ACCEPTED', i.acceptedAt = :acceptedAt WHERE i.id = :id AND i.status = 'PENDING'")
    int markAcceptedIfPending(@Param("id") Long id, @Param("acceptedAt") Instant acceptedAt);
}
