package com.examly.springapp.model;

import java.time.Instant;

public class LeaveAuditEventDTO {

    private Long id;
    private Long leaveId;
    private String eventType;
    private String actorUsername;
    private String actorRole;
    private Instant eventTimestamp;
    private String fromStatus;
    private String toStatus;
    private Integer previousBalance;
    private Integer newBalance;
    private String details;

    public LeaveAuditEventDTO() {
    }

    public LeaveAuditEventDTO(LeaveAuditEvent event) {
        if (event != null) {
            this.id = event.getId();
            this.leaveId = event.getLeaveId();
            this.eventType = event.getEventType();
            this.actorUsername = event.getActorUsername();
            this.actorRole = event.getActorRole();
            this.eventTimestamp = event.getEventTimestamp();
            this.fromStatus = event.getFromStatus();
            this.toStatus = event.getToStatus();
            this.previousBalance = event.getPreviousBalance();
            this.newBalance = event.getNewBalance();
            this.details = event.getDetails();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
