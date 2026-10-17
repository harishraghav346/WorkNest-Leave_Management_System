package com.examly.springapp.model;

public class ResendVerificationRequest {

    private String email;
    private String organizationSlug;

    public ResendVerificationRequest() {}

    public ResendVerificationRequest(String email) {
        this.email = email;
    }

    public ResendVerificationRequest(String email, String organizationSlug) {
        this.email = email;
        this.organizationSlug = organizationSlug;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public void setOrganizationSlug(String organizationSlug) {
        this.organizationSlug = organizationSlug;
    }
}
