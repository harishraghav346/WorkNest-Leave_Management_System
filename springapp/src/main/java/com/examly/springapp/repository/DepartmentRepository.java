package com.examly.springapp.repository;

import com.examly.springapp.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT d FROM Department d WHERE d.organization.id = :organizationId")
    List<Department> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.organization.id = :organizationId")
    Optional<Department> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    @Query("SELECT d FROM Department d WHERE d.organization.id = :organizationId AND LOWER(d.name) = LOWER(:name)")
    Optional<Department> findByOrganizationIdAndNameIgnoreCase(@Param("organizationId") Long organizationId, @Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.organization.id = :organizationId AND LOWER(d.name) = LOWER(:name)")
    boolean existsByOrganizationIdAndNameIgnoreCase(@Param("organizationId") Long organizationId, @Param("name") String name);

    @Query("SELECT d FROM Department d WHERE d.organization.id = :organizationId AND d.managerMembership.id = :managerMembershipId")
    List<Department> findByOrganizationIdAndManagerMembershipId(@Param("organizationId") Long organizationId, @Param("managerMembershipId") Long managerMembershipId);

    @Query("SELECT d FROM Department d WHERE d.organization.id = :organizationId AND d.managerMembership.user.username = :username")
    List<Department> findByOrganizationIdAndManagerUsername(@Param("organizationId") Long organizationId, @Param("username") String username);

    @Query("SELECT COUNT(d) FROM Department d WHERE d.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") Long organizationId);
}
