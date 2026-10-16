package com.examly.springapp.model;

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
    name = "organization_memberships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_membership_org_user", columnNames = {"organization_id", "user_id"})
    }
)
public class OrganizationMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String role; // ORG_ADMIN, HR_ADMIN, MANAGER, EMPLOYEE

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INVITED, DEACTIVATED

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    public OrganizationMembership() {
    }

    public OrganizationMembership(Organization organization, User user, String role, String status, Instant joinedAt) {
        this.organization = organization;
        this.user = user;
        this.role = role != null ? role.toUpperCase() : "EMPLOYEE";
        this.status = status != null ? status : "ACTIVE";
        this.joinedAt = joinedAt != null ? joinedAt : Instant.now();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase() : "EMPLOYEE";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
