package com.examly.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
    name = "employee_profiles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_org_empid", columnNames = {"organization_id", "employee_id"}),
        @UniqueConstraint(name = "uk_employee_org_email", columnNames = {"organization_id", "work_email"})
    }
)
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false, unique = true)
    private OrganizationMembership membership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "work_email", nullable = false)
    private String workEmail;

    @Column(name = "department")
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Department departmentRef;

    @Column(name = "designation")
    private String designation;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "employment_status", nullable = false)
    private String employmentStatus = "FULL_TIME"; // FULL_TIME, PROBATION, CONTRACTOR, PART_TIME

    public EmployeeProfile() {
    }

    public EmployeeProfile(OrganizationMembership membership, Organization organization, String employeeId,
                           String fullName, String workEmail, String department, String designation,
                           LocalDate joiningDate, String employmentStatus) {
        this.membership = membership;
        this.organization = organization;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.workEmail = workEmail;
        this.department = department;
        this.designation = designation;
        this.joiningDate = joiningDate;
        this.employmentStatus = employmentStatus != null ? employmentStatus : "FULL_TIME";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrganizationMembership getMembership() {
        return membership;
    }

    public void setMembership(OrganizationMembership membership) {
        this.membership = membership;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getWorkEmail() {
        return workEmail;
    }

    public void setWorkEmail(String workEmail) {
        this.workEmail = workEmail;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Department getDepartmentRef() {
        return departmentRef;
    }

    public void setDepartmentRef(Department departmentRef) {
        if (departmentRef != null && this.organization != null && departmentRef.getOrganization() != null) {
            if (!this.organization.getId().equals(departmentRef.getOrganization().getId())) {
                throw new IllegalArgumentException("Cross-organization department assignment is strictly prohibited.");
            }
        }
        this.departmentRef = departmentRef;
        if (departmentRef != null && departmentRef.getName() != null) {
            this.department = departmentRef.getName();
        }
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus != null ? employmentStatus : "FULL_TIME";
    }
}
