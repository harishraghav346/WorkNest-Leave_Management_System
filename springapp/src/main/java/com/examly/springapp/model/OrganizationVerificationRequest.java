package com.examly.springapp.model;

public class OrganizationVerificationRequest {

    private String token;
    private String email;

    public OrganizationVerificationRequest() {}

    public OrganizationVerificationRequest(String token) {
        this.token = token;
    }

    public OrganizationVerificationRequest(String token, String email) {
        this.token = token;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
