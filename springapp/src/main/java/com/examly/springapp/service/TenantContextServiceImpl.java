package com.examly.springapp.service;

import com.examly.springapp.model.Organization;
import com.examly.springapp.model.OrganizationMembership;
import com.examly.springapp.model.TenantAuthenticationDetails;
import com.examly.springapp.model.TenantContext;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TenantContextServiceImpl implements TenantContextService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Override
    public TenantContext requireTenantContext(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user record not found in system.");
        }

        boolean isPlatformAdmin = user.getRole() != null && (
                user.getRole().equalsIgnoreCase("admin") ||
                user.getRole().equalsIgnoreCase("ROLE_ADMIN") ||
                user.getRole().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")
        );

        Long selectedOrgId = null;
        if (authentication.getDetails() instanceof TenantAuthenticationDetails) {
            selectedOrgId = ((TenantAuthenticationDetails) authentication.getDetails()).getOrganizationId();
        }

        List<OrganizationMembership> activeMemberships = membershipRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");

        if (activeMemberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: No active organization membership found for user. Tenant isolation enforced.");
        }

        OrganizationMembership membership = null;
        if (selectedOrgId != null) {
            for (OrganizationMembership mem : activeMemberships) {
                if (mem.getOrganization() != null && selectedOrgId.equals(mem.getOrganization().getId())) {
                    membership = mem;
                    break;
                }
            }
            if (membership == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: User does not have an active membership in the selected organization.");
            }
        } else {
            if (activeMemberships.size() > 1) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: Ambiguous organization memberships. User belongs to multiple active organizations without selector.");
            }
            membership = activeMemberships.get(0);
        }
        Organization organization = membership.getOrganization();

        if (organization == null || !"ACTIVE".equalsIgnoreCase(organization.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Target organization is inactive or suspended.");
        }

        if (!"ACTIVE".equalsIgnoreCase(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Organization membership is inactive.");
        }

        return new TenantContext(
                organization.getId(),
                organization.getSlug(),
                organization.getName(),
                organization.getStatus(),
                user.getId(),
                user.getUsername(),
                membership.getId(),
                membership.getRole(),
                membership.getStatus(),
                isPlatformAdmin
        );
    }

    @Override
    public Optional<TenantContext> resolveTenantContext(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return Optional.empty();
        }

        List<OrganizationMembership> allMemberships = membershipRepository.findAllByUserId(user.getId());
        if (allMemberships.isEmpty()) {
            return Optional.empty();
        }

        // User belongs to one or more organizations; requireTenantContext enforces active status, active org, and unambiguous membership
        return Optional.of(requireTenantContext(authentication));
    }

    @Override
    public void validatePlatformAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        boolean isPlatformAdmin = authentication.getAuthorities() != null && authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") ||
                a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")
        );

        User user = userRepository.findByUsername(authentication.getName());
        if (user != null) {
            isPlatformAdmin = isPlatformAdmin || (user.getRole() != null && (
                    user.getRole().equalsIgnoreCase("admin") ||
                    user.getRole().equalsIgnoreCase("ROLE_ADMIN") ||
                    user.getRole().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")
            ));
        }

        if (!isPlatformAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Trusted platform administrator privileges required.");
        }
    }

    @Override
    public void validateOrgAdmin(TenantContext context) {
        if (context == null || !context.isOrgAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Organization administrator privileges required.");
        }
    }

    @Override
    public void validateApprovalAuthority(TenantContext context) {
        if (context == null || !context.hasApprovalPrivileges()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Leave approval privileges required (ORG_ADMIN, HR_ADMIN, or MANAGER).");
        }
    }
}
