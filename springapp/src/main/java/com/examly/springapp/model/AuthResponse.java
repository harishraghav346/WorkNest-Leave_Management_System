package com.examly.springapp.model;

import java.util.List;

public class AuthResponse {
    private String token;
    private Long id;
    private String username;
    private String role;
    private Organization organization;
    private boolean requiresTenantSelection;
    private List<OrganizationMembershipDTO> memberships;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, String username, String role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public AuthResponse(String token, Long id, String username, String role, Organization organization) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.role = role;
        this.organization = organization;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public boolean isRequiresTenantSelection() {
        return requiresTenantSelection;
    }

    public void setRequiresTenantSelection(boolean requiresTenantSelection) {
        this.requiresTenantSelection = requiresTenantSelection;
    }

    public List<OrganizationMembershipDTO> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<OrganizationMembershipDTO> memberships) {
        this.memberships = memberships;
    }

    private boolean requiresEmailVerification;
    private String message;
    private String rawToken;
    private String verificationLink;

    public boolean isRequiresEmailVerification() {
        return requiresEmailVerification;
    }

    public void setRequiresEmailVerification(boolean requiresEmailVerification) {
        this.requiresEmailVerification = requiresEmailVerification;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawToken() {
        return rawToken;
    }

    public void setRawToken(String rawToken) {
        this.rawToken = rawToken;
    }

    public String getVerificationLink() {
        return verificationLink;
    }

    public void setVerificationLink(String verificationLink) {
        this.verificationLink = verificationLink;
    }
}
