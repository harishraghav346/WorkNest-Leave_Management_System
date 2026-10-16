package com.examly.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "leave_audit_events")
public class LeaveAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(nullable = false)
    private Long leaveId;

    @Column(nullable = false, length = 32)
    private String eventType; // CREATED, UPDATED, APPROVED, REJECTED, CANCELLED

    @Column(nullable = false, length = 100)
    private String actorUsername;

    @Column(length = 50)
    private String actorRole;

    @Column(nullable = false)
    private Instant eventTimestamp;

    @Column(length = 32)
    private String fromStatus;

    @Column(nullable = false, length = 32)
    private String toStatus;

    private Integer previousBalance;

    private Integer newBalance;

    @Column(length = 1000)
    private String details;

    public LeaveAuditEvent() {
    }

    public LeaveAuditEvent(Long leaveId, String eventType, String actorUsername, String actorRole,
                            Instant eventTimestamp, String fromStatus, String toStatus,
                            Integer previousBalance, Integer newBalance, String details) {
        this.leaveId = leaveId;
        this.eventType = eventType;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.eventTimestamp = eventTimestamp;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.details = details;
    }

    public LeaveAuditEvent(Long organizationId, Long leaveId, String eventType, String actorUsername, String actorRole,
                            Instant eventTimestamp, String fromStatus, String toStatus,
                            Integer previousBalance, Integer newBalance, String details) {
        this.organizationId = organizationId;
        this.leaveId = leaveId;
        this.eventType = eventType;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.eventTimestamp = eventTimestamp;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(Long leaveId) {
        this.leaveId = leaveId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Integer getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(Integer previousBalance) {
        this.previousBalance = previousBalance;
    }

    public Integer getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(Integer newBalance) {
        this.newBalance = newBalance;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
