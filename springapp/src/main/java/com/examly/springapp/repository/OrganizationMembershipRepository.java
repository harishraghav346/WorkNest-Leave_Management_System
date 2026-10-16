package com.examly.springapp.repository;

import com.examly.springapp.model.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"organization"})
    List<OrganizationMembership> findByOrganizationId(Long organizationId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"organization"})
    List<OrganizationMembership> findAllByUserId(Long userId);

    default Optional<OrganizationMembership> findByUserId(Long userId) {
        List<OrganizationMembership> list = findAllByUserId(userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"organization"})
    List<OrganizationMembership> findByUserIdAndStatus(Long userId, String status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"organization"})
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"organization"})
    Optional<OrganizationMembership> findByOrganizationIdAndUserIdAndStatus(Long organizationId, Long userId, String status);
    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
