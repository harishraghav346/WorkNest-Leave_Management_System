package com.examly.springapp.model;

public class OnboardingRequest {
    private String organizationName;
    private String organizationSlug;
    private String adminUsername;
    private String adminEmail;
    private String adminFullName;
    private String adminPassword;

    public OnboardingRequest() {
    }

    public OnboardingRequest(String organizationName, String organizationSlug, String adminUsername, String adminEmail, String adminFullName, String adminPassword) {
        this.organizationName = organizationName;
        this.organizationSlug = organizationSlug;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminFullName = adminFullName;
        this.adminPassword = adminPassword;
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

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminFullName() {
        return adminFullName;
    }

    public void setAdminFullName(String adminFullName) {
        this.adminFullName = adminFullName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
