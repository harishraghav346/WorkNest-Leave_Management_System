package com.examly.springapp.repository;

import com.examly.springapp.model.OrganizationVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationVerificationTokenRepository extends JpaRepository<OrganizationVerificationToken, Long> {

    Optional<OrganizationVerificationToken> findByTokenHash(String tokenHash);

    Optional<OrganizationVerificationToken> findTopByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<OrganizationVerificationToken> findTopByAdminEmailIgnoreCaseOrderByCreatedAtDesc(String adminEmail);

    List<OrganizationVerificationToken> findAllByOrganizationId(Long organizationId);

    List<OrganizationVerificationToken> findAllByOrganizationIdAndStatus(Long organizationId, String status);

    /**
     * Atomically redeems the verification token if and only if it is currently in 'PENDING' status.
     * Prevents race conditions and duplicate redemption attempts.
     */
    @Modifying
    @Query("UPDATE OrganizationVerificationToken t SET t.status = 'ACCEPTED', t.verifiedAt = :now WHERE t.id = :id AND t.status = 'PENDING'")
    int updateStatusIfPending(@Param("id") Long id, @Param("now") Instant now);
}
