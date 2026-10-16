package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.NotificationService;
import com.examly.springapp.service.OrganizationMigrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkNestPhaseD2HardeningAndMigrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private NotificationService notificationService;

    @Autowired
    private OrganizationMigrationService migrationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Organization orgAlpha;
    private Organization orgBeta;
    private Organization suspendedOrg;

    private User alphaAdmin;
    private User alphaEmployee;
    private User alphaManager;
    private User betaAdmin;
    private User betaEmployee;
    private User platformAdmin;
    private User inactiveMemberUser;
    private User multiOrgUser;

    private String alphaAdminToken;
    private String alphaEmployeeToken;
    private String alphaManagerToken;
    private String betaAdminToken;
    private String betaEmployeeToken;
    private String platformAdminToken;
    private String inactiveMemberToken;
    private String multiOrgToken;

    private Leave legacyUnassignedLeave;
    private Leave alphaLeave;
    private Leave alphaPeerLeave;
    private Leave betaLeave;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        // 1. Create Organizations
        orgAlpha = organizationRepository.save(new Organization("Alpha Corp " + ts, "alpha-" + ts, "ACTIVE", Instant.now()));
        orgBeta = organizationRepository.save(new Organization("Beta LLC " + ts, "beta-" + ts, "ACTIVE", Instant.now()));
        suspendedOrg = organizationRepository.save(new Organization("Suspended Co " + ts, "suspended-" + ts, "SUSPENDED", Instant.now()));

        // 2. Org Alpha Users
        alphaAdmin = userRepository.save(new User("alpha_admin_" + ts, passwordEncoder.encode("pass"), "employee"));
        alphaAdmin.setLeaveBalance(25);
        userRepository.save(alphaAdmin);
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaAdmin, "ORG_ADMIN", "ACTIVE", Instant.now()));
        alphaAdminToken = "Bearer " + jwtUtil.generateToken(alphaAdmin.getUsername(), "ORG_ADMIN");

        alphaEmployee = userRepository.save(new User("alpha_emp_" + ts, passwordEncoder.encode("pass"), "employee"));
        alphaEmployee.setLeaveBalance(20);
        userRepository.save(alphaEmployee);
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaEmployee, "EMPLOYEE", "ACTIVE", Instant.now()));
        alphaEmployeeToken = "Bearer " + jwtUtil.generateToken(alphaEmployee.getUsername(), "EMPLOYEE");

        alphaManager = userRepository.save(new User("alpha_mgr_" + ts, passwordEncoder.encode("pass"), "employee"));
        alphaManager.setLeaveBalance(22);
        userRepository.save(alphaManager);
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaManager, "MANAGER", "ACTIVE", Instant.now()));
        alphaManagerToken = "Bearer " + jwtUtil.generateToken(alphaManager.getUsername(), "MANAGER");

        // 3. Org Beta Users
        betaAdmin = userRepository.save(new User("beta_admin_" + ts, passwordEncoder.encode("pass"), "employee"));
        betaAdmin.setLeaveBalance(25);
        userRepository.save(betaAdmin);
        membershipRepository.save(new OrganizationMembership(orgBeta, betaAdmin, "ORG_ADMIN", "ACTIVE", Instant.now()));
        betaAdminToken = "Bearer " + jwtUtil.generateToken(betaAdmin.getUsername(), "ORG_ADMIN");

        betaEmployee = userRepository.save(new User("beta_emp_" + ts, passwordEncoder.encode("pass"), "employee"));
        betaEmployee.setLeaveBalance(18);
        userRepository.save(betaEmployee);
        membershipRepository.save(new OrganizationMembership(orgBeta, betaEmployee, "EMPLOYEE", "ACTIVE", Instant.now()));
        betaEmployeeToken = "Bearer " + jwtUtil.generateToken(betaEmployee.getUsername(), "EMPLOYEE");

        // 4. Platform Admin (no organization membership)
        platformAdmin = userRepository.save(new User("plat_admin_" + ts, passwordEncoder.encode("pass"), "PLATFORM_ADMIN"));
        platformAdminToken = "Bearer " + jwtUtil.generateToken(platformAdmin.getUsername(), "PLATFORM_ADMIN");

        // 5. Inactive membership user
        inactiveMemberUser = userRepository.save(new User("inactive_user_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, inactiveMemberUser, "EMPLOYEE", "INACTIVE", Instant.now()));
        inactiveMemberToken = "Bearer " + jwtUtil.generateToken(inactiveMemberUser.getUsername(), "EMPLOYEE");

        // 6. Ambiguous multi-org user
        multiOrgUser = userRepository.save(new User("multi_org_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, multiOrgUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(orgBeta, multiOrgUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        multiOrgToken = "Bearer " + jwtUtil.generateToken(multiOrgUser.getUsername(), "EMPLOYEE");

        // 7. Seed Leaves
        // Legacy unassigned leave (organization = null)
        Leave unassigned = new Leave();
        unassigned.setEmployeeName("legacy_orphan_" + ts);
        unassigned.setStartDate("2026-11-02");
        unassigned.setEndDate("2026-11-04");
        unassigned.setDaysCount(3);
        unassigned.setStatus("Pending");
        unassigned.setLeaveType("Annual Leave");
        unassigned.setReason("Legacy unassigned leave");
        unassigned.setOrganization(null);
        legacyUnassignedLeave = leaveRepository.save(unassigned);

        // Alpha Leave
        Leave lAlpha = new Leave();
        lAlpha.setEmployeeName(alphaEmployee.getUsername());
        lAlpha.setStartDate("2026-11-10");
        lAlpha.setEndDate("2026-11-12");
        lAlpha.setDaysCount(3);
        lAlpha.setStatus("Pending");
        lAlpha.setLeaveType("Sick Leave");
        lAlpha.setReason("Flu");
        lAlpha.setOrganization(orgAlpha);
        alphaLeave = leaveRepository.save(lAlpha);

        // Alpha Peer Leave (applicant is manager, so employee can approve if promoted to admin)
        Leave lAlphaPeer = new Leave();
        lAlphaPeer.setEmployeeName(alphaManager.getUsername());
        lAlphaPeer.setStartDate("2026-11-20");
        lAlphaPeer.setEndDate("2026-11-22");
        lAlphaPeer.setDaysCount(3);
        lAlphaPeer.setStatus("Pending");
        lAlphaPeer.setLeaveType("Casual Leave");
        lAlphaPeer.setReason("Doctor visit");
        lAlphaPeer.setOrganization(orgAlpha);
        alphaPeerLeave = leaveRepository.save(lAlphaPeer);

        // Beta Leave
        Leave lBeta = new Leave();
        lBeta.setEmployeeName(betaEmployee.getUsername());
        lBeta.setStartDate("2026-11-16");
        lBeta.setEndDate("2026-11-18");
        lBeta.setDaysCount(3);
        lBeta.setStatus("Pending");
        lBeta.setLeaveType("Casual Leave");
        lBeta.setReason("Personal");
        lBeta.setOrganization(orgBeta);
        betaLeave = leaveRepository.save(lBeta);
    }

    // ==========================================
    // TASK 5.A: Null-Organization Leave Tests
    // ==========================================

    @Test
    @DisplayName("5A.1: Tenant admin cannot read unassigned legacy leave by ID -> 404")
    void testTenantAdminCannotReadUnassignedLeave() throws Exception {
        mockMvc.perform(get("/leaves/" + legacyUnassignedLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5A.2: Tenant admin cannot update unassigned legacy leave -> 404")
    void testTenantAdminCannotUpdateUnassignedLeave() throws Exception {
        Leave updateReq = new Leave();
        updateReq.setReason("Malicious update");

        mockMvc.perform(put("/leaves/" + legacyUnassignedLeave.getId())
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        Leave fresh = leaveRepository.findById(legacyUnassignedLeave.getId()).orElseThrow();
        assertEquals("Legacy unassigned leave", fresh.getReason());
    }

    @Test
    @DisplayName("5A.3: Tenant admin cannot delete unassigned legacy leave -> 404")
    void testTenantAdminCannotDeleteUnassignedLeave() throws Exception {
        mockMvc.perform(delete("/leaves/" + legacyUnassignedLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        assertTrue(leaveRepository.existsById(legacyUnassignedLeave.getId()));
    }

    @Test
    @DisplayName("5A.4: Tenant admin cannot approve unassigned legacy leave -> 404")
    void testTenantAdminCannotApproveUnassignedLeave() throws Exception {
        long preAuditCount = auditEventRepository.count();
        long preNotifCount = notificationRepository.count();

        mockMvc.perform(put("/leaves/" + legacyUnassignedLeave.getId() + "/approve")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        Leave fresh = leaveRepository.findById(legacyUnassignedLeave.getId()).orElseThrow();
        assertEquals("Pending", fresh.getStatus());
        assertEquals(preAuditCount, auditEventRepository.count());
        assertEquals(preNotifCount, notificationRepository.count());
    }

    @Test
    @DisplayName("5A.5: Tenant admin cannot reject unassigned legacy leave -> 404")
    void testTenantAdminCannotRejectUnassignedLeave() throws Exception {
        mockMvc.perform(put("/leaves/" + legacyUnassignedLeave.getId() + "/reject")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rejectionReason\":\"Rejected\"}"))
                .andExpect(status().isNotFound());

        Leave fresh = leaveRepository.findById(legacyUnassignedLeave.getId()).orElseThrow();
        assertEquals("Pending", fresh.getStatus());
    }

    @Test
    @DisplayName("5A.6: Tenant user cannot cancel unassigned legacy leave -> 404")
    void testTenantUserCannotCancelUnassignedLeave() throws Exception {
        mockMvc.perform(put("/leaves/" + legacyUnassignedLeave.getId() + "/cancel")
                .header("Authorization", alphaEmployeeToken))
                .andExpect(status().isNotFound());

        Leave fresh = leaveRepository.findById(legacyUnassignedLeave.getId()).orElseThrow();
        assertEquals("Pending", fresh.getStatus());
    }

    @Test
    @DisplayName("5A.7: Tenant admin cannot retrieve audit history for unassigned legacy leave -> 404")
    void testTenantAdminCannotRetrieveAuditForUnassignedLeave() throws Exception {
        mockMvc.perform(get("/leaves/" + legacyUnassignedLeave.getId() + "/audit")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5A.8: AI impact analysis and OOO template fail-closed on unassigned legacy leave -> 404")
    void testAiEndpointsRejectUnassignedLeave() throws Exception {
        mockMvc.perform(get("/ai/impact/" + legacyUnassignedLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/ai/ooo/" + legacyUnassignedLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // TASK 5.B: Cross-Tenant Isolation Tests
    // ==========================================

    @Test
    @DisplayName("5B.1: Org Alpha users cannot access Org Beta leave records -> 404")
    void testCrossTenantLeaveReadDenied() throws Exception {
        mockMvc.perform(get("/leaves/" + betaLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/leaves/" + betaLeave.getId())
                .header("Authorization", alphaEmployeeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5B.2: Org Alpha admin cannot approve, reject, or cancel Org Beta leave -> 404")
    void testCrossTenantLeaveMutationsDenied() throws Exception {
        mockMvc.perform(put("/leaves/" + betaLeave.getId() + "/approve")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/leaves/" + betaLeave.getId() + "/reject")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rejectionReason\":\"Unauthorized\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/leaves/" + betaLeave.getId() + "/cancel")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        Leave betaFresh = leaveRepository.findById(betaLeave.getId()).orElseThrow();
        assertEquals("Pending", betaFresh.getStatus());
    }

    @Test
    @DisplayName("5B.3: Org Alpha admin cannot view Org Beta leave audit trail -> 404")
    void testCrossTenantAuditTrailDenied() throws Exception {
        mockMvc.perform(get("/leaves/" + betaLeave.getId() + "/audit")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5B.4: Org Alpha users cannot analyze Org Beta leave via AI -> 404")
    void testCrossTenantAiAnalysisDenied() throws Exception {
        mockMvc.perform(get("/ai/impact/" + betaLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/ai/ooo/" + betaLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5B.5: Calendar isolates leaves strictly to the caller's organization")
    void testCalendarTenantIsolation() throws Exception {
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(alphaEmployee.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(betaEmployee.getUsername()))))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem("legacy_orphan"))));

        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", betaAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(betaEmployee.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(alphaEmployee.getUsername()))));
    }

    @Test
    @DisplayName("5B.6: Notifications isolate data strictly to the caller's organization")
    void testNotificationsTenantIsolation() throws Exception {
        notificationService.createNotification(orgAlpha.getId(), "Alpha confidential notification");
        notificationService.createNotification(orgBeta.getId(), "Beta confidential notification");

        mockMvc.perform(get("/notifications")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].message", hasItem("Alpha confidential notification")))
                .andExpect(jsonPath("$[*].message", not(hasItem("Beta confidential notification"))));

        mockMvc.perform(get("/notifications")
                .header("Authorization", betaAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].message", hasItem("Beta confidential notification")))
                .andExpect(jsonPath("$[*].message", not(hasItem("Alpha confidential notification"))));
    }

    // ==========================================
    // TASK 5.C: Missing-Context & Platform Admin Isolation Tests
    // ==========================================

    @Test
    @DisplayName("5C.1: Platform administrator without tenant context receives empty leave list")
    void testPlatformAdminGetsEmptyLeaveList() throws Exception {
        mockMvc.perform(get("/leaves")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/leaves/page?page=0&size=10")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("5C.2: Platform administrator without tenant context receives empty calendar and notifications")
    void testPlatformAdminGetsEmptyCalendarAndNotifications() throws Exception {
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/notifications")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("5C.3: Platform administrator cannot approve leave or view audit trail without tenant context -> 403/404")
    void testPlatformAdminCannotPerformTenantHrOperations() throws Exception {
        mockMvc.perform(put("/leaves/" + alphaLeave.getId() + "/approve")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/leaves/" + alphaLeave.getId() + "/audit")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/ai/impact/" + alphaLeave.getId())
                .header("Authorization", platformAdminToken))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // TASK 5.D: Role Consistency Tests
    // ==========================================

    @Test
    @DisplayName("5D.1: Active Org Admin can approve leave in their organization")
    void testOrgAdminCanApproveLeave() throws Exception {
        mockMvc.perform(put("/leaves/" + alphaLeave.getId() + "/approve")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Approved")));

        Leave fresh = leaveRepository.findById(alphaLeave.getId()).orElseThrow();
        assertEquals("Approved", fresh.getStatus());
        User freshEmp = userRepository.findByUsername(alphaEmployee.getUsername());
        assertEquals(17, freshEmp.getLeaveBalance()); // 20 - 3
    }

    @Test
    @DisplayName("5D.2: Regular employee cannot approve leave -> 403 Forbidden")
    void testEmployeeCannotApproveLeave() throws Exception {
        mockMvc.perform(put("/leaves/" + alphaLeave.getId() + "/approve")
                .header("Authorization", alphaEmployeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5D.3: Manager can approve leave in their organization")
    void testManagerCanApproveLeave() throws Exception {
        mockMvc.perform(put("/leaves/" + alphaLeave.getId() + "/approve")
                .header("Authorization", alphaManagerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Approved")));
    }

    @Test
    @DisplayName("5D.4: Org Admin CANNOT access platform migration endpoints -> 403 Forbidden")
    void testOrgAdminBlockedFromMigrationEndpoints() throws Exception {
        mockMvc.perform(get("/admin/migration/preview")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5D.5: Platform Admin CAN access platform migration preview")
    void testPlatformAdminCanAccessMigrationPreview() throws Exception {
        mockMvc.perform(get("/admin/migration/preview")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun", is(true)));
    }

    @Test
    @DisplayName("5D.6: Inactive membership denies tenant access -> 403 Forbidden")
    void testInactiveMembershipDenied() throws Exception {
        mockMvc.perform(get("/leaves")
                .header("Authorization", inactiveMemberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5D.7: Ambiguous multi-organization membership denies access -> 403 Forbidden")
    void testAmbiguousMembershipDenied() throws Exception {
        mockMvc.perform(get("/leaves")
                .header("Authorization", multiOrgToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5D.8: Dynamic role change in DB is reflected immediately in security context")
    void testDynamicRoleChangeReflectedImmediately() throws Exception {
        // Initially alphaEmployee cannot approve peer's leave (403 forbidden)
        mockMvc.perform(put("/leaves/" + alphaPeerLeave.getId() + "/approve")
                .header("Authorization", alphaEmployeeToken))
                .andExpect(status().isForbidden());

        // Update membership role to ORG_ADMIN in DB
        OrganizationMembership mem = membershipRepository.findByUserId(alphaEmployee.getId()).orElseThrow();
        mem.setRole("ORG_ADMIN");
        membershipRepository.saveAndFlush(mem);

        // Next request with existing token should now succeed because authorities are derived from live DB!
        mockMvc.perform(put("/leaves/" + alphaPeerLeave.getId() + "/approve")
                .header("Authorization", alphaEmployeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Approved")));
    }

    // ==========================================
    // TASK 5.E: Migration Service Tests
    // ==========================================

    @Test
    @DisplayName("5E.1: Migration preview performs zero database writes")
    void testMigrationPreviewPerformsZeroWrites() {
        long orgCountBefore = organizationRepository.count();
        long memCountBefore = membershipRepository.count();
        long profCountBefore = employeeProfileRepository.count();
        long leaveCountBefore = leaveRepository.count();
        long notifCountBefore = notificationRepository.count();
        long auditCountBefore = auditEventRepository.count();

        OrganizationMigrationReport report = migrationService.previewMigration();

        assertNotNull(report);
        assertTrue(report.isDryRun());
        assertTrue(report.getTotalLeavesFound() > 0);
        assertTrue(report.getTotalNotificationsFound() >= 0);

        assertEquals(orgCountBefore, organizationRepository.count());
        assertEquals(memCountBefore, membershipRepository.count());
        assertEquals(profCountBefore, employeeProfileRepository.count());
        assertEquals(leaveCountBefore, leaveRepository.count());
        assertEquals(notifCountBefore, notificationRepository.count());
        assertEquals(auditCountBefore, auditEventRepository.count());
    }

    @Test
    @DisplayName("5E.2: Ambiguous records are reported as unresolved in migration preview")
    void testMigrationReportsAmbiguousRecords() {
        // Seed an unassigned leave with null employee name
        Leave orphanLeave = new Leave();
        orphanLeave.setStartDate("2026-12-01");
        orphanLeave.setEndDate("2026-12-03");
        orphanLeave.setDaysCount(3);
        orphanLeave.setStatus("Pending");
        orphanLeave.setEmployeeName(null); // Ambiguous!
        leaveRepository.save(orphanLeave);

        // Seed an orphan audit event pointing to non-existent leave
        LeaveAuditEvent orphanAudit = new LeaveAuditEvent();
        orphanAudit.setLeaveId(999999L); // Non-existent!
        orphanAudit.setEventType("CREATED");
        orphanAudit.setActorUsername("orphan");
        orphanAudit.setEventTimestamp(Instant.now());
        orphanAudit.setToStatus("Pending");
        auditEventRepository.save(orphanAudit);

        // Seed an orphan notification with null recipient and no leave reference
        Notification orphanNotif = new Notification();
        orphanNotif.setMessage("Orphan system broadcast");
        orphanNotif.setRecipientUsername(null);
        notificationRepository.save(orphanNotif);

        OrganizationMigrationReport preview = migrationService.previewMigration();

        assertTrue(preview.getUnresolvedLeaves() >= 1, "Orphan leave must be flagged unresolved");
        assertTrue(preview.getUnresolvedAuditEvents() >= 1, "Orphan audit event must be flagged unresolved");
        assertTrue(preview.getUnresolvedNotifications() >= 1, "Orphan notification must be flagged unresolved");
    }

    @Test
    @DisplayName("5E.3: Migration execution safely associates historical leaves and is idempotent")
    void testMigrationExecutionAndIdempotency() {
        // Seed clean unassigned legacy user and leave
        User legacyUser = userRepository.save(new User("legacy_valid_emp", passwordEncoder.encode("pass"), "employee"));
        Leave legacyLeave = new Leave();
        legacyLeave.setEmployeeName(legacyUser.getUsername());
        legacyLeave.setStartDate("2026-12-10");
        legacyLeave.setEndDate("2026-12-12");
        legacyLeave.setDaysCount(3);
        legacyLeave.setStatus("Pending");
        legacyLeave.setOrganization(null);
        legacyLeave = leaveRepository.save(legacyLeave);

        // Run execution
        OrganizationMigrationReport report1 = migrationService.executeMigration();
        assertTrue(report1.isSuccess());
        assertTrue(report1.getLeavesMigrated() >= 1);

        Leave migratedLeave = leaveRepository.findById(legacyLeave.getId()).orElseThrow();
        assertNotNull(migratedLeave.getOrganization(), "Leave should now belong to default org");
        Organization assignedOrg = organizationRepository.findById(migratedLeave.getOrganization().getId()).orElseThrow();
        assertEquals("worknest-primary", assignedOrg.getSlug());

        // Repeated run should be idempotent
        OrganizationMigrationReport report2 = migrationService.executeMigration();
        assertTrue(report2.isSuccess());
        assertEquals(0, report2.getMembershipsCreated(), "Idempotent: 0 new memberships");
        assertEquals(0, report2.getLeavesMigrated(), "Idempotent: 0 leaves migrated");
    }
}
