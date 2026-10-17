package com.examly.springapp.model;

public class OrganizationMembershipDTO {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String organizationSlug;
    private String role;
    private String status;

    public OrganizationMembershipDTO() {
    }

    public OrganizationMembershipDTO(Long id, Long organizationId, String organizationName, String organizationSlug, String role, String status) {
        this.id = id;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.organizationSlug = organizationSlug;
        this.role = role;
        this.status = status;
    }

    public static OrganizationMembershipDTO fromEntity(OrganizationMembership mem) {
        if (mem == null) return null;
        Organization org = mem.getOrganization();
        return new OrganizationMembershipDTO(
                mem.getId(),
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                org != null ? org.getSlug() : null,
                mem.getRole(),
                mem.getStatus()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public void setOrganizationSlug(String organizationSlug) {
        this.organizationSlug = organizationSlug;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
