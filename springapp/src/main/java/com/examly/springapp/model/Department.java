package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "departments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_org_name", columnNames = {"organization_id", "name"})
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column(name = "code")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_membership_id")
    private OrganizationMembership managerMembership;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Department() {
    }

    public Department(Organization organization, String name, String code, OrganizationMembership managerMembership) {
        this.organization = organization;
        this.name = name;
        this.code = code;
        setManagerMembership(managerMembership);
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
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

    public OrganizationMembership getManagerMembership() {
        return managerMembership;
    }

    public void setManagerMembership(OrganizationMembership managerMembership) {
        if (managerMembership != null && this.organization != null && managerMembership.getOrganization() != null) {
            if (!this.organization.getId().equals(managerMembership.getOrganization().getId())) {
                throw new IllegalArgumentException("Cross-organization manager assignment is strictly prohibited.");
            }
        }
        this.managerMembership = managerMembership;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    public String getManagerUsername() {
        return (managerMembership != null && managerMembership.getUser() != null) 
                ? managerMembership.getUser().getUsername() : null;
    }
}
