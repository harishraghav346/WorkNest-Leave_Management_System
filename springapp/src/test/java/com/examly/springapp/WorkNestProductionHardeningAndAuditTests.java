package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.LeaveService;
import com.examly.springapp.config.DataInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("2A.4: Production alias 'production' missing JWT_SECRET must fail closed")
    void testJwtSecretMissingInProductionAliasFailsStartup() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, testJwt::validateAndInitializeKey);
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
        assertTrue(ex.getMessage().contains("Production profile 'production'"));
    }

    @Test
    @DisplayName("2A.5: Default dev secret in production profile must fail closed")
    void testDefaultDevSecretInProductionFailsStartup() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", JwtUtil.DEFAULT_DEV_SECRET);

        IllegalStateException ex = assertThrows(IllegalStateException.class, testJwt::validateAndInitializeKey);
        assertTrue(ex.getMessage().contains("default development JWT secret cannot be used in production"));
    }

    @Test
    @DisplayName("2A.6: Base64-encoded 32+ byte secret initializes successfully and generates valid tokens")
    void testBase64SecretInitializesAndValidatesTokens() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        // 32 bytes of binary material encoded in base64: "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=" (32 bytes = "abcdefghijklmnopqrstuvwxyz123456")
        String base64Secret = "base64:YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", base64Secret);

        assertDoesNotThrow(testJwt::validateAndInitializeKey);
        assertNotNull(testJwt.getSigningKey());

        String token = testJwt.generateToken("testuser", "admin");
        assertTrue(testJwt.validateToken(token));
        assertEquals("testuser", testJwt.extractUsername(token));
        assertEquals("admin", testJwt.extractRole(token));
    }

    @Test
    @DisplayName("2A.7: Malformed Base64 secret fails closed with descriptive IllegalStateException")
    void testMalformedBase64SecretFailsStartup() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "base64:!!!not-valid-base64!!!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, testJwt::validateAndInitializeKey);
        assertTrue(ex.getMessage().contains("Failed to decode Base64"));
    }

    @Test
    @DisplayName("2A.8: Base64 secret with decoded length under 32 bytes fails closed")
    void testBase64SecretTooShortFailsStartup() {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        // Base64 for "short" (5 bytes)
        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "base64:c2hvcnQ=");

        IllegalStateException ex = assertThrows(IllegalStateException.class, testJwt::validateAndInitializeKey);
        assertTrue(ex.getMessage().contains("at least 32 bytes"));
    }

    @Test
    @DisplayName("2A.9: Expired JWT token is rejected by validateToken and protected endpoints (401 Unauthorized)")
    void testExpiredTokenRejected() throws Exception {
        JwtUtil testJwt = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(testJwt, "environment", env);
        ReflectionTestUtils.setField(testJwt, "secret", "valid_secret_key_at_least_32_bytes_long_12345");
        // Negative expiration yields expired token
        ReflectionTestUtils.setField(testJwt, "expiration", -10000L);
        testJwt.validateAndInitializeKey();

        String expiredToken = testJwt.generateToken("testuser", "employee");
        assertFalse(testJwt.validateToken(expiredToken), "Expired token must be rejected");

        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2A.10: Token signed with a different key is rejected by validateToken and protected endpoints")
    void testInvalidSignatureTokenRejected() throws Exception {
        JwtUtil testJwtOther = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(testJwtOther, "environment", env);
        ReflectionTestUtils.setField(testJwtOther, "secret", "completely_different_signing_key_at_least_32_bytes!");
        testJwtOther.validateAndInitializeKey();

        String tamperedToken = testJwtOther.generateToken("admin", "admin");

        assertFalse(jwtUtil.validateToken(tamperedToken), "Token signed with different key must fail validation");

        mockMvc.perform(get("/leaves/1/audit")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // TASK 2B: ADMINISTRATOR PROVISIONING & SECURITY TESTS
    // =========================================================================

    @Test
    @DisplayName("2B.1: Production DataInitializer preserves existing admin and does not overwrite password")
    void testExistingAdminPreservedOnRestart() {
        DataInitializer initializer = new DataInitializer();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        User existingAdmin = userRepository.findByUsername("admin");
        assertNotNull(existingAdmin);
        String originalPasswordHash = existingAdmin.getPassword();

        ReflectionTestUtils.setField(initializer, "userRepository", userRepository);
        ReflectionTestUtils.setField(initializer, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(initializer, "environment", env);
        ReflectionTestUtils.setField(initializer, "initialAdminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "initialAdminPassword", "NewPasswordThatShouldNotBeUsed123!");

        // Run data initializer as if application is restarting
        initializer.run();

        User adminAfterRestart = userRepository.findByUsername("admin");
        assertEquals(originalPasswordHash, adminAfterRestart.getPassword(), "Admin password hash must remain untouched on restart");
    }

    @Test
    @DisplayName("2B.2: Production DataInitializer creates admin with BCrypt hash when 0 admins exist and password is provided")
    void testAdminBootstrapWhenZeroAdminsExist() {
        DataInitializer initializer = new DataInitializer();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        UserRepository mockRepo = Mockito.mock(UserRepository.class);
        Mockito.when(mockRepo.findAll()).thenReturn(List.of());

        ReflectionTestUtils.setField(initializer, "userRepository", mockRepo);
        ReflectionTestUtils.setField(initializer, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(initializer, "environment", env);
        ReflectionTestUtils.setField(initializer, "initialAdminUsername", "bootstrapped_admin");
        ReflectionTestUtils.setField(initializer, "initialAdminPassword", "SecureBootPass2026!");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(mockRepo).save(userCaptor.capture());

        User savedAdmin = userCaptor.getValue();
        assertEquals("bootstrapped_admin", savedAdmin.getUsername());
        assertEquals("admin", savedAdmin.getRole());
        assertTrue(passwordEncoder.matches("SecureBootPass2026!", savedAdmin.getPassword()), "Bootstrap password must be BCrypt hashed");
        assertNotEquals("SecureBootPass2026!", savedAdmin.getPassword(), "Password must never be saved in plaintext");
    }

    @Test
    @DisplayName("2B.3: Production DataInitializer does NOT create admin when 0 admins exist but no password is provided")
    void testNoAdminCreatedWhenPasswordMissingInProduction() {
        DataInitializer initializer = new DataInitializer();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        UserRepository mockRepo = Mockito.mock(UserRepository.class);
        Mockito.when(mockRepo.findAll()).thenReturn(List.of());

        ReflectionTestUtils.setField(initializer, "userRepository", mockRepo);
        ReflectionTestUtils.setField(initializer, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(initializer, "environment", env);
        ReflectionTestUtils.setField(initializer, "initialAdminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "initialAdminPassword", "");

        initializer.run();

        Mockito.verify(mockRepo, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    @DisplayName("2B.4: User password is not exposed in JSON serialization")
    void testUserPasswordNotExposedInJsonSerialization() throws Exception {
        User user = new User("testuser", passwordEncoder.encode("secretpassword"), "employee");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user);

        assertFalse(json.contains("secretpassword"), "Plaintext password must not appear in JSON");
        assertFalse(json.contains("password"), "Password field should not be serialized due to WRITE_ONLY");
    }

    @Test
    @DisplayName("2B.5: BCrypt password encoder validates correct password and rejects incorrect password")
    void testBCryptPasswordVerification() {
        String rawPass = "MySecureP@ssword2026";
        String encoded = passwordEncoder.encode(rawPass);

        assertTrue(passwordEncoder.matches(rawPass, encoded), "BCrypt must match correct raw password");
        assertFalse(passwordEncoder.matches("WrongPassword", encoded), "BCrypt must reject incorrect password");
    }

    @Test
    @DisplayName("2B.6: Removing ADMIN_INITIAL_PASSWORD after initial setup does not affect existing admin login")
    void testRemovingAdminInitialPasswordDoesNotAffectExistingAdmin() {
        DataInitializer initializer = new DataInitializer();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        User existingAdmin = userRepository.findByUsername("admin");
        assertNotNull(existingAdmin);
        String savedPasswordHash = existingAdmin.getPassword();

        // Subsequent startup where ADMIN_INITIAL_PASSWORD is omitted
        ReflectionTestUtils.setField(initializer, "userRepository", userRepository);
        ReflectionTestUtils.setField(initializer, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(initializer, "environment", env);
        ReflectionTestUtils.setField(initializer, "initialAdminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "initialAdminPassword", "");

        initializer.run();

        User adminAfterReboot = userRepository.findByUsername("admin");
        assertEquals(savedPasswordHash, adminAfterReboot.getPassword(), "Admin password hash must remain untouched");
        assertTrue(passwordEncoder.matches("password123", adminAfterReboot.getPassword()), "Existing admin remains able to log in");
    }

    @Test
    @DisplayName("2B.7: Public registration cannot create an administrator even if role is requested as admin")
    void testPublicRegistrationCannotCreateAdmin() throws Exception {
        String testUsername = "reg_sec_user_" + System.currentTimeMillis();
        String requestJson = String.format("{\"username\":\"%s\",\"password\":\"securePass123!\",\"role\":\"admin\"}", testUsername);

        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("employee"));

        User created = userRepository.findByUsername(testUsername);
        assertNotNull(created);
        assertEquals("employee", created.getRole(), "Public registration must force employee role");
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
