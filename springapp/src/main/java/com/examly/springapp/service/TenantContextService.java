package com.examly.springapp.service;

import com.examly.springapp.model.TenantContext;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface TenantContextService {

    /**
     * Resolves the trusted tenant context from the authenticated principal.
     * Fails closed by throwing a 403 Forbidden or 401 Unauthorized exception if:
     * - Authentication is missing or invalid
     * - User does not exist in the database
     * - User has no active organization membership
     * - Target organization is inactive or suspended
     * - Membership status is inactive
     * - Membership selection is ambiguous
     */
    TenantContext requireTenantContext(Authentication authentication);

    /**
     * Resolves tenant context if present, returning empty Optional without throwing.
     */
    Optional<TenantContext> resolveTenantContext(Authentication authentication);

    /**
     * Asserts that the authenticated principal is an authorized platform administrator.
     */
    void validatePlatformAdmin(Authentication authentication);

    /**
     * Asserts that the caller has organization administrator privileges.
     */
    void validateOrgAdmin(TenantContext context);

    /**
     * Asserts that the caller has leave approval privileges (ORG_ADMIN, HR_ADMIN, or MANAGER).
     */
    void validateApprovalAuthority(TenantContext context);
}
