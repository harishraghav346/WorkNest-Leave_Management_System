package com.examly.springapp.model;

public class TenantSelectionRequest {
    private Long organizationId;
    private String organizationSlug;

    public TenantSelectionRequest() {
    }

    public TenantSelectionRequest(Long organizationId, String organizationSlug) {
        this.organizationId = organizationId;
        this.organizationSlug = organizationSlug;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public void setOrganizationSlug(String organizationSlug) {
        this.organizationSlug = organizationSlug;
    }
}
