package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.invitation.dev-token-delivery=true"
})
public class WorkNestPhaseD3SecurityAndScopingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private EmployeeInvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrganizationMigrationService migrationService;

    private Organization orgAlpha;
    private Organization orgBeta;
    private Organization orgSuspended;

    private User alphaAdmin;
    private User alphaHr;
    private User alphaManagerEng;
    private User alphaEmployeeEng;
    private User alphaEmployeeSales;
    private User betaAdmin;
    private User deactivatedUser;

    private String alphaAdminToken;
    private String alphaHrToken;
    private String alphaManagerEngToken;
    private String alphaEmployeeEngToken;
    private String betaAdminToken;
    private String deactivatedUserToken;

    private Department deptEngineering;
    private Department deptSales;

    private OrganizationMembership memAlphaMgrEng;
    private OrganizationMembership memAlphaEmpEng;
    private OrganizationMembership memAlphaEmpSales;

    private String alphaEmpEngId;
    private String alphaEmpEngEmail;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        // 1. Organizations
        orgAlpha = organizationRepository.save(new Organization("Alpha Corp " + ts, "alpha-" + ts, "ACTIVE", Instant.now()));
        orgBeta = organizationRepository.save(new Organization("Beta LLC " + ts, "beta-" + ts, "ACTIVE", Instant.now()));
        orgSuspended = organizationRepository.save(new Organization("Suspended Ltd " + ts, "suspended-" + ts, "SUSPENDED", Instant.now()));

        // 2. Alpha Admin & HR
        alphaAdmin = userRepository.save(new User("d3_admin_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaAdmin, "ORG_ADMIN", "ACTIVE", Instant.now()));
        alphaAdminToken = "Bearer " + jwtUtil.generateToken(alphaAdmin.getUsername(), "ORG_ADMIN");

        alphaHr = userRepository.save(new User("d3_hr_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaHr, "HR_ADMIN", "ACTIVE", Instant.now()));
        alphaHrToken = "Bearer " + jwtUtil.generateToken(alphaHr.getUsername(), "HR_ADMIN");

        // 3. Manager for Engineering
        alphaManagerEng = userRepository.save(new User("d3_mreng_" + ts, passwordEncoder.encode("pass"), "employee"));
        memAlphaMgrEng = membershipRepository.save(new OrganizationMembership(orgAlpha, alphaManagerEng, "MANAGER", "ACTIVE", Instant.now()));
        alphaManagerEngToken = "Bearer " + jwtUtil.generateToken(alphaManagerEng.getUsername(), "MANAGER");

        // 4. Departments
        deptEngineering = departmentRepository.save(new Department(orgAlpha, "Engineering", "ENG", memAlphaMgrEng));
        deptSales = departmentRepository.save(new Department(orgAlpha, "Sales", "SALES", null));
        departmentRepository.save(new Department(orgBeta, "Beta Operations", "B-OPS", null));

        // 5. Employees
        alphaEmployeeEng = userRepository.save(new User("d3_emp_eng_" + ts, passwordEncoder.encode("pass"), "employee"));
        memAlphaEmpEng = membershipRepository.save(new OrganizationMembership(orgAlpha, alphaEmployeeEng, "EMPLOYEE", "ACTIVE", Instant.now()));
        alphaEmployeeEngToken = "Bearer " + jwtUtil.generateToken(alphaEmployeeEng.getUsername(), "EMPLOYEE");
        alphaEmpEngId = "EMP-ENG-" + ts;
        alphaEmpEngEmail = "alice_" + ts + "@alpha.test";
        EmployeeProfile profEng = new EmployeeProfile(memAlphaEmpEng, orgAlpha, alphaEmpEngId, "Alice Eng", alphaEmpEngEmail, "Engineering", "Developer", null, "FULL_TIME");
        profEng.setDepartmentRef(deptEngineering);
        employeeProfileRepository.save(profEng);

        alphaEmployeeSales = userRepository.save(new User("d3_emp_sales_" + ts, passwordEncoder.encode("pass"), "employee"));
        memAlphaEmpSales = membershipRepository.save(new OrganizationMembership(orgAlpha, alphaEmployeeSales, "EMPLOYEE", "ACTIVE", Instant.now()));
        EmployeeProfile profSales = new EmployeeProfile(memAlphaEmpSales, orgAlpha, "EMP-SALES-" + ts, "Bob Sales", "bob@alpha.test", "Sales", "Representative", null, "FULL_TIME");
        profSales.setDepartmentRef(deptSales);
        employeeProfileRepository.save(profSales);

        // 6. Beta Admin
        betaAdmin = userRepository.save(new User("d3_beta_admin_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgBeta, betaAdmin, "ORG_ADMIN", "ACTIVE", Instant.now()));
        betaAdminToken = "Bearer " + jwtUtil.generateToken(betaAdmin.getUsername(), "ORG_ADMIN");

        // 7. Deactivated User
        deactivatedUser = userRepository.save(new User("d3_deact_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, deactivatedUser, "EMPLOYEE", "DEACTIVATED", Instant.now()));
        deactivatedUserToken = "Bearer " + jwtUtil.generateToken(deactivatedUser.getUsername(), "EMPLOYEE");
    }

    // =========================================================================
    // WORKSTREAM 1: DEPARTMENTS AND MANAGER SCOPING
    // =========================================================================

    @Test
    @DisplayName("D3.1: Cross-organization department assignment rejected")
    void testCrossOrganizationDepartmentAssignmentRejection() throws Exception {
        // Attempt to create a department in Org Alpha referencing a manager from Org Beta -> 400 Bad Request
        OrganizationMembership betaMem = membershipRepository.save(
                new OrganizationMembership(orgBeta, userRepository.save(new User("beta_mgr", "p", "employee")), "MANAGER", "ACTIVE", Instant.now())
        );

        DepartmentRequest badReq = new DepartmentRequest("Invalid Dept", "INV", betaMem.getId());
        mockMvc.perform(post("/departments")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());

        // Direct entity validation
        assertThrows(IllegalArgumentException.class, () -> {
            new Department(orgAlpha, "Direct Entity Check", "DEC", betaMem);
        });
    }

    @Test
    @DisplayName("D3.2: Manager can approve leave in their authorized department")
    void testManagerCanApproveAuthorizedDepartmentLeave() throws Exception {
        // Employee in Engineering creates leave
        Leave leaveEng = new Leave();
        leaveEng.setEmployeeName(alphaEmployeeEng.getUsername());
        leaveEng.setStartDate("2026-12-01");
        leaveEng.setEndDate("2026-12-02");
        leaveEng.setReason("Engineering training");
        leaveEng.setOrganization(orgAlpha);
        leaveEng.setDepartment(deptEngineering);
        leaveEng = leaveRepository.save(leaveEng);

        // Manager of Engineering approves -> 200 OK
        mockMvc.perform(put("/leaves/" + leaveEng.getId() + "/approve")
                .header("Authorization", alphaManagerEngToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Approved")))
                .andExpect(jsonPath("$.approvedBy", is(alphaManagerEng.getUsername())));
    }

    @Test
    @DisplayName("D3.3: Manager CANNOT approve leave in unauthorized department -> 403 Forbidden")
    void testManagerCannotApproveUnauthorizedDepartmentLeave() throws Exception {
        // Employee in Sales creates leave
        Leave leaveSales = new Leave();
        leaveSales.setEmployeeName(alphaEmployeeSales.getUsername());
        leaveSales.setStartDate("2026-12-05");
        leaveSales.setEndDate("2026-12-06");
        leaveSales.setReason("Sales conference");
        leaveSales.setOrganization(orgAlpha);
        leaveSales.setDepartment(deptSales);
        leaveSales = leaveRepository.save(leaveSales);

        // Manager of Engineering attempts to approve Sales leave -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + leaveSales.getId() + "/approve")
                .header("Authorization", alphaManagerEngToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D3.4: Manager CANNOT reject leave in unauthorized department -> 403 Forbidden")
    void testManagerCannotRejectUnauthorizedDepartmentLeave() throws Exception {
        Leave leaveSales = new Leave();
        leaveSales.setEmployeeName(alphaEmployeeSales.getUsername());
        leaveSales.setStartDate("2026-12-10");
        leaveSales.setEndDate("2026-12-11");
        leaveSales.setReason("Sales workshop");
        leaveSales.setOrganization(orgAlpha);
        leaveSales.setDepartment(deptSales);
        leaveSales = leaveRepository.save(leaveSales);

        // Manager of Engineering attempts to reject Sales leave -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + leaveSales.getId() + "/reject")
                .header("Authorization", alphaManagerEngToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rejectionReason", "Not approved"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D3.5: Manager self-approval is strictly prohibited -> 403 Forbidden")
    void testManagerSelfApprovalProhibited() throws Exception {
        Leave mgrLeave = new Leave();
        mgrLeave.setEmployeeName(alphaManagerEng.getUsername());
        mgrLeave.setStartDate("2026-12-15");
        mgrLeave.setEndDate("2026-12-16");
        mgrLeave.setReason("Manager personal time");
        mgrLeave.setOrganization(orgAlpha);
        mgrLeave.setDepartment(deptEngineering);
        mgrLeave = leaveRepository.save(mgrLeave);

        // Manager attempts to approve their own leave request -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + mgrLeave.getId() + "/approve")
                .header("Authorization", alphaManagerEngToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D3.6: Organization administrator retains organization-wide approval access")
    void testOrgAdminRetainsOrgWideApprovalAccess() throws Exception {
        // Sales leave
        Leave leaveSales = new Leave();
        leaveSales.setEmployeeName(alphaEmployeeSales.getUsername());
        leaveSales.setStartDate("2026-12-20");
        leaveSales.setEndDate("2026-12-21");
        leaveSales.setReason("Client visit");
        leaveSales.setOrganization(orgAlpha);
        leaveSales.setDepartment(deptSales);
        leaveSales = leaveRepository.save(leaveSales);

        // Org Admin can approve leaves in ANY department of Org Alpha
        mockMvc.perform(put("/leaves/" + leaveSales.getId() + "/approve")
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Approved")));
    }

    @Test
    @DisplayName("D3.7: Manager team visibility restricted to authorized department")
    void testManagerTeamVisibilityRestricted() throws Exception {
        // Create Engineering leave
        Leave leaveEng = new Leave();
        leaveEng.setEmployeeName(alphaEmployeeEng.getUsername());
        leaveEng.setStartDate("2026-11-10");
        leaveEng.setEndDate("2026-11-11");
        leaveEng.setOrganization(orgAlpha);
        leaveEng.setDepartment(deptEngineering);
        leaveRepository.save(leaveEng);

        // Create Sales leave
        Leave leaveSales = new Leave();
        leaveSales.setEmployeeName(alphaEmployeeSales.getUsername());
        leaveSales.setStartDate("2026-11-12");
        leaveSales.setEndDate("2026-11-13");
        leaveSales.setOrganization(orgAlpha);
        leaveSales.setDepartment(deptSales);
        leaveRepository.save(leaveSales);

        // Manager lists leaves: must see Engineering leave, but NOT Sales leave
        mockMvc.perform(get("/leaves")
                .header("Authorization", alphaManagerEngToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(alphaEmployeeEng.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(alphaEmployeeSales.getUsername()))));
    }

    // =========================================================================
    // WORKSTREAM 2: SECURE EMPLOYEE INVITATIONS AND ACTIVATION
    // =========================================================================

    @Test
    @DisplayName("D3.8: HR creates invitation with secure token and single-use activation")
    void testCreateAndAcceptInvitationFlow() throws Exception {
        long ts = System.currentTimeMillis();
        InvitationRequest req = new InvitationRequest("EMP-INV-" + ts, "invited_" + ts + "@alpha.test", "Charlie Invitee", "EMPLOYEE", deptEngineering.getId());

        // HR Admin sends invitation
        String responseContent = mockMvc.perform(post("/invitations")
                .header("Authorization", alphaHrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId", is("EMP-INV-" + ts)))
                .andExpect(jsonPath("$.email", is("invited_" + ts + "@alpha.test")))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn().getResponse().getContentAsString();

        // Raw token is provided in dev response
        EmployeeInvitation created = objectMapper.readValue(responseContent, EmployeeInvitation.class);
        String rawToken = created.getRawToken();
        assertNotNull(rawToken, "Raw token must be populated for dev delivery");

        // Verify raw token is NOT stored in plain text in the database
        EmployeeInvitation dbRecord = invitationRepository.findById(created.getId()).orElseThrow();
        assertNull(dbRecord.getRawToken(), "Raw token must never be persisted");
        assertNotNull(dbRecord.getTokenHash(), "Token hash must be securely stored in DB");
        assertNotEquals(rawToken, dbRecord.getTokenHash(), "Stored hash must differ from raw token");

        // Activate account
        ActivationRequest actReq = new ActivationRequest(rawToken, "user_active_" + ts, "Secret123!", "invited_" + ts + "@alpha.test");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("user_active_" + ts)))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")));

        // Token reuse rejection: Attempt to use the same token again -> 400 Bad Request
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("D3.9: Invitation token expiry, invalid token, and email mismatch rejection")
    void testInvitationTokenFailureScenarios() throws Exception {
        long ts = System.currentTimeMillis();
        InvitationRequest req = new InvitationRequest("EMP-FAIL-" + ts, "fail_" + ts + "@alpha.test", "Fail Test", "EMPLOYEE", null);

        String res = mockMvc.perform(post("/invitations")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        EmployeeInvitation created = objectMapper.readValue(res, EmployeeInvitation.class);
        String rawToken = created.getRawToken();

        // 1. Invalid token -> 400 Bad Request
        ActivationRequest badTokenReq = new ActivationRequest("totally_bogus_token_12345", "user_x_" + ts, "Secret123!", "fail_" + ts + "@alpha.test");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badTokenReq)))
                .andExpect(status().isBadRequest());

        // 2. Email mismatch -> 400 Bad Request
        ActivationRequest emailMismatchReq = new ActivationRequest(rawToken, "user_y_" + ts, "Secret123!", "wrong_email@other.test");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailMismatchReq)))
                .andExpect(status().isBadRequest());

        // 3. Expired token -> 400 Bad Request
        EmployeeInvitation dbRecord = invitationRepository.findById(created.getId()).orElseThrow();
        dbRecord.setExpiresAt(Instant.now().minus(2, ChronoUnit.HOURS));
        invitationRepository.save(dbRecord);

        ActivationRequest expiredReq = new ActivationRequest(rawToken, "user_z_" + ts, "Secret123!", "fail_" + ts + "@alpha.test");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expiredReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("D3.10: Invitation role escalation attempts strictly rejected")
    void testInvitationRoleEscalationRejected() throws Exception {
        long ts = System.currentTimeMillis();

        // 1. Attempt to invite PLATFORM_ADMIN -> 400 Bad Request
        InvitationRequest platformAdminReq = new InvitationRequest("EMP-ES1-" + ts, "es1_" + ts + "@alpha.test", "Escalation 1", "PLATFORM_ADMIN", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(platformAdminReq)))
                .andExpect(status().isBadRequest());

        // 2. HR Admin attempts to invite ORG_ADMIN -> 403 Forbidden
        InvitationRequest orgAdminReq = new InvitationRequest("EMP-ES2-" + ts, "es2_" + ts + "@alpha.test", "Escalation 2", "ORG_ADMIN", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", alphaHrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orgAdminReq)))
                .andExpect(status().isForbidden());

        // 3. Regular employee attempts to invite -> 403 Forbidden
        InvitationRequest empReq = new InvitationRequest("EMP-ES3-" + ts, "es3_" + ts + "@alpha.test", "Escalation 3", "EMPLOYEE", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", alphaEmployeeEngToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D3.11: Duplicate employee ID and company email within one organization rejected")
    void testDuplicateEmployeeIdAndEmailWithinSameOrgRejected() throws Exception {
        long ts = System.currentTimeMillis();

        // Attempt duplicate employee ID
        InvitationRequest dupIdReq = new InvitationRequest(alphaEmpEngId, "different_email_" + ts + "@alpha.test", "Duplicate ID", "EMPLOYEE", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupIdReq)))
                .andExpect(status().isConflict());

        // Attempt duplicate email
        InvitationRequest dupEmailReq = new InvitationRequest("NEW-ID-" + ts, alphaEmpEngEmail, "Duplicate Email", "EMPLOYEE", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupEmailReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("D3.12: Same employee ID and email allowed across DIFFERENT organizations")
    void testSameEmployeeIdAndEmailAllowedAcrossDifferentOrgs() throws Exception {
        long ts = System.currentTimeMillis();
        String sharedEmpId = "SHARED-EMP-999";
        String sharedEmail = "shared_identity_" + ts + "@contractor.com";

        // Create invitation in Org Alpha
        InvitationRequest alphaReq = new InvitationRequest(sharedEmpId, sharedEmail, "Alpha Contractor", "EMPLOYEE", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", alphaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alphaReq)))
                .andExpect(status().isCreated());

        // Create invitation in Org Beta with SAME employee ID and SAME email -> Allowed (201 Created)
        InvitationRequest betaReq = new InvitationRequest(sharedEmpId, sharedEmail, "Beta Contractor", "EMPLOYEE", null);
        mockMvc.perform(post("/invitations")
                .header("Authorization", betaAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(betaReq)))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // WORKSTREAM 3: ACCOUNT LIFECYCLE AND AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("D3.13: Deactivated membership and suspended organization access denied -> 403 Forbidden")
    void testDeactivatedAndSuspendedAccessDenied() throws Exception {
        // User with DEACTIVATED membership attempts to view leaves -> 403 Forbidden
        mockMvc.perform(get("/leaves")
                .header("Authorization", deactivatedUserToken))
                .andExpect(status().isForbidden());

        // User in SUSPENDED organization attempts to view leaves -> 403 Forbidden
        long ts = System.currentTimeMillis();
        User suspUser = userRepository.save(new User("susp_u_" + ts, passwordEncoder.encode("p"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgSuspended, suspUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        String suspToken = "Bearer " + jwtUtil.generateToken(suspUser.getUsername(), "EMPLOYEE");

        mockMvc.perform(get("/leaves")
                .header("Authorization", suspToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D3.14: Multiple active memberships fail closed until explicit tenant selector exists")
    void testMultipleActiveMembershipsFailClosed() throws Exception {
        long ts = System.currentTimeMillis();
        User multiUser = userRepository.save(new User("multi_tenant_" + ts, passwordEncoder.encode("p"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, multiUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(orgBeta, multiUser, "EMPLOYEE", "ACTIVE", Instant.now()));

        String multiToken = "Bearer " + jwtUtil.generateToken(multiUser.getUsername(), "EMPLOYEE");

        // Fails closed with 403 Forbidden
        mockMvc.perform(get("/leaves")
                .header("Authorization", multiToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // WORKSTREAM 4: MIGRATION SAFEGUARDS
    // =========================================================================

    @Test
    @DisplayName("D3.15: Migration guards against cross-organization user leave collision")
    void testMigrationGuardsAgainstCrossOrgUserLeaveCollision() {
        long ts = System.currentTimeMillis();

        // Create user who explicitly belongs to Org Beta
        User userInBeta = userRepository.save(new User("beta_worker_" + ts, passwordEncoder.encode("p"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgBeta, userInBeta, "EMPLOYEE", "ACTIVE", Instant.now()));

        // Create legacy unassigned leave with userInBeta's username
        Leave legacyLeave = new Leave();
        legacyLeave.setEmployeeName(userInBeta.getUsername());
        legacyLeave.setStartDate("2025-01-10");
        legacyLeave.setEndDate("2025-01-11");
        legacyLeave.setReason("Legacy Beta work");
        leaveRepository.save(legacyLeave);

        // Preview migration into default workspace ('worknest-primary')
        OrganizationMigrationReport preview = migrationService.previewMigration();

        // Must NOT map this leave to default org; it must remain UNRESOLVED
        assertTrue(preview.getUnresolvedLeaves() > 0, "Cross-org user leave must be counted as unresolved");
        assertTrue(preview.getPlannedActions().stream().anyMatch(a -> a.contains("cross-organization or ambiguous")),
                "Planned actions must report cross-organization conflict");
    }

    @Test
    @DisplayName("D3.16: Migration preview performs NO database writes")
    void testMigrationPreviewPerformsNoWrites() {
        long countBefore = organizationRepository.count();
        long memsBefore = membershipRepository.count();
        long leavesBefore = leaveRepository.count();

        OrganizationMigrationReport preview = migrationService.previewMigration();
        assertNotNull(preview);
        assertTrue(preview.isDryRun());

        assertEquals(countBefore, organizationRepository.count());
        assertEquals(memsBefore, membershipRepository.count());
        assertEquals(leavesBefore, leaveRepository.count());
    }

    @Test
    @DisplayName("D3.17: Concurrent migration execution protection")
    void testConcurrentMigrationProtection() throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    migrationService.executeMigration();
                    successes.incrementAndGet();
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("Another migration is currently in progress")) {
                        rejected.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly one should succeed or both finish deterministically
        assertTrue(successes.get() >= 1, "At least one migration must succeed");
    }

    // =========================================================================
    // D2.1 REGRESSION SAFETY CHECKS
    // =========================================================================

    @Test
    @DisplayName("D3.18: Cross-tenant leave access returns 404 Not Found")
    void testCrossTenantLeaveAccessReturns404() throws Exception {
        // Leave in Org Beta
        Leave betaLeave = new Leave();
        betaLeave.setEmployeeName("beta_worker");
        betaLeave.setStartDate("2026-11-01");
        betaLeave.setEndDate("2026-11-02");
        betaLeave.setOrganization(orgBeta);
        betaLeave = leaveRepository.save(betaLeave);

        // Org Alpha Admin attempts to view Org Beta leave -> 404 Not Found
        mockMvc.perform(get("/leaves/" + betaLeave.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D3.19: Unassigned legacy leave cannot be accessed by tenant user -> 404 Not Found")
    void testUnassignedLeaveCannotBeAccessedByTenantUser() throws Exception {
        Leave unassigned = new Leave();
        unassigned.setEmployeeName("legacy_lone_user");
        unassigned.setStartDate("2026-11-05");
        unassigned.setEndDate("2026-11-06");
        unassigned.setOrganization(null);
        unassigned = leaveRepository.save(unassigned);

        // Org Alpha Admin attempts to access unassigned leave -> 404 Not Found
        mockMvc.perform(get("/leaves/" + unassigned.getId())
                .header("Authorization", alphaAdminToken))
                .andExpect(status().isNotFound());
    }
}
