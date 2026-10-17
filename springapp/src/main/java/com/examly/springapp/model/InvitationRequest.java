package com.examly.springapp.model;

public class InvitationRequest {
    private String employeeId;
    private String email;
    private String fullName;
    private String role = "EMPLOYEE";
    private Long departmentId;

    public InvitationRequest() {
    }

    public InvitationRequest(String employeeId, String email, String fullName, String role, Long departmentId) {
        this.employeeId = employeeId;
        this.email = email;
        this.fullName = fullName;
        this.role = role != null ? role.toUpperCase() : "EMPLOYEE";
        this.departmentId = departmentId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase() : "EMPLOYEE";
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
