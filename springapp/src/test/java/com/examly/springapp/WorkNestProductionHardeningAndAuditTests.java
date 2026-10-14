package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkNestProductionHardeningAndAuditTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveAuditEventRepository auditEventRepository;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String employeeToken;
    private final String testEmpUsername = "audit_employee";

    @BeforeEach
    void setUp() {
        // Setup admin
        User admin = userRepository.findByUsername("admin");
        if (admin == null) {
            admin = new User("admin", passwordEncoder.encode("password123"), "admin");
            admin.setLeaveBalance(20);
            userRepository.save(admin);
        }
        adminToken = "Bearer " + jwtUtil.generateToken("admin", "admin");

        // Setup test employee
        User emp = userRepository.findByUsername(testEmpUsername);
        if (emp == null) {
            emp = new User(testEmpUsername, passwordEncoder.encode("password123"), "employee");
            emp.setLeaveBalance(20);
            userRepository.save(emp);
        } else {
            emp.setLeaveBalance(20);
            userRepository.save(emp);
        }
        employeeToken = "Bearer " + jwtUtil.generateToken(testEmpUsername, "employee");
    }

    // =========================================================================
    // TASK 2A: JWT SECRET VALIDATION & PRODUCTION FAIL-CLOSED TESTS
    // =========================================================================

    @Test
    @DisplayName("2A.1: Production profile missing JWT_SECRET must fail closed with IllegalStateException")
    void testJwtSecretMissingInProductionProfileFailsStartup() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, testJwt::validateAndInitializeKey);
        assertTrue(ex.getMessage().contains("JWT_SECRET"), "Error message must state missing JWT_SECRET");
        assertTrue(ex.getMessage().contains("Production profile 'prod'"), "Error message must reference production profile");
    }

    @Test
    @DisplayName("2A.2: JWT secret with less than 32 bytes must fail closed for cryptographic safety")
    void testJwtSecretTooShortFailsStartup() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "short_secret_under_32_bytes");

        IllegalStateException ex = assertThrows(IllegalStateException.class, testJwt::validateAndInitializeKey);
        assertTrue(ex.getMessage().contains("at least 32 bytes"), "Error message must enforce 32-byte minimum");
    }

    @Test
    @DisplayName("2A.3: Valid 32+ byte JWT secret initializes signing key successfully")
    void testValidJwtSecretInitializesSuccessfully() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "this_is_a_sufficiently_long_32_byte_secret_key_12345");

        assertDoesNotThrow(testJwt::validateAndInitializeKey);
        assertNotNull(testJwt.getSigningKey());
    }

    // =========================================================================
    // TASK 3: APPEND-ONLY AUDIT TRAIL TESTS
    // =========================================================================

    @Test
    @DisplayName("3A.1: Leave creation produces persistent CREATED audit event")
    void testLeaveCreationCreatesAuditEvent() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-01");
        leave.setEndDate("2026-11-03");
        leave.setReason("Annual conference");
        leave.setLeaveType("Annual Leave");

        Leave created = leaveService.createLeave(leave);
        assertNotNull(created.getId());

        List<LeaveAuditEvent> events = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(1, events.size(), "Exactly 1 audit event must exist for newly created leave");

        LeaveAuditEvent event = events.get(0);
        assertEquals("CREATED", event.getEventType());
        assertEquals(testEmpUsername, event.getActorUsername());
        assertEquals("Pending", event.getToStatus());
        assertNull(event.getFromStatus());
        assertEquals(20, event.getPreviousBalance());
        assertEquals(20, event.getNewBalance());
        assertNotNull(event.getEventTimestamp());
    }

    @Test
    @DisplayName("3A.2: Leave approval produces APPROVED audit event and records balance deduction")
    void testLeaveApprovalCreatesAuditEventAndDeductsBalance() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-05");
        leave.setEndDate("2026-11-06"); // 2 days
        leave.setReason("Doctor visit");
        leave.setLeaveType("Sick Leave");
        Leave created = leaveService.createLeave(leave);

        Leave approved = leaveService.approveLeave(created.getId(), "admin");
        assertEquals("Approved", approved.getStatus());

        List<LeaveAuditEvent> events = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(2, events.size(), "Must have CREATED and APPROVED events");

        LeaveAuditEvent approveEvent = events.get(1);
        assertEquals("APPROVED", approveEvent.getEventType());
        assertEquals("admin", approveEvent.getActorUsername());
        assertEquals("ADMIN", approveEvent.getActorRole());
        assertEquals("Pending", approveEvent.getFromStatus());
        assertEquals("Approved", approveEvent.getToStatus());
        assertEquals(20, approveEvent.getPreviousBalance());
        assertEquals(18, approveEvent.getNewBalance(), "Leave balance must be deducted from 20 to 18");
    }

    @Test
    @DisplayName("3A.3: Leave rejection produces REJECTED audit event without balance deduction")
    void testLeaveRejectionCreatesAuditEventWithoutDeduction() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-10");
        leave.setEndDate("2026-11-11");
        leave.setReason("Personal travel");
        leave.setLeaveType("Casual Leave");
        Leave created = leaveService.createLeave(leave);

        Leave rejected = leaveService.rejectLeave(created.getId(), "Staffing constraints", "admin");
        assertEquals("Rejected", rejected.getStatus());

        List<LeaveAuditEvent> events = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(2, events.size(), "Must have CREATED and REJECTED events");

        LeaveAuditEvent rejectEvent = events.get(1);
        assertEquals("REJECTED", rejectEvent.getEventType());
        assertEquals("admin", rejectEvent.getActorUsername());
        assertEquals("Pending", rejectEvent.getFromStatus());
        assertEquals("Rejected", rejectEvent.getToStatus());
        assertEquals(20, rejectEvent.getPreviousBalance());
        assertEquals(20, rejectEvent.getNewBalance(), "Balance must remain untouched upon rejection");
        assertTrue(rejectEvent.getDetails().contains("Staffing constraints"));
    }

    @Test
    @DisplayName("3A.4: Cancelling an Approved leave records CANCELLED event and refunds balance")
    void testCancelApprovedLeaveRefundsBalanceAndRecordsAudit() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-15");
        leave.setEndDate("2026-11-17"); // 3 days
        leave.setReason("Family event");
        leave.setLeaveType("Casual Leave");
        Leave created = leaveService.createLeave(leave);

        leaveService.approveLeave(created.getId(), "admin");
        User empAfterApprove = userRepository.findByUsername(testEmpUsername);
        assertEquals(17, empAfterApprove.getLeaveBalance(), "Balance should be 17 after approval");

        Leave cancelled = leaveService.cancelLeave(created.getId(), testEmpUsername);
        assertEquals("Cancelled", cancelled.getStatus());

        User empAfterCancel = userRepository.findByUsername(testEmpUsername);
        assertEquals(20, empAfterCancel.getLeaveBalance(), "Balance must be restored to 20 after cancellation");

        List<LeaveAuditEvent> events = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(3, events.size(), "Must have CREATED, APPROVED, and CANCELLED events");

        LeaveAuditEvent cancelEvent = events.get(2);
        assertEquals("CANCELLED", cancelEvent.getEventType());
        assertEquals(testEmpUsername, cancelEvent.getActorUsername());
        assertEquals("Approved", cancelEvent.getFromStatus());
        assertEquals("Cancelled", cancelEvent.getToStatus());
        assertEquals(17, cancelEvent.getPreviousBalance());
        assertEquals(20, cancelEvent.getNewBalance());
    }

    @Test
    @DisplayName("3A.5: Cancelling a Pending leave records CANCELLED event with no balance refund")
    void testCancelPendingLeaveDoesNotRefundBalance() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-20");
        leave.setEndDate("2026-11-21");
        leave.setReason("Change of plans");
        leave.setLeaveType("Casual Leave");
        Leave created = leaveService.createLeave(leave);

        Leave cancelled = leaveService.cancelLeave(created.getId(), testEmpUsername);
        assertEquals("Cancelled", cancelled.getStatus());

        User emp = userRepository.findByUsername(testEmpUsername);
        assertEquals(20, emp.getLeaveBalance(), "Balance should remain 20 (no refund for pending request)");

        List<LeaveAuditEvent> events = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(2, events.size(), "Must have CREATED and CANCELLED events");

        LeaveAuditEvent cancelEvent = events.get(1);
        assertEquals("CANCELLED", cancelEvent.getEventType());
        assertEquals("Pending", cancelEvent.getFromStatus());
        assertEquals("Cancelled", cancelEvent.getToStatus());
        assertEquals(20, cancelEvent.getPreviousBalance());
        assertEquals(20, cancelEvent.getNewBalance());
    }

    @Test
    @DisplayName("3A.6: Audit trail preserves full chronological history without overwriting earlier events")
    void testAuditTrailPreservesChronologicalHistoryWithoutOverwriting() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-25");
        leave.setEndDate("2026-11-26");
        leave.setReason("Sprint break");
        Leave created = leaveService.createLeave(leave);

        leaveService.approveLeave(created.getId(), "admin");
        leaveService.cancelLeave(created.getId(), testEmpUsername);

        List<LeaveAuditEvent> history = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(3, history.size(), "Must retain all 3 sequential audit events");

        assertEquals("CREATED", history.get(0).getEventType());
        assertEquals("APPROVED", history.get(1).getEventType());
        assertEquals("CANCELLED", history.get(2).getEventType());

        // Verify timestamps are non-null and sequential
        assertFalse(history.get(1).getEventTimestamp().isBefore(history.get(0).getEventTimestamp()));
        assertFalse(history.get(2).getEventTimestamp().isBefore(history.get(1).getEventTimestamp()));
    }

    @Test
    @DisplayName("3A.7: Failed transaction does not persist uncommitted audit event")
    void testFailedTransactionDoesNotPersistAuditEvent() {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-12-01");
        leave.setEndDate("2026-12-02");
        Leave created = leaveService.createLeave(leave);

        // Attempt illegal self-approval
        assertThrows(ResponseStatusException.class, () -> {
            leaveService.approveLeave(created.getId(), testEmpUsername);
        });

        // Verify no APPROVED event was saved
        List<LeaveAuditEvent> events = auditEventRepository.findByLeaveIdOrderByEventTimestampAsc(created.getId());
        assertEquals(1, events.size(), "Only CREATED event should exist; failed approval must roll back");
        assertEquals("CREATED", events.get(0).getEventType());
    }

    // =========================================================================
    // TASK 3C: SECURE AUDIT RETRIEVAL ENDPOINT TESTS
    // =========================================================================

    @Test
    @DisplayName("3C.1: Administrator can retrieve paginated audit trail via GET /leaves/{id}/audit")
    void testAdminCanRetrieveAuditHistory() throws Exception {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-12-05");
        leave.setEndDate("2026-12-06");
        Leave created = leaveService.createLeave(leave);

        mockMvc.perform(get("/leaves/" + created.getId() + "/audit")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$.content[0].actorUsername").value(testEmpUsername));
    }

    @Test
    @DisplayName("3C.2: Employee cannot access administrative audit history (403 Forbidden)")
    void testEmployeeForbiddenFromAuditEndpoint() throws Exception {
        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-12-10");
        leave.setEndDate("2026-12-11");
        Leave created = leaveService.createLeave(leave);

        mockMvc.perform(get("/leaves/" + created.getId() + "/audit")
                .header("Authorization", employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3C.3: Anonymous caller cannot access audit endpoint (401 Unauthorized)")
    void testAnonymousUnauthorizedFromAuditEndpoint() throws Exception {
        mockMvc.perform(get("/leaves/1/audit"))
                .andExpect(status().isUnauthorized());
    }
}
