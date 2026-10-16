package com.examly.springapp.model;

/**
 * Immutable trusted tenant context resolved server-side from active database records.
 * Never constructed directly from client-supplied parameters.
 */
public class TenantContext {

    private final Long organizationId;
    private final String organizationSlug;
    private final String organizationName;
    private final String organizationStatus;
    private final Long userId;
    private final String username;
    private final Long membershipId;
    private final String organizationRole; // ORG_ADMIN, HR_ADMIN, MANAGER, EMPLOYEE
    private final String membershipStatus;
    private final boolean platformAdmin;

    public TenantContext(Long organizationId, String organizationSlug, String organizationName,
                         String organizationStatus, Long userId, String username,
                         Long membershipId, String organizationRole, String membershipStatus,
                         boolean platformAdmin) {
        this.organizationId = organizationId;
        this.organizationSlug = organizationSlug;
        this.organizationName = organizationName;
        this.organizationStatus = organizationStatus;
        this.userId = userId;
        this.username = username;
        this.membershipId = membershipId;
        this.organizationRole = organizationRole != null ? organizationRole.toUpperCase() : "EMPLOYEE";
        this.membershipStatus = membershipStatus;
        this.platformAdmin = platformAdmin;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationStatus() {
        return organizationStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public String getOrganizationRole() {
        return organizationRole;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public boolean isOrgAdmin() {
        return "ORG_ADMIN".equalsIgnoreCase(organizationRole);
    }

    public boolean isHrAdmin() {
        return "HR_ADMIN".equalsIgnoreCase(organizationRole);
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(organizationRole);
    }

    public boolean isEmployee() {
        return "EMPLOYEE".equalsIgnoreCase(organizationRole);
    }

    public boolean hasAdminOrHrPrivileges() {
        return isOrgAdmin() || isHrAdmin();
    }

    public boolean hasApprovalPrivileges() {
        return isOrgAdmin() || isHrAdmin() || isManager();
    }
}
