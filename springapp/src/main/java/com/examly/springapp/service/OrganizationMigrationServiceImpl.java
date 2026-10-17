package com.examly.springapp.service;

import com.examly.springapp.model.EmployeeProfile;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.Notification;
import com.examly.springapp.model.Organization;
import com.examly.springapp.model.OrganizationMembership;
import com.examly.springapp.model.OrganizationMigrationReport;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.EmployeeProfileRepository;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.repository.NotificationRepository;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.OrganizationRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrganizationMigrationServiceImpl implements OrganizationMigrationService {

    public static final String DEFAULT_ORG_NAME = "WorkNest Primary Workspace";
    public static final String DEFAULT_ORG_SLUG = "worknest-primary";

    private final java.util.concurrent.atomic.AtomicBoolean isMigrationRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private LeaveAuditEventRepository leaveAuditEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizationMigrationReport previewMigration() {
        OrganizationMigrationReport report = new OrganizationMigrationReport();
        report.setDryRun(true);
        report.setOrganizationName(DEFAULT_ORG_NAME);
        report.setOrganizationSlug(DEFAULT_ORG_SLUG);

        List<User> allUsers = userRepository.findAll();
        report.setTotalUsersFound(allUsers.size());

        Optional<Organization> orgOpt = organizationRepository.findBySlug(DEFAULT_ORG_SLUG);
        Long targetOrgId = orgOpt.map(Organization::getId).orElse(null);
        if (targetOrgId != null) {
            report.setOrganizationId(targetOrgId);
            report.getPlannedActions().add("Default organization already exists: '" + DEFAULT_ORG_NAME + "' (ID #" + targetOrgId + ")");
        } else {
            report.getPlannedActions().add("Will create default organization: '" + DEFAULT_ORG_NAME + "' (slug: " + DEFAULT_ORG_SLUG + ")");
        }

        Set<String> simulatedEmails = new HashSet<>();
        Set<String> simulatedEmpIds = new HashSet<>();
        long pending = 0;
        long profilesToRepair = 0;
        long alreadyMapped = 0;

        for (User user : allUsers) {
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                report.getErrors().add("Inconsistent legacy record: User ID #" + user.getId() + " has a null or blank username and cannot be safely migrated.");
                continue;
            }

            Optional<OrganizationMembership> userMemOpt = membershipRepository.findByUserId(user.getId());
            if (userMemOpt.isPresent()) {
                OrganizationMembership mem = userMemOpt.get();
                // Validate membership organization relationship
                if (mem.getOrganization() == null || mem.getOrganization().getId() == null || !organizationRepository.existsById(mem.getOrganization().getId())) {
                    Long linkedOrgId = mem.getOrganization() != null ? mem.getOrganization().getId() : null;
                    report.getErrors().add("Inconsistent membership reference: User ID #" + user.getId() + " ('" + user.getUsername()
                            + "') is associated with membership #" + mem.getId() + " referencing non-existent or null organization ID #" + linkedOrgId + ".");
                    continue;
                }

                // Check profile
                Optional<EmployeeProfile> profOpt = employeeProfileRepository.findByMembershipId(mem.getId());
                if (profOpt.isPresent()) {
                    EmployeeProfile prof = profOpt.get();
                    if (prof.getOrganization() == null || !prof.getOrganization().getId().equals(mem.getOrganization().getId())) {
                        report.getErrors().add("Inconsistent relationship reference: EmployeeProfile #" + prof.getId() + " for user ID #" + user.getId()
                                + " belongs to organization ID #" + (prof.getOrganization() != null ? prof.getOrganization().getId() : "null")
                                + " which conflicts with membership organization ID #" + mem.getOrganization().getId() + ".");
                        continue;
                    }
                    alreadyMapped++;
                } else {
                    // Membership exists but profile is missing: partial migration recovery needed
                    profilesToRepair++;
                    String projectedEmail = generateUniqueWorkEmail(mem.getOrganization().getId(), user, simulatedEmails);
                    String projectedEmpId = generateUniqueEmployeeId(mem.getOrganization().getId(), user, simulatedEmpIds);
                    report.getPlannedActions().add(
                            "User #" + user.getId() + " ('" + user.getUsername() + "') has membership #" + mem.getId()
                                    + " in org '" + mem.getOrganization().getName() + "' but is missing EmployeeProfile -> Will repair profile with ID " + projectedEmpId + " and email " + projectedEmail
                    );
                }
            } else {
                // Neither membership nor profile exists: complete migration into default org
                pending++;
                boolean isAdmin = user.getRole() != null && (user.getRole().equalsIgnoreCase("admin") || user.getRole().equalsIgnoreCase("ROLE_ADMIN"));
                String targetRole = isAdmin ? "ORG_ADMIN" : "EMPLOYEE";
                String projectedEmail = generateUniqueWorkEmail(targetOrgId, user, simulatedEmails);
                String projectedEmpId = generateUniqueEmployeeId(targetOrgId, user, simulatedEmpIds);

                report.getPlannedActions().add(
                        "User #" + user.getId() + " ('" + user.getUsername() + "', role: " + user.getRole() + ") -> Map to "
                                + targetRole + " with ID " + projectedEmpId + " and email " + projectedEmail
                );
            }
        }

        report.setUsersPendingMigration(pending);
        report.setProfilesRepaired(profilesToRepair);
        report.setPreviouslyMigratedUsers(alreadyMapped);

        // --- Leaves Preview ---
        List<Leave> allLeaves = leaveRepository.findAll();
        report.setTotalLeavesFound(allLeaves.size());
        long leavesPending = 0;
        long leavesPreviouslyMigrated = 0;
        long unresolvedLeaves = 0;

        for (Leave leave : allLeaves) {
            if (leave.getOrganization() != null) {
                leavesPreviouslyMigrated++;
            } else {
                String emp = leave.getEmployeeName();
                if (emp == null || emp.isBlank()) {
                    unresolvedLeaves++;
                    report.getPlannedActions().add("Leave #" + leave.getId() + " has null or blank employeeName -> Unresolved");
                } else {
                    User user = userRepository.findByUsername(emp);
                    if (user == null) {
                        unresolvedLeaves++;
                        report.getPlannedActions().add("Leave #" + leave.getId() + " references non-existent employee '" + emp + "' -> Unresolved");
                    } else {
                        List<OrganizationMembership> userMems = membershipRepository.findAllByUserId(user.getId());
                        boolean belongsToOtherOrg = userMems.stream()
                                .anyMatch(m -> {
                                    if (m.getOrganization() == null) return false;
                                    if (targetOrgId != null) {
                                        return !targetOrgId.equals(m.getOrganization().getId());
                                    } else {
                                        return !DEFAULT_ORG_SLUG.equals(m.getOrganization().getSlug());
                                    }
                                });
                        boolean hasMultipleMemberships = userMems.size() > 1;

                        if (belongsToOtherOrg || hasMultipleMemberships) {
                            unresolvedLeaves++;
                            report.getPlannedActions().add("Leave #" + leave.getId() + " ('" + emp + "') belongs to user with cross-organization or ambiguous memberships -> Unresolved");
                        } else {
                            leavesPending++;
                            report.getPlannedActions().add("Leave #" + leave.getId() + " ('" + emp + "') -> Will associate with default organization '" + DEFAULT_ORG_NAME + "'");
                        }
                    }
                }
            }
        }
        report.setLeavesPendingMigration(leavesPending);
        report.setLeavesPreviouslyMigrated(leavesPreviouslyMigrated);
        report.setUnresolvedLeaves(unresolvedLeaves);

        // --- Audit Events Preview ---
        List<LeaveAuditEvent> allEvents = leaveAuditEventRepository.findAll();
        report.setTotalAuditEventsFound(allEvents.size());
        long auditEventsPending = 0;
        long auditEventsPreviouslyMigrated = 0;
        long unresolvedAuditEvents = 0;

        for (LeaveAuditEvent event : allEvents) {
            if (event.getOrganizationId() != null) {
                auditEventsPreviouslyMigrated++;
            } else {
                Optional<Leave> leaveOpt = leaveRepository.findById(event.getLeaveId());
                if (leaveOpt.isPresent()) {
                    auditEventsPending++;
                    report.getPlannedActions().add("LeaveAuditEvent #" + event.getId() + " for leave #" + event.getLeaveId() + " -> Will associate with organization");
                } else {
                    unresolvedAuditEvents++;
                    report.getPlannedActions().add("LeaveAuditEvent #" + event.getId() + " references non-existent leave #" + event.getLeaveId() + " -> Unresolved");
                }
            }
        }
        report.setAuditEventsPendingMigration(auditEventsPending);
        report.setAuditEventsPreviouslyMigrated(auditEventsPreviouslyMigrated);
        report.setUnresolvedAuditEvents(unresolvedAuditEvents);

        // --- Notifications Preview ---
        List<Notification> allNotifications = notificationRepository.findAll();
        report.setTotalNotificationsFound(allNotifications.size());
        long notificationsPending = 0;
        long notificationsPreviouslyMigrated = 0;
        long unresolvedNotifications = 0;

        for (Notification n : allNotifications) {
            if (n.getOrganizationId() != null) {
                notificationsPreviouslyMigrated++;
            } else {
                String recipient = n.getRecipientUsername();
                if (recipient != null && !recipient.isBlank()) {
                    User user = userRepository.findByUsername(recipient);
                    if (user != null) {
                        List<OrganizationMembership> userMems = membershipRepository.findAllByUserId(user.getId());
                        boolean belongsToOtherOrg = userMems.stream()
                                .anyMatch(m -> {
                                    if (m.getOrganization() == null) return false;
                                    if (targetOrgId != null) {
                                        return !targetOrgId.equals(m.getOrganization().getId());
                                    } else {
                                        return !DEFAULT_ORG_SLUG.equals(m.getOrganization().getSlug());
                                    }
                                });
                        boolean hasMultipleMemberships = userMems.size() > 1;

                        if (belongsToOtherOrg || hasMultipleMemberships) {
                            unresolvedNotifications++;
                            report.getPlannedActions().add("Notification #" + n.getId() + " recipient '" + recipient + "' belongs to another/multiple organizations -> Unresolved");
                        } else {
                            notificationsPending++;
                            report.getPlannedActions().add("Notification #" + n.getId() + " for recipient '" + recipient + "' -> Will associate with default organization");
                        }
                    } else {
                        unresolvedNotifications++;
                        report.getPlannedActions().add("Notification #" + n.getId() + " recipient '" + recipient + "' not found -> Unresolved");
                    }
                } else {
                    Long refLeaveId = extractLeaveIdFromMessage(n.getMessage());
                    if (refLeaveId != null && leaveRepository.existsById(refLeaveId)) {
                        notificationsPending++;
                        report.getPlannedActions().add("Notification #" + n.getId() + " references leave #" + refLeaveId + " -> Will associate with organization");
                    } else {
                        unresolvedNotifications++;
                        report.getPlannedActions().add("Notification #" + n.getId() + " has null recipient and no resolvable leave -> Unresolved");
                    }
                }
            }
        }
        report.setNotificationsPendingMigration(notificationsPending);
        report.setNotificationsPreviouslyMigrated(notificationsPreviouslyMigrated);
        report.setUnresolvedNotifications(unresolvedNotifications);

        if (!report.getErrors().isEmpty()) {
            report.setSuccess(false);
            report.setMessage("Dry-run validation found " + report.getErrors().size() + " blocking error(s). Migration will abort if executed without resolution.");
        } else {
            report.setSuccess(true);
            if (profilesToRepair > 0) {
                report.setMessage("Dry-run preview complete. " + pending + " user(s) ready for migration, " + profilesToRepair + " profile(s) need repair in '" + DEFAULT_ORG_NAME + "'.");
            } else {
                report.setMessage("Dry-run preview complete. " + pending + " user(s), " + leavesPending + " leave(s), " + auditEventsPending + " audit event(s), " + notificationsPending + " notification(s) ready for migration into '" + DEFAULT_ORG_NAME + "'.");
            }
        }

        return report;
    }

    @Override
    @Transactional
    public OrganizationMigrationReport executeMigration() {
        if (!isMigrationRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Migration execution rejected: Another migration is currently in progress. Concurrent execution is prohibited.");
        }
        try {
            return doExecuteMigration();
        } finally {
            isMigrationRunning.set(false);
        }
    }

    private OrganizationMigrationReport doExecuteMigration() {
        OrganizationMigrationReport report = new OrganizationMigrationReport();
        report.setDryRun(false);
        report.setOrganizationName(DEFAULT_ORG_NAME);
        report.setOrganizationSlug(DEFAULT_ORG_SLUG);

        List<User> allUsers = userRepository.findAll();
        report.setTotalUsersFound(allUsers.size());

        // Pre-validation pass: Check for blank usernames and inconsistent relationships before any writes
        for (User user : allUsers) {
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                report.getErrors().add("Aborting migration: User ID #" + user.getId() + " has null or blank username.");
            }
            Optional<OrganizationMembership> memOpt = membershipRepository.findByUserId(user.getId());
            if (memOpt.isPresent()) {
                OrganizationMembership mem = memOpt.get();
                if (mem.getOrganization() == null || mem.getOrganization().getId() == null || !organizationRepository.existsById(mem.getOrganization().getId())) {
                    Long linkedOrgId = mem.getOrganization() != null ? mem.getOrganization().getId() : null;
                    report.getErrors().add("Aborting migration: User ID #" + user.getId() + " ('" + user.getUsername()
                            + "') has membership #" + mem.getId() + " referencing non-existent or null organization ID #" + linkedOrgId + ".");
                } else {
                    Optional<EmployeeProfile> profOpt = employeeProfileRepository.findByMembershipId(mem.getId());
                    if (profOpt.isPresent()) {
                        EmployeeProfile prof = profOpt.get();
                        if (prof.getOrganization() == null || !prof.getOrganization().getId().equals(mem.getOrganization().getId())) {
                            report.getErrors().add("Aborting migration: EmployeeProfile #" + prof.getId() + " for user ID #" + user.getId()
                                    + " has organization ID #" + (prof.getOrganization() != null ? prof.getOrganization().getId() : "null")
                                    + " which conflicts with membership organization ID #" + mem.getOrganization().getId() + ".");
                        }
                    }
                }
            }
        }

        if (!report.getErrors().isEmpty()) {
            report.setSuccess(false);
            report.setMessage("Migration aborted: Inconsistent legacy user records detected. No data was modified.");
            return report;
        }

        // Get or create legacy default organization
        Organization org = organizationRepository.findBySlug(DEFAULT_ORG_SLUG).orElseGet(() ->
                organizationRepository.saveAndFlush(new Organization(DEFAULT_ORG_NAME, DEFAULT_ORG_SLUG, "ACTIVE", Instant.now())));
        report.setOrganizationId(org.getId());

        Set<String> sessionUsedEmails = new HashSet<>();
        Set<String> sessionUsedEmpIds = new HashSet<>();
        long membershipsCreated = 0;
        long profilesCreated = 0;
        long profilesRepaired = 0;
        long previouslyMigrated = 0;

        for (User user : allUsers) {
            Optional<OrganizationMembership> memOpt = membershipRepository.findByUserId(user.getId());
            if (memOpt.isPresent()) {
                OrganizationMembership existingMem = memOpt.get();
                Optional<EmployeeProfile> profOpt = employeeProfileRepository.findByMembershipId(existingMem.getId());
                if (profOpt.isPresent()) {
                    // Both membership and profile already exist and are valid: preserve them
                    previouslyMigrated++;
                    continue;
                }

                // Partial migration recovery: Membership exists, but EmployeeProfile is missing.
                // Safely create missing profile using the correct existing membership and organization.
                Organization targetOrgForProfile = existingMem.getOrganization();
                String empId = generateUniqueEmployeeId(targetOrgForProfile.getId(), user, sessionUsedEmpIds);
                String workEmail = generateUniqueWorkEmail(targetOrgForProfile.getId(), user, sessionUsedEmails);

                EmployeeProfile repairedProfile = new EmployeeProfile(
                        existingMem,
                        targetOrgForProfile,
                        empId,
                        user.getUsername(),
                        workEmail,
                        null,
                        null,
                        null,
                        "FULL_TIME"
                );
                employeeProfileRepository.saveAndFlush(repairedProfile);
                profilesRepaired++;
                continue;
            }

            // Neither membership nor profile exists: create both in default organization
            boolean isAdmin = user.getRole() != null && (user.getRole().equalsIgnoreCase("admin") || user.getRole().equalsIgnoreCase("ROLE_ADMIN"));
            String targetRole = isAdmin ? "ORG_ADMIN" : "EMPLOYEE";

            OrganizationMembership membership = new OrganizationMembership(org, user, targetRole, "ACTIVE", Instant.now());
            OrganizationMembership savedMembership = membershipRepository.saveAndFlush(membership);
            membershipsCreated++;

            String empId = generateUniqueEmployeeId(org.getId(), user, sessionUsedEmpIds);
            String workEmail = generateUniqueWorkEmail(org.getId(), user, sessionUsedEmails);

            EmployeeProfile profile = new EmployeeProfile(
                    savedMembership,
                    org,
                    empId,
                    user.getUsername(),
                    workEmail,
                    null,
                    null,
                    null,
                    "FULL_TIME"
            );
            employeeProfileRepository.saveAndFlush(profile);
            profilesCreated++;
        }

        report.setMembershipsCreated(membershipsCreated);
        report.setProfilesCreated(profilesCreated);
        report.setProfilesRepaired(profilesRepaired);
        report.setPreviouslyMigratedUsers(previouslyMigrated);

        // Verification assertion: newly created memberships must match newly created profiles
        if (membershipsCreated != profilesCreated) {
            throw new IllegalStateException("Integrity failure: Memberships created (" + membershipsCreated + ") does not match profiles created (" + profilesCreated + "). Transaction rolled back.");
        }

        // --- Migrate Leaves ---
        List<Leave> allLeaves = leaveRepository.findAll();
        report.setTotalLeavesFound(allLeaves.size());
        long leavesMigrated = 0;
        long leavesPreviouslyMigrated = 0;
        long unresolvedLeaves = 0;

        for (Leave leave : allLeaves) {
            if (leave.getOrganization() != null) {
                leavesPreviouslyMigrated++;
            } else {
                String emp = leave.getEmployeeName();
                if (emp == null || emp.isBlank()) {
                    unresolvedLeaves++;
                } else {
                    User user = userRepository.findByUsername(emp);
                    if (user == null) {
                        unresolvedLeaves++;
                    } else {
                        List<OrganizationMembership> userMems = membershipRepository.findAllByUserId(user.getId());
                        boolean belongsToOtherOrg = userMems.stream()
                                .anyMatch(m -> m.getOrganization() != null && org.getId() != null && !org.getId().equals(m.getOrganization().getId()));
                        boolean hasMultipleMemberships = userMems.size() > 1;

                        if (belongsToOtherOrg || hasMultipleMemberships) {
                            unresolvedLeaves++;
                        } else {
                            leave.setOrganization(org);
                            leaveRepository.saveAndFlush(leave);
                            leavesMigrated++;
                        }
                    }
                }
            }
        }
        report.setLeavesMigrated(leavesMigrated);
        report.setLeavesPreviouslyMigrated(leavesPreviouslyMigrated);
        report.setUnresolvedLeaves(unresolvedLeaves);

        // --- Migrate Audit Events ---
        List<LeaveAuditEvent> allEvents = leaveAuditEventRepository.findAll();
        report.setTotalAuditEventsFound(allEvents.size());
        long auditEventsMigrated = 0;
        long auditEventsPreviouslyMigrated = 0;
        long unresolvedAuditEvents = 0;

        for (LeaveAuditEvent event : allEvents) {
            if (event.getOrganizationId() != null) {
                auditEventsPreviouslyMigrated++;
            } else {
                Optional<Leave> leaveOpt = leaveRepository.findById(event.getLeaveId());
                if (leaveOpt.isPresent()) {
                    Leave l = leaveOpt.get();
                    Long assocOrgId = l.getOrganization() != null ? l.getOrganization().getId() : org.getId();
                    event.setOrganizationId(assocOrgId);
                    leaveAuditEventRepository.saveAndFlush(event);
                    auditEventsMigrated++;
                } else {
                    unresolvedAuditEvents++;
                }
            }
        }
        report.setAuditEventsMigrated(auditEventsMigrated);
        report.setAuditEventsPreviouslyMigrated(auditEventsPreviouslyMigrated);
        report.setUnresolvedAuditEvents(unresolvedAuditEvents);

        // --- Migrate Notifications ---
        List<Notification> allNotifications = notificationRepository.findAll();
        report.setTotalNotificationsFound(allNotifications.size());
        long notificationsMigrated = 0;
        long notificationsPreviouslyMigrated = 0;
        long unresolvedNotifications = 0;

        for (Notification n : allNotifications) {
            if (n.getOrganizationId() != null) {
                notificationsPreviouslyMigrated++;
            } else {
                String recipient = n.getRecipientUsername();
                if (recipient != null && !recipient.isBlank()) {
                    User user = userRepository.findByUsername(recipient);
                    if (user != null) {
                        List<OrganizationMembership> userMems = membershipRepository.findAllByUserId(user.getId());
                        boolean belongsToOtherOrg = userMems.stream()
                                .anyMatch(m -> m.getOrganization() != null && org.getId() != null && !org.getId().equals(m.getOrganization().getId()));
                        boolean hasMultipleMemberships = userMems.size() > 1;

                        if (belongsToOtherOrg || hasMultipleMemberships) {
                            unresolvedNotifications++;
                        } else {
                            n.setOrganizationId(org.getId());
                            notificationRepository.saveAndFlush(n);
                            notificationsMigrated++;
                        }
                    } else {
                        unresolvedNotifications++;
                    }
                } else {
                    Long refLeaveId = extractLeaveIdFromMessage(n.getMessage());
                    if (refLeaveId != null) {
                        Optional<Leave> refLeaveOpt = leaveRepository.findById(refLeaveId);
                        if (refLeaveOpt.isPresent()) {
                            Long assocOrgId = refLeaveOpt.get().getOrganization() != null ? refLeaveOpt.get().getOrganization().getId() : org.getId();
                            n.setOrganizationId(assocOrgId);
                            notificationRepository.saveAndFlush(n);
                            notificationsMigrated++;
                        } else {
                            unresolvedNotifications++;
                        }
                    } else {
                        unresolvedNotifications++;
                    }
                }
            }
        }
        report.setNotificationsMigrated(notificationsMigrated);
        report.setNotificationsPreviouslyMigrated(notificationsPreviouslyMigrated);
        report.setUnresolvedNotifications(unresolvedNotifications);

        report.setSuccess(true);
        if (profilesRepaired > 0) {
            report.setMessage("Migration completed successfully. " + membershipsCreated + " new membership(s)/profile(s) created, " + profilesRepaired + " profile(s) repaired, " + leavesMigrated + " leave(s), " + auditEventsMigrated + " audit event(s), " + notificationsMigrated + " notification(s) migrated in '" + DEFAULT_ORG_NAME + "'.");
        } else {
            report.setMessage("Migration completed successfully. " + membershipsCreated + " new membership(s)/profile(s) created, " + leavesMigrated + " leave(s), " + auditEventsMigrated + " audit event(s), " + notificationsMigrated + " notification(s) migrated in '" + DEFAULT_ORG_NAME + "'.");
        }
        return report;
    }

    private Long extractLeaveIdFromMessage(String message) {
        if (message == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("#(\\d+)").matcher(message);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Deterministically generates a unique work email scoped to the organization.
     * Note: Synthetic addresses ending in '@worknest.local' are legacy placeholders and are not real, verified, or deliverable email addresses.
     */
    private String generateUniqueWorkEmail(Long orgId, User user, Set<String> sessionUsedEmails) {
        String username = user.getUsername() != null ? user.getUsername().trim() : "";

        // 1. If username is already a valid email, check if it can be preserved
        if (username.contains("@") && !username.startsWith("@") && !username.endsWith("@")) {
            String candidate = username.toLowerCase();
            boolean taken = (orgId != null && employeeProfileRepository.existsByOrganizationIdAndWorkEmail(orgId, candidate))
                    || sessionUsedEmails.contains(candidate);
            if (!taken) {
                sessionUsedEmails.add(candidate);
                return candidate;
            }
        }

        // 2. Extract base name for deterministic placeholder generation
        String rawBase = username.contains("@") ? username.substring(0, username.indexOf('@')) : username;
        String base = rawBase.replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();
        if (base.isBlank()) {
            base = "user" + (user.getId() != null ? user.getId() : "");
        }

        // 3. For non-email usernames, attempt clean standard fallback first
        if (!username.contains("@")) {
            String standardFallback = base + "@worknest.local";
            boolean taken = (orgId != null && employeeProfileRepository.existsByOrganizationIdAndWorkEmail(orgId, standardFallback))
                    || sessionUsedEmails.contains(standardFallback);
            if (!taken) {
                sessionUsedEmails.add(standardFallback);
                return standardFallback;
            }
        }

        // 4. Deterministic collision loop (up to 50 attempts)
        for (int attempt = 1; attempt <= 50; attempt++) {
            String candidateEmail = (attempt == 1)
                    ? base + "." + (user.getId() != null ? user.getId() : "0") + "@worknest.local"
                    : base + "." + (user.getId() != null ? user.getId() : "0") + "." + attempt + "@worknest.local";

            boolean taken = (orgId != null && employeeProfileRepository.existsByOrganizationIdAndWorkEmail(orgId, candidateEmail))
                    || sessionUsedEmails.contains(candidateEmail);
            if (!taken) {
                sessionUsedEmails.add(candidateEmail);
                return candidateEmail;
            }
        }

        throw new IllegalStateException("Failed to generate a unique work email for user ID #" + user.getId()
                + " ('" + user.getUsername() + "') in organization ID #" + orgId + " after 50 deterministic attempts. Manual resolution required.");
    }

    /**
     * Deterministically generates a unique employee ID scoped to the organization.
     */
    private String generateUniqueEmployeeId(Long orgId, User user, Set<String> sessionUsedEmpIds) {
        String defaultEmpId = "EMP-" + String.format("%04d", user.getId() != null ? user.getId() : 0);
        boolean taken = (orgId != null && employeeProfileRepository.existsByOrganizationIdAndEmployeeId(orgId, defaultEmpId))
                || sessionUsedEmpIds.contains(defaultEmpId);
        if (!taken) {
            sessionUsedEmpIds.add(defaultEmpId);
            return defaultEmpId;
        }

        for (int attempt = 1; attempt <= 50; attempt++) {
            String candidateId = defaultEmpId + "-" + attempt;
            boolean candidateTaken = (orgId != null && employeeProfileRepository.existsByOrganizationIdAndEmployeeId(orgId, candidateId))
                    || sessionUsedEmpIds.contains(candidateId);
            if (!candidateTaken) {
                sessionUsedEmpIds.add(candidateId);
                return candidateId;
            }
        }

        throw new IllegalStateException("Failed to generate a unique employee ID for user ID #" + user.getId()
                + " in organization ID #" + orgId + " after 50 deterministic attempts. Manual resolution required.");
    }
}
