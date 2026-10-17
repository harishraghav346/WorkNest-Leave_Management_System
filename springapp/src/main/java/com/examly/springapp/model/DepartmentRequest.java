package com.examly.springapp.model;

public class DepartmentRequest {
    private String name;
    private String code;
    private Long managerMembershipId;

    public DepartmentRequest() {
    }

    public DepartmentRequest(String name, String code, Long managerMembershipId) {
        this.name = name;
        this.code = code;
        this.managerMembershipId = managerMembershipId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getManagerMembershipId() {
        return managerMembershipId;
    }

    public void setManagerMembershipId(Long managerMembershipId) {
        this.managerMembershipId = managerMembershipId;
    }
}
