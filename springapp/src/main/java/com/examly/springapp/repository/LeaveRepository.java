package com.examly.springapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.examly.springapp.model.Leave;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByEmployeeNameIgnoreCase(String employeeName);
    Page<Leave> findByEmployeeNameIgnoreCase(String employeeName, Pageable pageable);

    // Multi-tenant scoped queries
    @Query("SELECT l FROM Leave l WHERE l.organization.id = :organizationId")
    List<Leave> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT l FROM Leave l WHERE l.organization.id = :organizationId")
    Page<Leave> findByOrganizationId(@Param("organizationId") Long organizationId, Pageable pageable);

    @Query("SELECT l FROM Leave l WHERE l.organization.id = :organizationId AND LOWER(l.employeeName) = LOWER(:employeeName)")
    List<Leave> findByOrganizationIdAndEmployeeNameIgnoreCase(@Param("organizationId") Long organizationId, @Param("employeeName") String employeeName);

    @Query("SELECT l FROM Leave l WHERE l.organization.id = :organizationId AND LOWER(l.employeeName) = LOWER(:employeeName)")
    Page<Leave> findByOrganizationIdAndEmployeeNameIgnoreCase(@Param("organizationId") Long organizationId, @Param("employeeName") String employeeName, Pageable pageable);

    @Query("SELECT l FROM Leave l WHERE l.id = :id AND l.organization.id = :organizationId")
    Optional<Leave> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    // Scoped calendar query
    @Query("SELECT l FROM Leave l WHERE l.organization.id = :organizationId AND l.startDate IS NOT NULL AND l.endDate IS NOT NULL AND l.startDate <= :rangeEnd AND l.endDate >= :rangeStart AND l.status IN ('Approved', 'Pending') ORDER BY l.startDate ASC, l.employeeName ASC")
    List<Leave> findOverlappingActiveLeavesByOrg(@Param("organizationId") Long organizationId, @Param("rangeStart") String rangeStart, @Param("rangeEnd") String rangeEnd);

    // Legacy query retained for backward compatibility
    @Query("SELECT l FROM Leave l WHERE l.startDate IS NOT NULL AND l.endDate IS NOT NULL AND l.startDate <= :rangeEnd AND l.endDate >= :rangeStart AND l.status IN ('Approved', 'Pending') ORDER BY l.startDate ASC, l.employeeName ASC")
    List<Leave> findOverlappingActiveLeaves(@Param("rangeStart") String rangeStart, @Param("rangeEnd") String rangeEnd);

    // Unassigned legacy queries strictly isolated from tenant operations
    @Query("SELECT l FROM Leave l WHERE l.organization IS NULL")
    List<Leave> findByOrganizationIsNull();

    @Query("SELECT l FROM Leave l WHERE l.organization IS NULL")
    Page<Leave> findByOrganizationIsNull(Pageable pageable);

    @Query("SELECT l FROM Leave l WHERE l.organization IS NULL AND LOWER(l.employeeName) = LOWER(:employeeName)")
    List<Leave> findByOrganizationIsNullAndEmployeeNameIgnoreCase(@Param("employeeName") String employeeName);

    @Query("SELECT l FROM Leave l WHERE l.organization IS NULL AND LOWER(l.employeeName) = LOWER(:employeeName)")
    Page<Leave> findByOrganizationIsNullAndEmployeeNameIgnoreCase(@Param("employeeName") String employeeName, Pageable pageable);

    @Query("SELECT l FROM Leave l WHERE l.organization IS NULL AND l.startDate IS NOT NULL AND l.endDate IS NOT NULL AND l.startDate <= :rangeEnd AND l.endDate >= :rangeStart AND l.status IN ('Approved', 'Pending') ORDER BY l.startDate ASC, l.employeeName ASC")
    List<Leave> findOverlappingActiveLeavesUnassigned(@Param("rangeStart") String rangeStart, @Param("rangeEnd") String rangeEnd);
}