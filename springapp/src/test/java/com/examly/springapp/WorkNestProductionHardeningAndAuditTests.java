package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.LeaveRepository;
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
import org.springframework.http.MediaType;
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
    private LeaveRepository leaveRepository;

    @Autowired
    private LeaveAuditEventRepository auditEventRepository;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private ObjectMapper objectMapper;

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
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-04");
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
        leave.setStartDate("2026-11-16");
        leave.setEndDate("2026-11-18"); // 3 working days (Mon-Wed)
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
        leave.setStartDate("2026-12-07");
        leave.setEndDate("2026-12-08"); // 2 working days (Mon-Tue)
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

    // =========================================================================
    // PHASE A1: WORKING-DAY CALCULATION TESTS (TASK 4)
    // =========================================================================

    @Test
    @DisplayName("4.1: Monday through Friday produces exactly 5 working days")
    void testMondayThroughFridayIsFiveWorkingDays() {
        java.time.LocalDate monday = java.time.LocalDate.of(2026, 11, 2);
        java.time.LocalDate friday = java.time.LocalDate.of(2026, 11, 6);
        int workDays = leaveService.calculateWorkingDays(monday, friday);
        assertEquals(5, workDays, "Monday through Friday must be exactly 5 working days");

        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-06");
        leave.setReason("Sprint work week");
        leave.setLeaveType("Annual Leave");
        Leave created = leaveService.createLeave(leave);

        assertEquals(5, created.getDaysCount(), "Stored daysCount on created leave must be 5");
    }

    @Test
    @DisplayName("4.2: Friday through Monday produces exactly 2 working days")
    void testFridayThroughMondayIsTwoWorkingDays() {
        java.time.LocalDate friday = java.time.LocalDate.of(2026, 11, 6);
        java.time.LocalDate monday = java.time.LocalDate.of(2026, 11, 9);
        int workDays = leaveService.calculateWorkingDays(friday, monday);
        assertEquals(2, workDays, "Friday through Monday must be exactly 2 working days (excluding Sat and Sun)");

        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-06");
        leave.setEndDate("2026-11-09");
        leave.setReason("Long weekend");
        leave.setLeaveType("Casual Leave");
        Leave created = leaveService.createLeave(leave);

        assertEquals(2, created.getDaysCount(), "Stored daysCount must be 2 working days");
    }

    @Test
    @DisplayName("4.3: Saturday through Sunday produces 0 working days and request is rejected")
    void testSaturdayThroughSundayZeroWorkingDaysRejected() throws Exception {
        java.time.LocalDate saturday = java.time.LocalDate.of(2026, 11, 7);
        java.time.LocalDate sunday = java.time.LocalDate.of(2026, 11, 8);
        int workDays = leaveService.calculateWorkingDays(saturday, sunday);
        assertEquals(0, workDays, "Saturday through Sunday contains 0 working days");

        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-07");
        leave.setEndDate("2026-11-08");
        leave.setReason("Weekend only");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            leaveService.createLeave(leave);
        });
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("0 working days"), "Exception reason must mention 0 working days");

        // Also verify via HTTP POST /leaves
        String payload = objectMapper.writeValueAsString(leave);
        mockMvc.perform(post("/leaves")
                .header("Authorization", employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4.4: Monday through Wednesday produces exactly 3 working days")
    void testMondayThroughWednesdayIsThreeWorkingDays() {
        java.time.LocalDate monday = java.time.LocalDate.of(2026, 11, 2);
        java.time.LocalDate wednesday = java.time.LocalDate.of(2026, 11, 4);
        int workDays = leaveService.calculateWorkingDays(monday, wednesday);
        assertEquals(3, workDays, "Monday through Wednesday must be 3 working days");

        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-04");
        leave.setReason("Midweek break");
        Leave created = leaveService.createLeave(leave);

        assertEquals(3, created.getDaysCount(), "Stored daysCount must be 3");
    }

    @Test
    @DisplayName("4.5: Range containing a weekend and several weekdays calculates correctly")
    void testRangeContainingWeekendAndSeveralWeekdays() {
        // Wednesday Nov 4, 2026 through Thursday Nov 12, 2026:
        // Wed 4, Thu 5, Fri 6, Sat 7 (weekend), Sun 8 (weekend), Mon 9, Tue 10, Wed 11, Thu 12
        // Total working days = 7
        java.time.LocalDate wed = java.time.LocalDate.of(2026, 11, 4);
        java.time.LocalDate nextThu = java.time.LocalDate.of(2026, 11, 12);
        int workDays = leaveService.calculateWorkingDays(wed, nextThu);
        assertEquals(7, workDays, "Wed through next Thu must be exactly 7 working days (excluding weekend)");

        Leave leave = new Leave();
        leave.setEmployeeName(testEmpUsername);
        leave.setStartDate("2026-11-04");
        leave.setEndDate("2026-11-12");
        leave.setReason("Multi-week project break");
        Leave created = leaveService.createLeave(leave);

        assertEquals(7, created.getDaysCount(), "Stored daysCount must be 7");
    }

    @Test
    @DisplayName("4.6: Invalid date ranges are rejected with BAD_REQUEST")
    void testInvalidDateRangesRejected() {
        // Inverted range (end before start)
        java.time.LocalDate start = java.time.LocalDate.of(2026, 11, 10);
        java.time.LocalDate end = java.time.LocalDate.of(2026, 11, 5);
        ResponseStatusException exInverted = assertThrows(ResponseStatusException.class, () -> {
            leaveService.calculateWorkingDays(start, end);
        });
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exInverted.getStatusCode());

        // Range exceeding 366 calendar days
        java.time.LocalDate yearStart = java.time.LocalDate.of(2026, 1, 1);
        java.time.LocalDate yearEnd = java.time.LocalDate.of(2027, 2, 1);
        ResponseStatusException exTooLarge = assertThrows(ResponseStatusException.class, () -> {
            leaveService.calculateWorkingDays(yearStart, yearEnd);
        });
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exTooLarge.getStatusCode());

        // Malformed date strings via calculateDays
        ResponseStatusException exMalformed = assertThrows(ResponseStatusException.class, () -> {
            leaveService.calculateDays("invalid-date", "2026-11-05", 1);
        });
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exMalformed.getStatusCode());
    }

    @Test
    @DisplayName("4.7: Correct balance deduction after approval of working-day leave")
    void testCorrectBalanceDeductionAfterApproval() {
        String testUser = "weekday_deduct_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        u.setLeaveBalance(20);
        userRepository.save(u);

        // Friday Nov 6 to Monday Nov 9 = 2 working days
        Leave leave = new Leave();
        leave.setEmployeeName(testUser);
        leave.setStartDate("2026-11-06");
        leave.setEndDate("2026-11-09");
        leave.setReason("Weekend trip");
        leave.setLeaveType("Casual Leave");
        Leave created = leaveService.createLeave(leave);
        assertEquals(2, created.getDaysCount());

        leaveService.approveLeave(created.getId(), "admin");

        User updatedUser = userRepository.findByUsername(testUser);
        assertEquals(18, updatedUser.getLeaveBalance(), "Balance must be reduced from 20 to 18 (2 working days deducted)");
    }

    @Test
    @DisplayName("4.8: Correct refund when an approved request is cancelled")
    void testCorrectRefundWhenApprovedRequestIsCancelled() {
        String testUser = "weekday_refund_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        u.setLeaveBalance(20);
        userRepository.save(u);

        // Friday Nov 6 to Monday Nov 9 = 2 working days
        Leave leave = new Leave();
        leave.setEmployeeName(testUser);
        leave.setStartDate("2026-11-06");
        leave.setEndDate("2026-11-09");
        Leave created = leaveService.createLeave(leave);
        leaveService.approveLeave(created.getId(), "admin");

        User userAfterApprove = userRepository.findByUsername(testUser);
        assertEquals(18, userAfterApprove.getLeaveBalance());

        leaveService.cancelLeave(created.getId(), testUser);

        User userAfterCancel = userRepository.findByUsername(testUser);
        assertEquals(20, userAfterCancel.getLeaveBalance(), "Balance must be restored back to 20 (+2 refund)");
    }

    @Test
    @DisplayName("4.9: Pending cancellation does not modify balance")
    void testPendingCancellationDoesNotModifyBalance() {
        String testUser = "weekday_pending_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        u.setLeaveBalance(20);
        userRepository.save(u);

        Leave leave = new Leave();
        leave.setEmployeeName(testUser);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-04"); // 3 working days
        Leave created = leaveService.createLeave(leave);

        leaveService.cancelLeave(created.getId(), testUser);

        User userAfterCancel = userRepository.findByUsername(testUser);
        assertEquals(20, userAfterCancel.getLeaveBalance(), "Pending leave cancellation must not alter balance");
    }

    @Test
    @DisplayName("4.10: Existing historical duration is preserved and not silently rewritten on refund")
    void testHistoricalDurationNotSilentlyRewrittenOnRefund() {
        String testUser = "historical_user_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        // Simulate historical state: employee had 20, was previously deducted 4 calendar days (Fri-Mon), balance is 16
        u.setLeaveBalance(16);
        userRepository.save(u);

        // Simulate an existing historical approved leave in the database where daysCount = 4
        // (Friday Nov 6 to Monday Nov 9 was 4 calendar days under old system, but 2 working days under new system)
        Leave historicalLeave = new Leave();
        historicalLeave.setEmployeeName(testUser);
        historicalLeave.setStartDate("2026-11-06");
        historicalLeave.setEndDate("2026-11-09");
        historicalLeave.setDaysCount(4); // Originally stored historical deduction
        historicalLeave.setStatus("Approved");
        historicalLeave.setApprovedBy("admin");
        Leave savedHistorical = leaveRepository.save(historicalLeave);

        // When cancelling this historical approved leave
        leaveService.cancelLeave(savedHistorical.getId(), testUser);

        // Verify: The refund must be exactly the 4 days originally deducted, restoring balance to 20!
        User restoredUser = userRepository.findByUsername(testUser);
        assertEquals(20, restoredUser.getLeaveBalance(), "Cancellation of historical leave must refund the originally deducted 4 days, not recalculated 2 days");

        // Verify: The stored daysCount remains intact
        Leave fresh = leaveRepository.findById(savedHistorical.getId()).orElseThrow();
        assertEquals(4, fresh.getDaysCount(), "Historical daysCount must not be silently rewritten");
    }

    // =========================================================================
    // PHASE B: TEAM AVAILABILITY CALENDAR TESTS
    // =========================================================================

    @Test
    @DisplayName("5.1: Authenticated access to the calendar returns 200 OK")
    void testAuthenticatedAccessToCalendar() throws Exception {
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("5.2: Unauthorized anonymous access to calendar is rejected (401)")
    void testUnauthorizedAnonymousAccessToCalendarFails() throws Exception {
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5.3: Rejection of invalid date ranges in calendar endpoint")
    void testCalendarRejectsInvalidDateRanges() throws Exception {
        // 1. Inverted range (end before start)
        mockMvc.perform(get("/leaves/calendar?start=2026-11-20&end=2026-11-10")
                .header("Authorization", employeeToken))
                .andExpect(status().isBadRequest());

        // 2. Missing parameters
        mockMvc.perform(get("/leaves/calendar?start=2026-11-01")
                .header("Authorization", employeeToken))
                .andExpect(status().isBadRequest());

        // 3. Malformed date format
        mockMvc.perform(get("/leaves/calendar?start=2026-invalid-01&end=2026-11-30")
                .header("Authorization", employeeToken))
                .andExpect(status().isBadRequest());

        // 4. Excessive range (> 93 calendar days)
        mockMvc.perform(get("/leaves/calendar?start=2026-01-01&end=2026-05-01")
                .header("Authorization", employeeToken))
                .andExpect(status().isBadRequest());
    }

    private boolean containsLeaveId(String json, Long id) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
        if (root.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode node : root) {
                if (node.has("id") && node.get("id").asLong() == id) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("5.4: Inclusion of leave overlapping the requested period, including starting before")
    void testCalendarIncludesOverlappingLeaves() throws Exception {
        String testUser = "overlap_user_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(u);

        // Leave A: Starts BEFORE period and ends INSIDE period (Oct 28 to Nov 04)
        Leave leaveA = new Leave();
        leaveA.setEmployeeName(testUser);
        leaveA.setStartDate("2026-10-28");
        leaveA.setEndDate("2026-11-04");
        leaveA.setStatus("Approved");
        leaveA.setDaysCount(6);
        leaveRepository.save(leaveA);

        // Leave B: Starts and ends INSIDE period (Nov 10 to Nov 12)
        Leave leaveB = new Leave();
        leaveB.setEmployeeName(testUser);
        leaveB.setStartDate("2026-11-10");
        leaveB.setEndDate("2026-11-12");
        leaveB.setStatus("Approved");
        leaveB.setDaysCount(3);
        leaveRepository.save(leaveB);

        // Leave C: Starts INSIDE and ends AFTER period (Nov 26 to Dec 04)
        Leave leaveC = new Leave();
        leaveC.setEmployeeName(testUser);
        leaveC.setStartDate("2026-11-26");
        leaveC.setEndDate("2026-12-04");
        leaveC.setStatus("Approved");
        leaveC.setDaysCount(7);
        leaveRepository.save(leaveC);

        // Leave D: Strictly OUTSIDE period (Dec 10 to Dec 15)
        Leave leaveD = new Leave();
        leaveD.setEmployeeName(testUser);
        leaveD.setStartDate("2026-12-10");
        leaveD.setEndDate("2026-12-15");
        leaveD.setStatus("Approved");
        leaveD.setDaysCount(4);
        leaveRepository.save(leaveD);

        String json = mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Verify Leave A, B, and C are included, but Leave D is not
        assertTrue(containsLeaveId(json, leaveA.getId()), "Leave starting before period must be included");
        assertTrue(containsLeaveId(json, leaveB.getId()), "Leave inside period must be included");
        assertTrue(containsLeaveId(json, leaveC.getId()), "Leave ending after period must be included");
        assertFalse(containsLeaveId(json, leaveD.getId()), "Leave strictly outside period must be excluded");
    }

    @Test
    @DisplayName("5.5: Exclusion of rejected and cancelled leave from availability views")
    void testCalendarExcludesRejectedAndCancelledLeaves() throws Exception {
        String testUser = "excluded_user_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(u);

        // Rejected Leave
        Leave rejectedLeave = new Leave();
        rejectedLeave.setEmployeeName(testUser);
        rejectedLeave.setStartDate("2026-11-02");
        rejectedLeave.setEndDate("2026-11-04");
        rejectedLeave.setStatus("Rejected");
        rejectedLeave.setDaysCount(3);
        Leave savedRejected = leaveRepository.save(rejectedLeave);

        // Cancelled Leave
        Leave cancelledLeave = new Leave();
        cancelledLeave.setEmployeeName(testUser);
        cancelledLeave.setStartDate("2026-11-09");
        cancelledLeave.setEndDate("2026-11-11");
        cancelledLeave.setStatus("Cancelled");
        cancelledLeave.setDaysCount(3);
        Leave savedCancelled = leaveRepository.save(cancelledLeave);

        String json = mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(containsLeaveId(json, savedRejected.getId()), "Rejected leaves must be excluded from calendar");
        assertFalse(containsLeaveId(json, savedCancelled.getId()), "Cancelled leaves must be excluded from calendar");
    }

    @Test
    @DisplayName("5.6: Explicitly defined handling of pending leave (privacy & role-based visibility)")
    void testCalendarPendingLeavePrivacyHandling() throws Exception {
        String colleague = "colleague_user_" + System.currentTimeMillis();
        User colleagueUser = new User(colleague, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(colleagueUser);

        // Colleague's Pending request
        Leave colleaguePending = new Leave();
        colleaguePending.setEmployeeName(colleague);
        colleaguePending.setStartDate("2026-11-16");
        colleaguePending.setEndDate("2026-11-18");
        colleaguePending.setStatus("Pending");
        colleaguePending.setDaysCount(3);
        Leave savedColleaguePending = leaveRepository.save(colleaguePending);

        // Current employee's OWN Pending request
        Leave ownPending = new Leave();
        ownPending.setEmployeeName(testEmpUsername);
        ownPending.setStartDate("2026-11-23");
        ownPending.setEndDate("2026-11-25");
        ownPending.setStatus("Pending");
        ownPending.setDaysCount(3);
        Leave savedOwnPending = leaveRepository.save(ownPending);

        // 1. When viewed by standard employee:
        String employeeJson = mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(containsLeaveId(employeeJson, savedOwnPending.getId()), "Employee must see their OWN pending leave on calendar");
        assertFalse(containsLeaveId(employeeJson, savedColleaguePending.getId()), "Employee must NOT see a colleague's pending leave");

        // 2. When viewed by admin:
        String adminJson = mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(containsLeaveId(adminJson, savedColleaguePending.getId()), "Admin must see colleague's pending leave for planning");
        assertTrue(containsLeaveId(adminJson, savedOwnPending.getId()), "Admin must see employee's pending leave");
    }

    @Test
    @DisplayName("5.7: Absence of leave reasons and sensitive fields in serialized calendar responses")
    void testCalendarOmitsReasonsAndSensitiveFields() throws Exception {
        String testUser = "privacy_user_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(u);

        Leave sensitiveLeave = new Leave();
        sensitiveLeave.setEmployeeName(testUser);
        sensitiveLeave.setStartDate("2026-11-02");
        sensitiveLeave.setEndDate("2026-11-04");
        sensitiveLeave.setStatus("Approved");
        sensitiveLeave.setDaysCount(3);
        sensitiveLeave.setReason("Confidential medical biopsy and personal treatment plan");
        sensitiveLeave.setRejectionReason("Not applicable");
        sensitiveLeave.setAiRecommendation("AI recommendation summary with confidential risk factors");
        sensitiveLeave.setImpactScore("High");
        Leave saved = leaveRepository.save(sensitiveLeave);

        String json = mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(containsLeaveId(json, saved.getId()));
        assertTrue(json.contains("Out of Office"), "Display label should be Out of Office");
        assertFalse(json.contains("Confidential medical biopsy"), "Private reason must NEVER be returned in calendar DTO");
        assertFalse(json.contains("confidential risk factors"), "AI recommendation must NEVER be returned in calendar DTO");
        assertFalse(json.contains("\"reason\":"), "JSON must not contain a 'reason' field");
        assertFalse(json.contains("\"rejectionReason\":"), "JSON must not contain a 'rejectionReason' field");
        assertFalse(json.contains("\"aiRecommendation\":"), "JSON must not contain an 'aiRecommendation' field");
    }

    @Test
    @DisplayName("5.8: Empty calendar results return empty JSON array (200 OK)")
    void testCalendarEmptyResultsWhenNoAbsences() throws Exception {
        mockMvc.perform(get("/leaves/calendar?start=2029-06-01&end=2029-06-30")
                .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("5.9: Correct use of stored daysCount in calendar response")
    void testCalendarUsesStoredDaysCount() throws Exception {
        String testUser = "dayscount_user_" + System.currentTimeMillis();
        User u = new User(testUser, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(u);

        Leave leave = new Leave();
        leave.setEmployeeName(testUser);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-06");
        leave.setDaysCount(5); // 5 working days
        leave.setStatus("Approved");
        Leave saved = leaveRepository.save(leave);

        mockMvc.perform(get("/leaves/calendar?start=2026-11-01&end=2026-11-30")
                .header("Authorization", employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + saved.getId() + ")].daysCount").value(5));
    }
}
