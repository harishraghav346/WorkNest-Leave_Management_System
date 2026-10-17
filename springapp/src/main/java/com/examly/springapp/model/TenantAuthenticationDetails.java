package com.examly.springapp.model;

import java.io.Serializable;

public class TenantAuthenticationDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long organizationId;
    private final String organizationSlug;

    public TenantAuthenticationDetails(Long organizationId, String organizationSlug) {
        this.organizationId = organizationId;
        this.organizationSlug = organizationSlug;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }
}
