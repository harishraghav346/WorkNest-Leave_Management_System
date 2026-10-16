package com.examly.springapp.model;

import java.util.ArrayList;
import java.util.List;

public class OrganizationMigrationReport {
    private boolean dryRun;
    private boolean success;
    private String message;
    private String organizationName;
    private String organizationSlug;
    private Long organizationId;
    private long totalUsersFound;
    private long usersPendingMigration;
    private long membershipsCreated;
    private long profilesCreated;
    private long profilesRepaired;
    private long previouslyMigratedUsers;

    private long totalLeavesFound;
    private long leavesPendingMigration;
    private long leavesMigrated;
    private long leavesPreviouslyMigrated;
    private long unresolvedLeaves;

    private long totalAuditEventsFound;
    private long auditEventsPendingMigration;
    private long auditEventsMigrated;
    private long auditEventsPreviouslyMigrated;
    private long unresolvedAuditEvents;

    private long totalNotificationsFound;
    private long notificationsPendingMigration;
    private long notificationsMigrated;
    private long notificationsPreviouslyMigrated;
    private long unresolvedNotifications;

    private List<String> plannedActions = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public OrganizationMigrationReport() {
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public long getTotalUsersFound() {
        return totalUsersFound;
    }

    public void setTotalUsersFound(long totalUsersFound) {
        this.totalUsersFound = totalUsersFound;
    }

    public long getUsersPendingMigration() {
        return usersPendingMigration;
    }

    public void setUsersPendingMigration(long usersPendingMigration) {
        this.usersPendingMigration = usersPendingMigration;
    }

    public long getMembershipsCreated() {
        return membershipsCreated;
    }

    public void setMembershipsCreated(long membershipsCreated) {
        this.membershipsCreated = membershipsCreated;
    }

    public long getProfilesCreated() {
        return profilesCreated;
    }

    public void setProfilesCreated(long profilesCreated) {
        this.profilesCreated = profilesCreated;
    }

    public long getProfilesRepaired() {
        return profilesRepaired;
    }

    public void setProfilesRepaired(long profilesRepaired) {
        this.profilesRepaired = profilesRepaired;
    }

    public long getPreviouslyMigratedUsers() {
        return previouslyMigratedUsers;
    }

    public void setPreviouslyMigratedUsers(long previouslyMigratedUsers) {
        this.previouslyMigratedUsers = previouslyMigratedUsers;
    }

    public long getTotalLeavesFound() {
        return totalLeavesFound;
    }

    public void setTotalLeavesFound(long totalLeavesFound) {
        this.totalLeavesFound = totalLeavesFound;
    }

    public long getLeavesPendingMigration() {
        return leavesPendingMigration;
    }

    public void setLeavesPendingMigration(long leavesPendingMigration) {
        this.leavesPendingMigration = leavesPendingMigration;
    }

    public long getLeavesMigrated() {
        return leavesMigrated;
    }

    public void setLeavesMigrated(long leavesMigrated) {
        this.leavesMigrated = leavesMigrated;
    }

    public long getLeavesPreviouslyMigrated() {
        return leavesPreviouslyMigrated;
    }

    public void setLeavesPreviouslyMigrated(long leavesPreviouslyMigrated) {
        this.leavesPreviouslyMigrated = leavesPreviouslyMigrated;
    }

    public long getUnresolvedLeaves() {
        return unresolvedLeaves;
    }

    public void setUnresolvedLeaves(long unresolvedLeaves) {
        this.unresolvedLeaves = unresolvedLeaves;
    }

    public long getTotalAuditEventsFound() {
        return totalAuditEventsFound;
    }

    public void setTotalAuditEventsFound(long totalAuditEventsFound) {
        this.totalAuditEventsFound = totalAuditEventsFound;
    }

    public long getAuditEventsPendingMigration() {
        return auditEventsPendingMigration;
    }

    public void setAuditEventsPendingMigration(long auditEventsPendingMigration) {
        this.auditEventsPendingMigration = auditEventsPendingMigration;
    }

    public long getAuditEventsMigrated() {
        return auditEventsMigrated;
    }

    public void setAuditEventsMigrated(long auditEventsMigrated) {
        this.auditEventsMigrated = auditEventsMigrated;
    }

    public long getAuditEventsPreviouslyMigrated() {
        return auditEventsPreviouslyMigrated;
    }

    public void setAuditEventsPreviouslyMigrated(long auditEventsPreviouslyMigrated) {
        this.auditEventsPreviouslyMigrated = auditEventsPreviouslyMigrated;
    }

    public long getUnresolvedAuditEvents() {
        return unresolvedAuditEvents;
    }

    public void setUnresolvedAuditEvents(long unresolvedAuditEvents) {
        this.unresolvedAuditEvents = unresolvedAuditEvents;
    }

    public long getTotalNotificationsFound() {
        return totalNotificationsFound;
    }

    public void setTotalNotificationsFound(long totalNotificationsFound) {
        this.totalNotificationsFound = totalNotificationsFound;
    }

    public long getNotificationsPendingMigration() {
        return notificationsPendingMigration;
    }

    public void setNotificationsPendingMigration(long notificationsPendingMigration) {
        this.notificationsPendingMigration = notificationsPendingMigration;
    }

    public long getNotificationsMigrated() {
        return notificationsMigrated;
    }

    public void setNotificationsMigrated(long notificationsMigrated) {
        this.notificationsMigrated = notificationsMigrated;
    }

    public long getNotificationsPreviouslyMigrated() {
        return notificationsPreviouslyMigrated;
    }

    public void setNotificationsPreviouslyMigrated(long notificationsPreviouslyMigrated) {
        this.notificationsPreviouslyMigrated = notificationsPreviouslyMigrated;
    }

    public long getUnresolvedNotifications() {
        return unresolvedNotifications;
    }

    public void setUnresolvedNotifications(long unresolvedNotifications) {
        this.unresolvedNotifications = unresolvedNotifications;
    }

    public List<String> getPlannedActions() {
        return plannedActions;
    }

    public void setPlannedActions(List<String> plannedActions) {
        this.plannedActions = plannedActions != null ? plannedActions : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
