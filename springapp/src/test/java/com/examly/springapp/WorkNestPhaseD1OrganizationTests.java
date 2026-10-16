package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.EmployeeProfile;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.Organization;
import com.examly.springapp.model.OrganizationMembership;
import com.examly.springapp.model.OrganizationMigrationReport;
import com.examly.springapp.model.User;
import com.examly.springapp.model.Notification;
import com.examly.springapp.repository.EmployeeProfileRepository;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.repository.NotificationRepository;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.OrganizationRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.OrganizationMigrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkNestPhaseD1OrganizationTests {

    @Autowired
    private MockMvc mockMvc;

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
    private LeaveAuditEventRepository auditEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrganizationMigrationService migrationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtUtil.generateToken("d1_admin", "admin");
        employeeToken = "Bearer " + jwtUtil.generateToken("d1_employee", "employee");
    }

    @Test
    @DisplayName("D1.1: Organization creation and unique slug constraints")
    void testCreateOrganizationAndUniqueSlugConstraint() {
        String slug = "org-test-" + System.currentTimeMillis();
        Organization org1 = new Organization("Test Org 1", slug, "ACTIVE", Instant.now());
        Organization saved1 = organizationRepository.save(org1);
        assertNotNull(saved1.getId());
        assertEquals(slug, saved1.getSlug());

        // Attempting to save another organization with identical slug must fail unique constraint
        Organization orgDuplicateSlug = new Organization("Test Org Duplicate", slug, "ACTIVE", Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> {
            organizationRepository.saveAndFlush(orgDuplicateSlug);
        }, "Duplicate organization slug must trigger DataIntegrityViolationException");
    }

    @Test
    @DisplayName("D1.2: Membership relationships and role validation")
    void testOrganizationMembershipRelationshipsAndRoles() {
        String testUser = "mem_user_" + System.currentTimeMillis();
        User user = userRepository.save(new User(testUser, passwordEncoder.encode("pass"), "employee"));

        Organization org = organizationRepository.save(
                new Organization("Membership Test Org", "mem-org-" + System.currentTimeMillis(), "ACTIVE", Instant.now())
        );

        OrganizationMembership membership = new OrganizationMembership(org, user, "HR_ADMIN", "ACTIVE", Instant.now());
        OrganizationMembership savedMem = membershipRepository.save(membership);

        assertNotNull(savedMem.getId());
        assertEquals("HR_ADMIN", savedMem.getRole());
        assertEquals(org.getId(), savedMem.getOrganization().getId());
        assertEquals(user.getId(), savedMem.getUser().getId());

        // Duplicate membership for same user in same org must violate unique constraint
        OrganizationMembership duplicateMem = new OrganizationMembership(org, user, "EMPLOYEE", "ACTIVE", Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> {
            membershipRepository.saveAndFlush(duplicateMem);
        }, "Duplicate membership in same org for same user must be rejected");
    }

    @Test
    @DisplayName("D1.3: Employee profile relationships and org-scoped uniqueness")
    void testEmployeeProfileRelationshipsAndOrgScopedUniqueness() {
        long ts = System.currentTimeMillis();
        Organization orgA = organizationRepository.save(new Organization("Org Alpha", "org-alpha-" + ts, "ACTIVE", Instant.now()));
        Organization orgB = organizationRepository.save(new Organization("Org Beta", "org-beta-" + ts, "ACTIVE", Instant.now()));

        User userA1 = userRepository.save(new User("userA1_" + ts, passwordEncoder.encode("pass"), "employee"));
        User userA2 = userRepository.save(new User("userA2_" + ts, passwordEncoder.encode("pass"), "employee"));
        User userB1 = userRepository.save(new User("userB1_" + ts, passwordEncoder.encode("pass"), "employee"));

        OrganizationMembership memA1 = membershipRepository.save(new OrganizationMembership(orgA, userA1, "EMPLOYEE", "ACTIVE", Instant.now()));
        OrganizationMembership memA2 = membershipRepository.save(new OrganizationMembership(orgA, userA2, "EMPLOYEE", "ACTIVE", Instant.now()));
        OrganizationMembership memB1 = membershipRepository.save(new OrganizationMembership(orgB, userB1, "EMPLOYEE", "ACTIVE", Instant.now()));

        // Profile A1 in Org A
        EmployeeProfile profA1 = new EmployeeProfile(
                memA1, orgA, "EMP-100", "Alice Alpha", "shared@worknest.test", "Engineering", "Senior Dev", LocalDate.of(2025, 1, 15), "FULL_TIME"
        );
        employeeProfileRepository.saveAndFlush(profA1);

        // In Org A, another employee with same employeeId "EMP-100" must be rejected
        EmployeeProfile profDuplicateId = new EmployeeProfile(
                memA2, orgA, "EMP-100", "Bob Alpha", "bob@worknest.test", "Design", "Lead", LocalDate.of(2025, 2, 1), "FULL_TIME"
        );
        assertThrows(DataIntegrityViolationException.class, () -> {
            employeeProfileRepository.saveAndFlush(profDuplicateId);
        }, "Employee ID duplicate within same organization must fail");

        // In Org B (different org), an employee CAN reuse employeeId "EMP-100" and workEmail "shared@worknest.test"
        EmployeeProfile profB1 = new EmployeeProfile(
                memB1, orgB, "EMP-100", "Charlie Beta", "shared@worknest.test", "Operations", "Manager", LocalDate.of(2025, 3, 1), "FULL_TIME"
        );
        EmployeeProfile savedB1 = employeeProfileRepository.saveAndFlush(profB1);
        assertNotNull(savedB1.getId());
        assertEquals("EMP-100", savedB1.getEmployeeId());
        assertEquals("shared@worknest.test", savedB1.getWorkEmail());
    }

    @Test
    @DisplayName("D1.4: Migration dry-run behavior does not write to database")
    void testMigrationDryRunDoesNotModifyDatabase() {
        long orgCountBefore = organizationRepository.count();
        long memCountBefore = membershipRepository.count();
        long profCountBefore = employeeProfileRepository.count();

        OrganizationMigrationReport report = migrationService.previewMigration();

        assertTrue(report.isDryRun(), "Report must indicate dry-run mode");
        assertTrue(report.isSuccess(), "Dry-run validation should succeed");
        assertNotNull(report.getPlannedActions());
        assertFalse(report.getPlannedActions().isEmpty());

        // Verify zero database writes occurred during preview
        assertEquals(orgCountBefore, organizationRepository.count(), "Organization count must not change during dry-run");
        assertEquals(memCountBefore, membershipRepository.count(), "Membership count must not change during dry-run");
        assertEquals(profCountBefore, employeeProfileRepository.count(), "Profile count must not change during dry-run");
    }

    @Test
    @DisplayName("D1.5: Migration execution and idempotency")
    void testMigrationExecutionAndIdempotency() {
        // Create test legacy users
        String u1Name = "legacy_exec_emp_" + System.currentTimeMillis();
        String u2Name = "legacy_exec_adm_" + System.currentTimeMillis();
        userRepository.save(new User(u1Name, passwordEncoder.encode("p1"), "employee"));
        userRepository.save(new User(u2Name, passwordEncoder.encode("p2"), "admin"));

        // 1. First execution
        OrganizationMigrationReport report1 = migrationService.executeMigration();
        assertFalse(report1.isDryRun());
        assertTrue(report1.isSuccess());
        assertTrue(report1.getMembershipsCreated() >= 2);
        assertEquals(report1.getMembershipsCreated(), report1.getProfilesCreated());

        Optional<Organization> defaultOrg = organizationRepository.findBySlug("worknest-primary");
        assertTrue(defaultOrg.isPresent(), "Default organization 'worknest-primary' must exist");

        long memCountAfterFirst = membershipRepository.count();
        long profCountAfterFirst = employeeProfileRepository.count();

        // 2. Second execution (Idempotency test)
        OrganizationMigrationReport report2 = migrationService.executeMigration();
        assertTrue(report2.isSuccess());
        assertEquals(0, report2.getMembershipsCreated(), "Subsequent migration must create 0 new memberships");
        assertEquals(0, report2.getProfilesCreated(), "Subsequent migration must create 0 new profiles");
        assertTrue(report2.getPreviouslyMigratedUsers() >= 2);

        assertEquals(memCountAfterFirst, membershipRepository.count(), "Database membership count must remain unchanged");
        assertEquals(profCountAfterFirst, employeeProfileRepository.count(), "Database profile count must remain unchanged");
    }

    @Test
    @DisplayName("D1.6: Preservation of user credentials, roles, and leave balances during migration")
    void testPreservationOfLegacyUserCredentialsAndBalances() {
        String testUser = "balance_preserve_" + System.currentTimeMillis();
        String rawPass = "SecretSecurePass123!";
        String encodedPass = passwordEncoder.encode(rawPass);

        User legacyUser = new User(testUser, encodedPass, "employee");
        legacyUser.setLeaveBalance(14); // specific non-default balance
        User savedUser = userRepository.save(legacyUser);

        // Execute migration
        OrganizationMigrationReport report = migrationService.executeMigration();
        assertTrue(report.isSuccess());

        // Reload user from database and verify integrity
        User reloaded = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals(savedUser.getId(), reloaded.getId());
        assertEquals(testUser, reloaded.getUsername());
        assertEquals(encodedPass, reloaded.getPassword());
        assertTrue(passwordEncoder.matches(rawPass, reloaded.getPassword()), "BCrypt password verification must remain valid");
        assertEquals("employee", reloaded.getRole());
        assertEquals(14, reloaded.getLeaveBalance(), "Leave balance must be preserved exactly");
    }

    @Test
    @DisplayName("D1.7: Preservation of leave request records, daysCount, and audit events")
    void testPreservationOfLeaveRequestsAndAuditEvents() {
        String testUser = "leave_preserve_" + System.currentTimeMillis();
        userRepository.save(new User(testUser, passwordEncoder.encode("pass"), "employee"));

        Leave leave = new Leave();
        leave.setEmployeeName(testUser);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-06");
        leave.setDaysCount(5); // Phase A1 weekday-calculated count
        leave.setStatus("Approved");
        leave.setReason("Preserved historical reason");
        Leave savedLeave = leaveRepository.save(leave);

        LeaveAuditEvent audit = new LeaveAuditEvent(
                savedLeave.getId(), "APPROVED", "admin", "ADMIN", Instant.now(),
                "Pending", "Approved", 20, 15, "Approved historical leave"
        );
        LeaveAuditEvent savedAudit = auditEventRepository.save(audit);

        // Run migration
        OrganizationMigrationReport report = migrationService.executeMigration();
        assertTrue(report.isSuccess());

        // Verify leave request was untouched
        Leave reloadedLeave = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals(savedLeave.getId(), reloadedLeave.getId());
        assertEquals("Approved", reloadedLeave.getStatus());
        assertEquals(5, reloadedLeave.getDaysCount());
        assertEquals("2026-11-02", reloadedLeave.getStartDate());
        assertEquals("2026-11-06", reloadedLeave.getEndDate());

        // Verify audit event was untouched
        LeaveAuditEvent reloadedAudit = auditEventRepository.findById(savedAudit.getId()).orElseThrow();
        assertEquals(savedAudit.getId(), reloadedAudit.getId());
        assertEquals(savedLeave.getId(), reloadedAudit.getLeaveId());
        assertEquals("APPROVED", reloadedAudit.getEventType());
        assertEquals(20, reloadedAudit.getPreviousBalance());
        assertEquals(15, reloadedAudit.getNewBalance());
    }

    @Test
    @DisplayName("D1.8: Migration detects and aborts on inconsistent legacy user records")
    void testMigrationDetectsInconsistentLegacyUser() {
        // Save an invalid user with blank username
        User invalidUser = new User();
        invalidUser.setUsername("   ");
        invalidUser.setPassword(passwordEncoder.encode("pass"));
        invalidUser.setRole("employee");
        User savedInvalid = userRepository.save(invalidUser);

        try {
            OrganizationMigrationReport preview = migrationService.previewMigration();
            assertFalse(preview.isSuccess(), "Preview should flag inconsistent user");
            assertFalse(preview.getErrors().isEmpty());

            OrganizationMigrationReport result = migrationService.executeMigration();
            assertFalse(result.isSuccess(), "Execution should abort on inconsistent user");
            assertTrue(result.getMessage().contains("Inconsistent legacy user records detected"));
        } finally {
            // Cleanup invalid test user to restore clean state
            userRepository.delete(savedInvalid);
        }
    }

    @Test
    @DisplayName("D1.9: Admin migration endpoints authorization checks for preview and execute")
    void testAdminMigrationEndpointsSecurity() throws Exception {
        // 1. GET /admin/migration/preview
        // Anonymous request must be 401
        mockMvc.perform(get("/admin/migration/preview"))
                .andExpect(status().isUnauthorized());

        // Employee request must be 403
        mockMvc.perform(get("/admin/migration/preview")
                .header("Authorization", employeeToken))
                .andExpect(status().isForbidden());

        // Admin request must be 200 OK
        mockMvc.perform(get("/admin/migration/preview")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.organizationSlug").value("worknest-primary"));

        // 2. POST /admin/migration/execute
        // Anonymous request must be 401
        mockMvc.perform(post("/admin/migration/execute"))
                .andExpect(status().isUnauthorized());

        // Employee request must be 403
        mockMvc.perform(post("/admin/migration/execute")
                .header("Authorization", employeeToken))
                .andExpect(status().isForbidden());

        // Admin request must be 200 OK
        mockMvc.perform(post("/admin/migration/execute")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("D1.10: Partial migration recovery repairs missing EmployeeProfile safely and idempotently")
    void testPartialMigrationRecoveryMissingProfile() {
        long ts = System.currentTimeMillis();
        Organization org = organizationRepository.findBySlug("worknest-primary")
                .orElseGet(() -> organizationRepository.save(new Organization("WorkNest Primary Workspace", "worknest-primary", "ACTIVE", Instant.now())));

        // Create user with existing membership but missing profile
        User user = userRepository.save(new User("partial_user_" + ts, passwordEncoder.encode("pass"), "employee"));
        OrganizationMembership mem = membershipRepository.save(new OrganizationMembership(org, user, "EMPLOYEE", "ACTIVE", Instant.now()));

        long profCountBefore = employeeProfileRepository.count();
        long memCountBefore = membershipRepository.count();

        // 1. Dry run preview must detect missing profile and report it as needing repair
        OrganizationMigrationReport preview = migrationService.previewMigration();
        assertTrue(preview.isDryRun());
        assertTrue(preview.isSuccess());
        assertTrue(preview.getProfilesRepaired() >= 1, "Preview must report at least 1 profile to repair");
        // Verify zero writes occurred during preview
        assertEquals(profCountBefore, employeeProfileRepository.count(), "Preview must perform zero profile writes");
        assertEquals(memCountBefore, membershipRepository.count(), "Preview must perform zero membership writes");

        // 2. Execute migration: must repair the missing profile
        OrganizationMigrationReport execReport = migrationService.executeMigration();
        assertTrue(execReport.isSuccess());
        assertTrue(execReport.getProfilesRepaired() >= 1, "Execute report must record repaired profiles");

        // Verify the profile was created and correctly linked
        Optional<EmployeeProfile> repairedProfOpt = employeeProfileRepository.findByMembershipId(mem.getId());
        assertTrue(repairedProfOpt.isPresent(), "Repaired EmployeeProfile must now exist for membership");
        EmployeeProfile repairedProfile = repairedProfOpt.get();
        assertEquals(mem.getId(), repairedProfile.getMembership().getId());
        assertEquals(org.getId(), repairedProfile.getOrganization().getId());
        assertEquals(user.getUsername(), repairedProfile.getFullName());
        assertNotNull(repairedProfile.getWorkEmail());
        assertNotNull(repairedProfile.getEmployeeId());

        // Verify membership was NOT duplicated
        assertEquals(memCountBefore, membershipRepository.count(), "Membership must not be duplicated");

        // 3. Repeated execution (idempotency verification after recovery)
        OrganizationMigrationReport rerunReport = migrationService.executeMigration();
        assertTrue(rerunReport.isSuccess());
        assertEquals(0, rerunReport.getProfilesRepaired(), "Subsequent migration must have 0 profiles to repair");
        assertEquals(0, rerunReport.getMembershipsCreated(), "Subsequent migration must create 0 memberships");
        assertEquals(0, rerunReport.getProfilesCreated(), "Subsequent migration must create 0 profiles");

        // Verify repaired profile data was not altered
        EmployeeProfile reloadedProf = employeeProfileRepository.findById(repairedProfile.getId()).orElseThrow();
        assertEquals(repairedProfile.getWorkEmail(), reloadedProf.getWorkEmail());
        assertEquals(repairedProfile.getEmployeeId(), reloadedProf.getEmployeeId());
    }

    @Test
    @DisplayName("D1.11: Migration detects inconsistent organization references and aborts safely")
    void testMigrationDetectsInconsistentOrganizationReference() {
        long ts = System.currentTimeMillis();
        // Create two organizations
        Organization orgA = organizationRepository.save(new Organization("Org A " + ts, "org-a-" + ts, "ACTIVE", Instant.now()));
        Organization orgB = organizationRepository.save(new Organization("Org B " + ts, "org-b-" + ts, "ACTIVE", Instant.now()));

        User conflictedUser = userRepository.save(new User("conflicted_user_" + ts, passwordEncoder.encode("pass"), "employee"));
        // Create membership in Org A, but profile referencing Org B (inconsistent relationship)
        OrganizationMembership mem = membershipRepository.save(new OrganizationMembership(orgA, conflictedUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        EmployeeProfile prof = employeeProfileRepository.save(new EmployeeProfile(
                mem, orgB, "EMP-CONF-" + ts, conflictedUser.getUsername(), "conflicted_" + ts + "@worknest.local", null, null, null, "FULL_TIME"
        ));

        try {
            // Preview should detect conflict and flag error
            OrganizationMigrationReport preview = migrationService.previewMigration();
            assertFalse(preview.isSuccess(), "Preview should detect conflicting organization membership/profile reference");
            assertTrue(preview.getErrors().stream().anyMatch(e -> e.contains("Inconsistent relationship reference")));

            // Execute should abort cleanly without modifying data
            OrganizationMigrationReport execReport = migrationService.executeMigration();
            assertFalse(execReport.isSuccess(), "Execute must abort when conflicting organization reference exists");
            assertTrue(execReport.getMessage().contains("Inconsistent legacy user records detected"));
        } finally {
            // Clean up test entities
            employeeProfileRepository.delete(prof);
            membershipRepository.delete(mem);
            userRepository.delete(conflictedUser);
            organizationRepository.delete(orgA);
            organizationRepository.delete(orgB);
        }
    }

    @Test
    @DisplayName("D1.12: Robust deterministic email collision handling")
    void testDeterministicEmailCollisionHandling() {
        long ts = System.currentTimeMillis();
        // Create two users with the identical username base
        String dupeUsername = "dupeuser_" + ts;
        User user1 = userRepository.save(new User(dupeUsername, passwordEncoder.encode("p1"), "employee"));
        User user2 = userRepository.save(new User(dupeUsername, passwordEncoder.encode("p2"), "employee"));

        // Run migration
        OrganizationMigrationReport report = migrationService.executeMigration();
        assertTrue(report.isSuccess(), "Migration must succeed with collision handling");

        // Verify profiles for user1 and user2
        OrganizationMembership mem1 = membershipRepository.findByUserId(user1.getId()).orElseThrow();
        OrganizationMembership mem2 = membershipRepository.findByUserId(user2.getId()).orElseThrow();

        EmployeeProfile prof1 = employeeProfileRepository.findByMembershipId(mem1.getId()).orElseThrow();
        EmployeeProfile prof2 = employeeProfileRepository.findByMembershipId(mem2.getId()).orElseThrow();

        // Both emails must be non-null, valid, and NOT equal to each other
        assertNotNull(prof1.getWorkEmail());
        assertNotNull(prof2.getWorkEmail());
        assertNotEquals(prof1.getWorkEmail(), prof2.getWorkEmail(), "Colliding usernames must produce unique work emails");
        assertTrue(prof1.getWorkEmail().contains("@worknest.local"));
        assertTrue(prof2.getWorkEmail().contains("@worknest.local"));

        // Both employee IDs must be unique
        assertNotEquals(prof1.getEmployeeId(), prof2.getEmployeeId(), "Employee IDs must be unique");

        // Rerun migration and ensure determinism: emails must remain identical
        OrganizationMigrationReport rerun = migrationService.executeMigration();
        assertTrue(rerun.isSuccess());

        EmployeeProfile reloaded1 = employeeProfileRepository.findByMembershipId(mem1.getId()).orElseThrow();
        EmployeeProfile reloaded2 = employeeProfileRepository.findByMembershipId(mem2.getId()).orElseThrow();
        assertEquals(prof1.getWorkEmail(), reloaded1.getWorkEmail(), "Work emails must be deterministically preserved");
        assertEquals(prof2.getWorkEmail(), reloaded2.getWorkEmail(), "Work emails must be deterministically preserved");
    }

    @Test
    @DisplayName("D1.13: Transactional rollback on mid-migration database failure")
    void testMigrationTransactionalRollbackOnDatabaseFailure() {
        Organization org = organizationRepository.findBySlug("worknest-primary")
                .orElseGet(() -> organizationRepository.save(new Organization("WorkNest Primary Workspace", "worknest-primary", "ACTIVE", Instant.now())));

        long ts = System.currentTimeMillis();
        // Baseline existing user, leave, audit, notification
        User baselineUser = userRepository.save(new User("base_user_" + ts, passwordEncoder.encode("basepass"), "employee"));
        Leave baselineLeave = new Leave();
        baselineLeave.setEmployeeName("base_user_" + ts);
        baselineLeave.setStartDate("2026-12-01");
        baselineLeave.setEndDate("2026-12-05");
        baselineLeave.setDaysCount(5);
        baselineLeave.setStatus("Pending");
        baselineLeave.setReason("Holiday trip");
        Leave savedBaselineLeave = leaveRepository.save(baselineLeave);

        LeaveAuditEvent baselineAudit = auditEventRepository.save(new LeaveAuditEvent(
                savedBaselineLeave.getId(), "CREATED", "base_user_" + ts, "EMPLOYEE", Instant.now(),
                null, "Pending", 20, 20, "Created request"
        ));

        Notification baselineNotif = new Notification();
        baselineNotif.setMessage("Baseline notification " + ts);
        baselineNotif.setDate(LocalDateTime.now());
        baselineNotif.setRead(false);
        Notification savedBaselineNotif = notificationRepository.save(baselineNotif);

        // Pre-create membership & profile for baseline user so they are already migrated
        OrganizationMembership baseMem = membershipRepository.save(new OrganizationMembership(org, baselineUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        employeeProfileRepository.save(new EmployeeProfile(
                baseMem, org, "EMP-BASE-" + ts, "base_user_" + ts, "base_" + ts + "@worknest.local", null, null, null, "FULL_TIME"
        ));

        // Create 2 unmigrated legacy users
        User user1 = userRepository.save(new User("rollback_user1_" + ts, passwordEncoder.encode("p1"), "employee"));
        User user2 = userRepository.save(new User("fail_rollback_user2_" + ts, passwordEncoder.encode("p2"), "employee"));

        long memCountBefore = membershipRepository.count();
        long profCountBefore = employeeProfileRepository.count();

        // Add temporary H2 check constraint that fails when full_name contains 'fail_rollback'
        jdbcTemplate.execute("ALTER TABLE employee_profiles ADD CONSTRAINT check_fail_rollback CHECK (full_name NOT LIKE '%fail_rollback%')");

        try {
            // executeMigration should fail when inserting profile for user2
            assertThrows(Exception.class, () -> {
                migrationService.executeMigration();
            }, "Migration must throw exception due to database constraint violation");

            // Transactional rollback verification:
            // User 1's membership and profile must have been rolled back completely!
            assertFalse(membershipRepository.findByOrganizationIdAndUserId(org.getId(), user1.getId()).isPresent(),
                    "User 1 membership must be rolled back");
            assertFalse(membershipRepository.findByOrganizationIdAndUserId(org.getId(), user2.getId()).isPresent(),
                    "User 2 membership must be rolled back");

            // Database count of memberships and profiles must be identical to before the failed migration attempt
            assertEquals(memCountBefore, membershipRepository.count(), "Membership count must remain unchanged after rollback");
            assertEquals(profCountBefore, employeeProfileRepository.count(), "Profile count must remain unchanged after rollback");

            // Existing entities must be completely intact
            User reloadedUser = userRepository.findById(baselineUser.getId()).orElseThrow();
            assertEquals("base_user_" + ts, reloadedUser.getUsername());
            assertTrue(passwordEncoder.matches("basepass", reloadedUser.getPassword()));

            Leave reloadedLeave = leaveRepository.findById(savedBaselineLeave.getId()).orElseThrow();
            assertEquals("Pending", reloadedLeave.getStatus());
            assertEquals(5, reloadedLeave.getDaysCount());

            LeaveAuditEvent reloadedAudit = auditEventRepository.findById(baselineAudit.getId()).orElseThrow();
            assertEquals("CREATED", reloadedAudit.getEventType());

            Notification reloadedNotif = notificationRepository.findById(savedBaselineNotif.getId()).orElseThrow();
            assertEquals("Baseline notification " + ts, reloadedNotif.getMessage());
            assertFalse(reloadedNotif.isRead());
        } finally {
            // Always drop the temporary test constraint and cleanup unmigrated users
            jdbcTemplate.execute("ALTER TABLE employee_profiles DROP CONSTRAINT check_fail_rollback");
            userRepository.delete(user1);
            userRepository.delete(user2);
        }
    }

    @Test
    @DisplayName("D1.14: Explicit preservation of notifications, credentials, roles, and leave records")
    void testPreservationOfNotificationsAndAllLegacyData() {
        long ts = System.currentTimeMillis();
        String username = "legacy_notify_user_" + ts;
        String rawPassword = "StrongSecurePassword987!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(username, encodedPassword, "employee");
        user.setLeaveBalance(17);
        User savedUser = userRepository.save(user);

        Leave leave = new Leave();
        leave.setEmployeeName(username);
        leave.setStartDate("2026-11-10");
        leave.setEndDate("2026-11-13");
        leave.setDaysCount(4);
        leave.setStatus("Pending");
        leave.setReason("Personal development conference");
        Leave savedLeave = leaveRepository.save(leave);

        LeaveAuditEvent audit = auditEventRepository.save(new LeaveAuditEvent(
                savedLeave.getId(), "SUBMITTED", username, "EMPLOYEE", Instant.now(),
                null, "Pending", 17, 13, "Submitted request"
        ));

        Notification notifRead = new Notification();
        notifRead.setMessage("Welcome to WorkNest " + ts);
        notifRead.setDate(LocalDateTime.of(2026, 10, 1, 9, 30));
        notifRead.setRead(true);
        Notification savedNotifRead = notificationRepository.save(notifRead);

        Notification notifUnread = new Notification();
        notifUnread.setMessage("Upcoming leave reminder " + ts);
        notifUnread.setDate(LocalDateTime.of(2026, 10, 15, 14, 0));
        notifUnread.setRead(false);
        Notification savedNotifUnread = notificationRepository.save(notifUnread);

        // Execute migration
        OrganizationMigrationReport report = migrationService.executeMigration();
        assertTrue(report.isSuccess(), "Migration execution should succeed");

        // Verify notifications are 100% preserved
        Notification reloadedRead = notificationRepository.findById(savedNotifRead.getId()).orElseThrow();
        assertEquals(savedNotifRead.getId(), reloadedRead.getId());
        assertEquals("Welcome to WorkNest " + ts, reloadedRead.getMessage());
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 30), reloadedRead.getDate());
        assertTrue(reloadedRead.isRead());

        Notification reloadedUnread = notificationRepository.findById(savedNotifUnread.getId()).orElseThrow();
        assertEquals(savedNotifUnread.getId(), reloadedUnread.getId());
        assertEquals("Upcoming leave reminder " + ts, reloadedUnread.getMessage());
        assertEquals(LocalDateTime.of(2026, 10, 15, 14, 0), reloadedUnread.getDate());
        assertFalse(reloadedUnread.isRead());

        // Verify legacy user data
        User reloadedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals(savedUser.getId(), reloadedUser.getId());
        assertEquals(username, reloadedUser.getUsername());
        assertEquals(encodedPassword, reloadedUser.getPassword());
        assertTrue(passwordEncoder.matches(rawPassword, reloadedUser.getPassword()));
        assertEquals("employee", reloadedUser.getRole());
        assertEquals(17, reloadedUser.getLeaveBalance());

        // Verify leave request and audit event
        Leave reloadedLeave = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals(savedLeave.getId(), reloadedLeave.getId());
        assertEquals(username, reloadedLeave.getEmployeeName());
        assertEquals("Pending", reloadedLeave.getStatus());
        assertEquals(4, reloadedLeave.getDaysCount());
        assertEquals("Personal development conference", reloadedLeave.getReason());

        LeaveAuditEvent reloadedAudit = auditEventRepository.findById(audit.getId()).orElseThrow();
        assertEquals(audit.getId(), reloadedAudit.getId());
        assertEquals("SUBMITTED", reloadedAudit.getEventType());
        assertEquals(17, reloadedAudit.getPreviousBalance());
        assertEquals(13, reloadedAudit.getNewBalance());
    }
}
