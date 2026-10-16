package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.Organization;
import com.examly.springapp.model.OrganizationMembership;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.OrganizationRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.NotificationService;
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
public class WorkNestPhaseD2MultiTenantSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private LeaveAuditEventRepository auditEventRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Organization orgA;
    private Organization orgB;
    private Organization suspendedOrg;

    private User orgAAdminUser;
    private User orgAEmployeeUser;
    private User orgBAdminUser;
    private User orgBEmployeeUser;
    private User inactiveMemUser;
    private User suspendedOrgUser;
    private User platformAdminUser;
    private User orgAdminRoleUser;

    private String orgAAdminToken;
    private String orgAEmployeeToken;
    private String orgBAdminToken;
    private String orgBEmployeeToken;
    private String inactiveMemToken;
    private String suspendedOrgToken;
    private String platformAdminToken;
    private String orgAdminRoleToken;

    private Leave orgALeave;
    private Leave orgBLeave;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        // 1. Create Organizations
        orgA = organizationRepository.save(new Organization("Acme Corp " + ts, "acme-" + ts, "ACTIVE", Instant.now()));
        orgB = organizationRepository.save(new Organization("Beta Ltd " + ts, "beta-" + ts, "ACTIVE", Instant.now()));
        suspendedOrg = organizationRepository.save(new Organization("Suspended Inc " + ts, "suspended-" + ts, "SUSPENDED", Instant.now()));

        // 2. Create Users & Memberships for Org A
        orgAAdminUser = userRepository.save(new User("acme_admin_" + ts, passwordEncoder.encode("pass"), "admin"));
        orgAAdminUser.setLeaveBalance(25);
        userRepository.save(orgAAdminUser);
        membershipRepository.save(new OrganizationMembership(orgA, orgAAdminUser, "ORG_ADMIN", "ACTIVE", Instant.now()));
        orgAAdminToken = "Bearer " + jwtUtil.generateToken(orgAAdminUser.getUsername(), "admin");

        orgAEmployeeUser = userRepository.save(new User("acme_emp_" + ts, passwordEncoder.encode("pass"), "employee"));
        orgAEmployeeUser.setLeaveBalance(20);
        userRepository.save(orgAEmployeeUser);
        membershipRepository.save(new OrganizationMembership(orgA, orgAEmployeeUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        orgAEmployeeToken = "Bearer " + jwtUtil.generateToken(orgAEmployeeUser.getUsername(), "employee");

        // 3. Create Users & Memberships for Org B
        orgBAdminUser = userRepository.save(new User("beta_admin_" + ts, passwordEncoder.encode("pass"), "admin"));
        orgBAdminUser.setLeaveBalance(25);
        userRepository.save(orgBAdminUser);
        membershipRepository.save(new OrganizationMembership(orgB, orgBAdminUser, "ORG_ADMIN", "ACTIVE", Instant.now()));
        orgBAdminToken = "Bearer " + jwtUtil.generateToken(orgBAdminUser.getUsername(), "admin");

        orgBEmployeeUser = userRepository.save(new User("beta_emp_" + ts, passwordEncoder.encode("pass"), "employee"));
        orgBEmployeeUser.setLeaveBalance(20);
        userRepository.save(orgBEmployeeUser);
        membershipRepository.save(new OrganizationMembership(orgB, orgBEmployeeUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        orgBEmployeeToken = "Bearer " + jwtUtil.generateToken(orgBEmployeeUser.getUsername(), "employee");

        // 4. Create Inactive Membership User
        inactiveMemUser = userRepository.save(new User("inactive_user_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(orgA, inactiveMemUser, "EMPLOYEE", "INACTIVE", Instant.now()));
        inactiveMemToken = "Bearer " + jwtUtil.generateToken(inactiveMemUser.getUsername(), "employee");

        // 5. Create Suspended Org User
        suspendedOrgUser = userRepository.save(new User("suspended_user_" + ts, passwordEncoder.encode("pass"), "employee"));
        membershipRepository.save(new OrganizationMembership(suspendedOrg, suspendedOrgUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        suspendedOrgToken = "Bearer " + jwtUtil.generateToken(suspendedOrgUser.getUsername(), "employee");

        // 6. Create Pure Platform Admin (no org membership)
        platformAdminUser = userRepository.save(new User("platform_admin_" + ts, passwordEncoder.encode("pass"), "ROLE_PLATFORM_ADMIN"));
        platformAdminToken = "Bearer " + jwtUtil.generateToken(platformAdminUser.getUsername(), "ROLE_PLATFORM_ADMIN");

        // 7. Create Dedicated ORG_ADMIN user for role separation testing
        orgAdminRoleUser = userRepository.save(new User("dedicated_org_admin_" + ts, passwordEncoder.encode("pass"), "ROLE_ORG_ADMIN"));
        membershipRepository.save(new OrganizationMembership(orgA, orgAdminRoleUser, "ORG_ADMIN", "ACTIVE", Instant.now()));
        orgAdminRoleToken = "Bearer " + jwtUtil.generateToken(orgAdminRoleUser.getUsername(), "ROLE_ORG_ADMIN");

        // 8. Create Sample Leaves in Org A and Org B
        orgALeave = new Leave();
        orgALeave.setEmployeeName(orgAEmployeeUser.getUsername());
        orgALeave.setStartDate("2026-11-09");
        orgALeave.setEndDate("2026-11-13");
        orgALeave.setDaysCount(5);
        orgALeave.setStatus("Pending");
        orgALeave.setLeaveType("Annual Leave");
        orgALeave.setReason("Acme vacation");
        orgALeave.setOrganization(orgA);
        orgALeave = leaveRepository.save(orgALeave);

        orgBLeave = new Leave();
        orgBLeave.setEmployeeName(orgBEmployeeUser.getUsername());
        orgBLeave.setStartDate("2026-11-09");
        orgBLeave.setEndDate("2026-11-13");
        orgBLeave.setDaysCount(5);
        orgBLeave.setStatus("Pending");
        orgBLeave.setLeaveType("Annual Leave");
        orgBLeave.setReason("Beta conference");
        orgBLeave.setOrganization(orgB);
        orgBLeave = leaveRepository.save(orgBLeave);
    }

    // =========================================================================
    // SCENARIO 1: CROSS-TENANT DATA ISOLATION (Org A vs Org B)
    // =========================================================================

    @Test
    @DisplayName("D2.1: Leave List Isolation - Org A user sees only Org A leaves; Org B user sees only Org B leaves")
    void testLeaveListCrossTenantIsolation() throws Exception {
        // Org A Admin requests all leaves
        mockMvc.perform(get("/leaves")
                .header("Authorization", orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(orgAEmployeeUser.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(orgBEmployeeUser.getUsername()))));

        // Org B Admin requests all leaves
        mockMvc.perform(get("/leaves")
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(orgBEmployeeUser.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(orgAEmployeeUser.getUsername()))));
    }

    @Test
    @DisplayName("D2.2: Leave by ID Isolation - Org B user attempting to access Org A leave receives 404")
    void testLeaveByIdCrossTenantIsolation() throws Exception {
        // Org A user accesses Org A leave -> 200 OK
        mockMvc.perform(get("/leaves/" + orgALeave.getId())
                .header("Authorization", orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgALeave.getId()));

        // Org B user accesses Org A leave -> 404 Not Found (cross-tenant obscurity)
        mockMvc.perform(get("/leaves/" + orgALeave.getId())
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isNotFound());

        // Org B employee accesses Org A leave -> 404 Not Found
        mockMvc.perform(get("/leaves/" + orgALeave.getId())
                .header("Authorization", orgBEmployeeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D2.3: Approval & Rejection Cross-Tenant Isolation - Approver in Org B cannot approve or reject Org A leave")
    void testApprovalAndRejectionCrossTenantIsolation() throws Exception {
        // Org B Admin attempts to approve Org A leave -> 404 Not Found
        mockMvc.perform(put("/leaves/" + orgALeave.getId() + "/approve")
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isNotFound());

        // Org B Admin attempts to reject Org A leave -> 404 Not Found
        mockMvc.perform(put("/leaves/" + orgALeave.getId() + "/reject")
                .header("Authorization", orgBAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rejectionReason", "Cross tenant reject"))))
                .andExpect(status().isNotFound());

        // Leave in Org A must remain unchanged (Pending)
        Leave reloaded = leaveRepository.findById(orgALeave.getId()).orElseThrow();
        assertEquals("Pending", reloaded.getStatus(), "Org A leave must not be approved or rejected by Org B administrator");
    }

    @Test
    @DisplayName("D2.4: Cancellation Cross-Tenant Isolation - Org B user cannot cancel Org A leave")
    void testCancellationCrossTenantIsolation() throws Exception {
        // Org B user attempts to cancel Org A leave -> 404 Not Found
        mockMvc.perform(put("/leaves/" + orgALeave.getId() + "/cancel")
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isNotFound());

        // Leave in Org A must remain unchanged (Pending)
        Leave reloaded = leaveRepository.findById(orgALeave.getId()).orElseThrow();
        assertEquals("Pending", reloaded.getStatus(), "Org A leave must not be cancelled by Org B user");
    }

    @Test
    @DisplayName("D2.5: Calendar Isolation - Org A calendar displays only Org A leaves; Org B leaves are never returned")
    void testTeamCalendarCrossTenantIsolation() throws Exception {
        // Approve both leaves first so they appear in calendar
        orgALeave.setStatus("Approved");
        leaveRepository.save(orgALeave);
        orgBLeave.setStatus("Approved");
        leaveRepository.save(orgBLeave);

        // Org A user queries calendar
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", orgAEmployeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(orgAEmployeeUser.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(orgBEmployeeUser.getUsername()))));

        // Org B user queries calendar
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", orgBEmployeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].employeeName", hasItem(orgBEmployeeUser.getUsername())))
                .andExpect(jsonPath("$[*].employeeName", not(hasItem(orgAEmployeeUser.getUsername()))));
    }

    @Test
    @DisplayName("D2.6: Notification Isolation - Notifications generated in Org A are visible only to Org A users")
    void testNotificationCrossTenantIsolation() throws Exception {
        // Create notifications specifically for Org A and Org B
        notificationService.createNotification(orgA.getId(), orgAEmployeeUser.getUsername(), "Important update for Acme employees");
        notificationService.createNotification(orgB.getId(), orgBEmployeeUser.getUsername(), "Beta confidential announcement");

        // Org A employee retrieves notifications
        mockMvc.perform(get("/notifications")
                .header("Authorization", orgAEmployeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].message", hasItem("Important update for Acme employees")))
                .andExpect(jsonPath("$[*].message", not(hasItem("Beta confidential announcement"))));

        // Org B employee retrieves notifications
        mockMvc.perform(get("/notifications")
                .header("Authorization", orgBEmployeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].message", hasItem("Beta confidential announcement")))
                .andExpect(jsonPath("$[*].message", not(hasItem("Important update for Acme employees"))));
    }

    @Test
    @DisplayName("D2.7: Audit Trail Isolation - Audit events for Org A leaves are not visible to Org B administrators")
    void testAuditTrailCrossTenantIsolation() throws Exception {
        // Create an audit event for Org A leave
        LeaveAuditEvent auditA = new LeaveAuditEvent(
                orgALeave.getId(), "CREATED", orgAEmployeeUser.getUsername(), "EMPLOYEE",
                Instant.now(), null, "Pending", 20, 20, "Acme leave created"
        );
        auditA.setOrganizationId(orgA.getId());
        auditEventRepository.save(auditA);

        // Org A admin can view audit history
        mockMvc.perform(get("/leaves/" + orgALeave.getId() + "/audit")
                .header("Authorization", orgAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        // Org B admin attempting to view Org A audit trail receives 404
        mockMvc.perform(get("/leaves/" + orgALeave.getId() + "/audit")
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D2.8: AI Impact & OOO Isolation - Org B cannot evaluate AI impact or generate OOO for Org A leave")
    void testAiCrossTenantIsolation() throws Exception {
        // Org A user can analyze impact of their own org's leave
        mockMvc.perform(get("/ai/impact/" + orgALeave.getId())
                .header("Authorization", orgAAdminToken))
                .andExpect(status().isOk());

        // Org B user receives 404 when attempting to analyze impact of Org A leave
        mockMvc.perform(get("/ai/impact/" + orgALeave.getId())
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isNotFound());

        // Org B user receives 404 when attempting to generate OOO for Org A leave
        mockMvc.perform(get("/ai/ooo/" + orgALeave.getId())
                .header("Authorization", orgBAdminToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // SCENARIO 2: MEMBERSHIP LIFECYCLE AND ACCESS CONTROL
    // =========================================================================

    @Test
    @DisplayName("D2.9: Inactive membership fails closed (403 Forbidden)")
    void testInactiveMembershipFailsClosed() throws Exception {
        // User with INACTIVE membership attempts to view leaves
        mockMvc.perform(get("/leaves")
                .header("Authorization", inactiveMemToken))
                .andExpect(status().isForbidden());

        // User with INACTIVE membership attempts to view calendar
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", inactiveMemToken))
                .andExpect(status().isForbidden());

        // User with INACTIVE membership attempts to view notifications
        mockMvc.perform(get("/notifications")
                .header("Authorization", inactiveMemToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D2.10: Suspended organization fails closed (403 Forbidden)")
    void testSuspendedOrganizationFailsClosed() throws Exception {
        // User belonging to SUSPENDED organization attempts to view leaves
        mockMvc.perform(get("/leaves")
                .header("Authorization", suspendedOrgToken))
                .andExpect(status().isForbidden());

        // User belonging to SUSPENDED organization attempts to view calendar
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", suspendedOrgToken))
                .andExpect(status().isForbidden());

        // User belonging to SUSPENDED organization attempts to view notifications
        mockMvc.perform(get("/notifications")
                .header("Authorization", suspendedOrgToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D2.11: Ambiguous multi-organization membership without selector fails closed (403 Forbidden)")
    void testAmbiguousMultiOrganizationMembershipFailsClosed() throws Exception {
        long ts = System.currentTimeMillis();
        User multiOrgUser = userRepository.save(new User("multi_org_" + ts, passwordEncoder.encode("pass"), "employee"));
        // Belongs to BOTH Org A and Org B actively
        membershipRepository.save(new OrganizationMembership(orgA, multiOrgUser, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(orgB, multiOrgUser, "EMPLOYEE", "ACTIVE", Instant.now()));

        String multiOrgToken = "Bearer " + jwtUtil.generateToken(multiOrgUser.getUsername(), "employee");

        // Request must fail closed with 403 Forbidden
        mockMvc.perform(get("/leaves")
                .header("Authorization", multiOrgToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SCENARIO 3: ROLE SEPARATION
    // =========================================================================

    @Test
    @DisplayName("D2.12: ROLE_ORG_ADMIN cannot access platform migration endpoints (/admin/**)")
    void testOrgAdminCannotAccessPlatformMigrationEndpoints() throws Exception {
        // Dedicated ORG_ADMIN attempts to access preview migration -> 403 Forbidden
        mockMvc.perform(get("/admin/migration/preview")
                .header("Authorization", orgAdminRoleToken))
                .andExpect(status().isForbidden());

        // Dedicated ORG_ADMIN attempts to access execute migration -> 403 Forbidden
        mockMvc.perform(post("/admin/migration/execute")
                .header("Authorization", orgAdminRoleToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D2.13: Platform Administrator can access /admin/** but cannot access tenant leaves without membership")
    void testPlatformAdminAccessBoundaries() throws Exception {
        // Platform admin can access platform migration preview
        mockMvc.perform(get("/admin/migration/preview")
                .header("Authorization", platformAdminToken))
                .andExpect(status().isOk());

        // Platform admin without membership in Org A attempts to access Org A leave -> 404 Not Found
        mockMvc.perform(get("/leaves/" + orgALeave.getId())
                .header("Authorization", platformAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D2.14: Regular Employee cannot approve, reject, or view audit logs")
    void testEmployeePrivilegeBoundaries() throws Exception {
        // Employee cannot approve leave -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + orgALeave.getId() + "/approve")
                .header("Authorization", orgAEmployeeToken))
                .andExpect(status().isForbidden());

        // Employee cannot reject leave -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + orgALeave.getId() + "/reject")
                .header("Authorization", orgAEmployeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("rejectionReason", "Unauthorized rejection"))))
                .andExpect(status().isForbidden());

        // Employee cannot view audit trail -> 403 Forbidden
        mockMvc.perform(get("/leaves/" + orgALeave.getId() + "/audit")
                .header("Authorization", orgAEmployeeToken))
                .andExpect(status().isForbidden());
    }
}
