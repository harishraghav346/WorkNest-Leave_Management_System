package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.repository.UserRepository;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkNestSecurityAndBalanceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        // Ensure standard admin and employee exist
        User admin = userRepository.findByUsername("admin");
        if (admin == null) {
            admin = new User("admin", passwordEncoder.encode("password123"), "admin");
            admin.setLeaveBalance(20);
            userRepository.save(admin);
        } else if (!"admin".equals(admin.getRole())) {
            admin.setRole("admin");
            userRepository.save(admin);
        }

        User employee1 = userRepository.findByUsername("employee1");
        if (employee1 == null) {
            employee1 = new User("employee1", passwordEncoder.encode("password123"), "employee");
            employee1.setLeaveBalance(20);
            userRepository.save(employee1);
        }

        adminToken = jwtUtil.generateToken("admin", "admin");
        employeeToken = jwtUtil.generateToken("employee1", "employee");
    }

    @Test
    @DisplayName("1. Public registration cannot create an administrator account")
    void testPublicRegistrationCannotCreateAdmin() throws Exception {
        String testUser = "public_reg_user_" + System.currentTimeMillis();
        Map<String, String> regPayload = Map.of(
                "username", testUser,
                "password", "secretPass123",
                "role", "admin" // Attempt privilege escalation
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("employee"));

        User savedUser = userRepository.findByUsername(testUser);
        assertNotNull(savedUser, "Registered user should exist in DB");
        assertEquals("employee", savedUser.getRole(), "Role must be forced to 'employee', preventing privilege escalation");
    }

    @Test
    @DisplayName("2. Unauthenticated users cannot access protected operations")
    void testUnauthenticatedUsersBlockedFromProtectedOperations() throws Exception {
        // Unauthenticated access to /leaves
        mockMvc.perform(get("/leaves"))
                .andExpect(status().isUnauthorized());

        // Unauthenticated access to /notifications
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());

        // Unauthenticated access to /leaves/1/approve
        mockMvc.perform(put("/leaves/1/approve"))
                .andExpect(status().isUnauthorized());

        // Unauthenticated access to /ai/impact/1
        mockMvc.perform(get("/ai/impact/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Employees cannot approve their own leave")
    void testEmployeesCannotApproveOwnLeave() throws Exception {
        // Create a leave request where the applicant is 'admin'
        Leave leave = new Leave();
        leave.setEmployeeName("admin");
        leave.setStartDate("2026-11-10");
        leave.setEndDate("2026-11-11");
        leave.setReason("Personal emergency");
        leave.setStatus("Pending");
        leave.setDaysCount(2);
        Leave savedLeave = leaveRepository.save(leave);

        // When 'admin' attempts to approve their own leave request
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // Verify status remains unchanged
        Leave fresh = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals("Pending", fresh.getStatus(), "Status should remain Pending after forbidden self-approval attempt");
    }

    @Test
    @DisplayName("4. Unauthorized users cannot approve another employee's leave")
    void testUnauthorizedUsersCannotApproveLeave() throws Exception {
        // Create a leave request for employee1
        Leave leave = new Leave();
        leave.setEmployeeName("employee1");
        leave.setStartDate("2026-11-15");
        leave.setEndDate("2026-11-16");
        leave.setReason("Vacation");
        leave.setStatus("Pending");
        leave.setDaysCount(2);
        Leave savedLeave = leaveRepository.save(leave);

        // Another employee (non-admin) attempts to approve it
        String regularEmployeeToken = jwtUtil.generateToken("employee2", "employee");

        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + regularEmployeeToken))
                .andExpect(status().isForbidden());

        // Verify status remains unchanged
        Leave fresh = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals("Pending", fresh.getStatus(), "Leave should remain Pending when non-admin attempts approval");
    }

    @Test
    @DisplayName("5. Approving a three-day request deducts three days from the appropriate balance")
    void testThreeDayLeaveDeductionOnApproval() throws Exception {
        String empUsername = "test_deduct_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        // 3-day leave: Nov 2 to Nov 4 (Monday through Wednesday = 3 working days)
        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-04");
        leave.setReason("Family function");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Admin approves the request
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved"));

        // Verify employee balance deducted by 3 days
        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(17, freshEmp.getLeaveBalance(), "Balance should be reduced from 20 to 17 (3 days deducted)");
    }

    @Test
    @DisplayName("6. Repeating an approval does not deduct leave twice")
    void testDuplicateApprovalProtection() throws Exception {
        String empUsername = "test_dup_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-03"); // 2 working days
        leave.setReason("Medical checkup");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // First approval succeeds
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(18, freshEmp.getLeaveBalance(), "Balance should be 18 after first approval");

        // Second approval attempt should be rejected
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Balance should STILL be 18, not deducted again
        User freshEmpAfterSecondAttempt = userRepository.findByUsername(empUsername);
        assertEquals(18, freshEmpAfterSecondAttempt.getLeaveBalance(), "Balance must not be deducted a second time");
    }

    @Test
    @DisplayName("7. Rejected requests do not deduct leave")
    void testRejectedRequestsDoNotDeductLeave() throws Exception {
        String empUsername = "test_reject_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-05");
        leave.setEndDate("2026-11-08"); // 4 days
        leave.setReason("Trip");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        Map<String, String> rejectBody = Map.of("rejectionReason", "Peak delivery period");

        // Admin rejects the request
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Rejected"))
                .andExpect(jsonPath("$.rejectionReason").value("Peak delivery period"));

        // Verify balance remains completely untouched
        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(20, freshEmp.getLeaveBalance(), "Balance must remain 20 after rejection");
    }

    @Test
    @DisplayName("8. Insufficient balance prevents approval where policy requires it")
    void testInsufficientBalancePreventsApproval() throws Exception {
        String empUsername = "test_lowbal_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(2); // Only 2 days available
        userRepository.save(testEmp);

        // Requesting 5 days
        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-10");
        leave.setEndDate("2026-11-14"); // 5 days
        leave.setReason("Extended vacation");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Approval must fail due to insufficient balance
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Verify leave remains Pending and balance remains untouched
        Leave freshLeave = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals("Pending", freshLeave.getStatus(), "Leave status should remain Pending");

        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(2, freshEmp.getLeaveBalance(), "Balance should remain unchanged at 2");
    }

    @Test
    @DisplayName("9. Database changes roll back appropriately when an operation fails")
    void testRollbackOnOperationFailure() throws Exception {
        String empUsername = "test_rollback_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-20");
        leave.setEndDate("2026-11-22");
        leave.setStatus("AlreadyProcessed"); // Invalid initial state
        Leave savedLeave = leaveRepository.save(leave);

        // Approval will fail because status is not 'Pending'
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Balance and status remain rolled back / untouched
        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(20, freshEmp.getLeaveBalance(), "Balance must remain 20 after failed operation");

        Leave freshLeave = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals("AlreadyProcessed", freshLeave.getStatus());
    }

    @Test
    @DisplayName("10. Existing authentication and legitimate workflows continue to work")
    void testExistingAuthenticationAndLegitimateWorkflows() throws Exception {
        // 1. Login as admin
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "admin",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("admin"));

        // 2. Login as employee
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "employee1",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("employee"));

        // 3. Employee creates a leave request
        Leave newLeave = new Leave();
        newLeave.setEmployeeName("employee1");
        newLeave.setStartDate("2026-12-01");
        newLeave.setEndDate("2026-12-01");
        newLeave.setReason("Single day appointment");
        newLeave.setLeaveType("Casual Leave");

        String createResp = mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLeave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andReturn().getResponse().getContentAsString();

        Leave created = objectMapper.readValue(createResp, Leave.class);

        // 4. Admin lists leaves
        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 5. Admin approves employee's leave
        mockMvc.perform(put("/leaves/" + created.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved"));

        // 6. Notifications endpoint accessible with auth
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // PHASE 3: LEAVE CANCELLATION WORKFLOW TESTS
    // =========================================================================

    @Test
    @DisplayName("11. Cancelling an approved request restores the correct balance")
    void testCancellingApprovedRequestRestoresBalance() throws Exception {
        String empUsername = "cancel_approved_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        // Create and approve a 3-day leave (Mon Nov 2 to Wed Nov 4)
        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-02");
        leave.setEndDate("2026-11-04"); // 3 working days
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Approve it -> balance decreases to 17
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User empAfterApproval = userRepository.findByUsername(empUsername);
        assertEquals(17, empAfterApproval.getLeaveBalance(), "Balance should be 17 after approval");

        // Employee cancels their approved request
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Balance must be fully restored back to 20
        User empAfterCancel = userRepository.findByUsername(empUsername);
        assertEquals(20, empAfterCancel.getLeaveBalance(), "Balance should be restored to 20 upon cancellation of approved leave");
    }

    @Test
    @DisplayName("12. Cancelling a pending request does not refund balance")
    void testCancellingPendingRequestDoesNotRefundBalance() throws Exception {
        String empUsername = "cancel_pending_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-05");
        leave.setEndDate("2026-11-06"); // 2 days
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Employee cancels their pending request
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Balance should remain 20 (not 22)
        User empAfterCancel = userRepository.findByUsername(empUsername);
        assertEquals(20, empAfterCancel.getLeaveBalance(), "Pending cancellation must not refund days that were never deducted");
    }

    @Test
    @DisplayName("13. Cancelling a rejected request does not refund balance")
    void testCancellingRejectedRequestDoesNotRefundBalance() throws Exception {
        String empUsername = "cancel_rejected_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-08");
        leave.setEndDate("2026-11-09");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Reject it
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rejectionReason", "Busy period"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Rejected"));

        // Cancel the rejected request
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Balance remains 20
        User empAfterCancel = userRepository.findByUsername(empUsername);
        assertEquals(20, empAfterCancel.getLeaveBalance(), "Cancelling rejected leave must not refund days");
    }

    @Test
    @DisplayName("14. Repeated cancellation does not issue duplicate refunds")
    void testRepeatedCancellationDoesNotIssueDuplicateRefunds() throws Exception {
        String empUsername = "cancel_dup_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-11");
        leave.setEndDate("2026-11-13"); // 3 working days (Wed-Fri)
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Approve it -> balance 17
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // First cancel -> balance restored to 20
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        User empAfterFirstCancel = userRepository.findByUsername(empUsername);
        assertEquals(20, empAfterFirstCancel.getLeaveBalance());

        // Repeated cancel attempt -> Bad Request (already Cancelled)
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());

        // Balance must remain 20 (not 23)
        User empAfterSecondCancel = userRepository.findByUsername(empUsername);
        assertEquals(20, empAfterSecondCancel.getLeaveBalance(), "Duplicate cancellation must not refund days a second time");
    }

    @Test
    @DisplayName("15. Employees cannot cancel another employee's request")
    void testEmployeeCannotCancelAnotherEmployeeRequest() throws Exception {
        String alice = "alice_" + System.currentTimeMillis();
        String bob = "bob_" + System.currentTimeMillis();

        User userAlice = new User(alice, passwordEncoder.encode("pass123"), "employee");
        userAlice.setLeaveBalance(20);
        userRepository.save(userAlice);

        User userBob = new User(bob, passwordEncoder.encode("pass123"), "employee");
        userBob.setLeaveBalance(20);
        userRepository.save(userBob);

        String bobToken = jwtUtil.generateToken(bob, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(alice);
        leave.setStartDate("2026-11-15");
        leave.setEndDate("2026-11-16");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Bob attempts to cancel Alice's leave -> Forbidden
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());

        // Alice's leave remains Pending
        Leave fresh = leaveRepository.findById(savedLeave.getId()).orElseThrow();
        assertEquals("Pending", fresh.getStatus(), "Leave should remain Pending after unauthorized cancel attempt");
    }

    @Test
    @DisplayName("16. Unauthenticated requests to cancel are rejected")
    void testUnauthenticatedCancelIsRejected() throws Exception {
        mockMvc.perform(put("/leaves/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("17. Invalid status transitions are rejected")
    void testInvalidStatusTransitionRejected() throws Exception {
        String empUsername = "invalid_trans_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-20");
        leave.setEndDate("2026-11-21");
        leave.setStatus("Archived"); // Invalid status
        Leave savedLeave = leaveRepository.save(leave);

        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("18. Failed operations do not leave partially updated data")
    void testFailedCancelOperationRollsBack() throws Exception {
        String empUsername = "rollback_cancel_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-25");
        leave.setEndDate("2026-11-26");
        leave.setStatus("Cancelled"); // Already cancelled
        Leave savedLeave = leaveRepository.save(leave);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());

        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(20, freshEmp.getLeaveBalance(), "Balance must remain unchanged after failed cancel");
    }

    @Test
    @DisplayName("19. Admin can cancel an employee's leave request")
    void testAdminCanCancelEmployeeLeave() throws Exception {
        String empUsername = "admin_cancels_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-11-23");
        leave.setEndDate("2026-11-25"); // 3 working days (Mon-Wed)
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Approve it first -> balance 17
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin cancels it on behalf of employee
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Balance restored back to 20
        User freshEmp = userRepository.findByUsername(empUsername);
        assertEquals(20, freshEmp.getLeaveBalance(), "Admin cancellation must restore balance for approved leave");
    }

    // =========================================================================
    // PHASE 4: Dynamic Employee Self-Service (ESS) Integration Tests (20 - 27)
    // =========================================================================

    @Test
    @DisplayName("20. GET /auth/me returns authenticated employee profile and balance without exposing password")
    void testAuthenticatedEmployeeProfileRetrievalViaAuthMe() throws Exception {
        String empUsername = "ess_profile_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("SecretPass123!"), "employee");
        testEmp.setLeaveBalance(18);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(empUsername))
                .andExpect(jsonPath("$.role").value("employee"))
                .andExpect(jsonPath("$.leaveBalance").value(18))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("21. Employee GET /leaves and /leaves/page isolates data to logged-in employee")
    void testEmployeeLeaveHistoryContainsOnlyOwnRecords() throws Exception {
        String empA = "ess_user_a_" + System.currentTimeMillis();
        String empB = "ess_user_b_" + System.currentTimeMillis();

        User userA = new User(empA, passwordEncoder.encode("pass123"), "employee");
        userA.setLeaveBalance(20);
        userRepository.save(userA);

        User userB = new User(empB, passwordEncoder.encode("pass123"), "employee");
        userB.setLeaveBalance(20);
        userRepository.save(userB);

        String tokenA = jwtUtil.generateToken(empA, "employee");

        // Leave for User A
        Leave leaveA = new Leave();
        leaveA.setEmployeeName(empA);
        leaveA.setStartDate("2026-12-01");
        leaveA.setEndDate("2026-12-02");
        leaveA.setStatus("Pending");
        leaveA.setReason("Vacation A");
        leaveRepository.save(leaveA);

        // Leave for User B
        Leave leaveB = new Leave();
        leaveB.setEmployeeName(empB);
        leaveB.setStartDate("2026-12-05");
        leaveB.setEndDate("2026-12-06");
        leaveB.setStatus("Pending");
        leaveB.setReason("Vacation B");
        leaveRepository.save(leaveB);

        // User A fetches list
        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeName").value(empA))
                .andExpect(jsonPath("$[0].reason").value("Vacation A"));

        // User A fetches paginated
        mockMvc.perform(get("/leaves/page?page=0&size=10")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeName").value(empA));
    }

    @Test
    @DisplayName("22. Employee accessing another employee's leave by ID returns 403 Forbidden")
    void testEmployeeCannotAccessOtherEmployeeLeaveById() throws Exception {
        String empA = "ess_idor_a_" + System.currentTimeMillis();
        String empB = "ess_idor_b_" + System.currentTimeMillis();

        User userA = new User(empA, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(userA);
        User userB = new User(empB, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(userB);

        String tokenA = jwtUtil.generateToken(empA, "employee");

        Leave leaveB = new Leave();
        leaveB.setEmployeeName(empB);
        leaveB.setStartDate("2026-12-10");
        leaveB.setEndDate("2026-12-11");
        leaveB.setStatus("Pending");
        Leave savedLeaveB = leaveRepository.save(leaveB);

        mockMvc.perform(get("/leaves/" + savedLeaveB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. Leave creation binds employeeName to authenticated user and ignores spoofed body name")
    void testLeaveCreationBindsToAuthenticatedContext() throws Exception {
        String realEmp = "ess_bind_user_" + System.currentTimeMillis();
        User testEmp = new User(realEmp, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(testEmp);

        String tokenReal = jwtUtil.generateToken(realEmp, "employee");

        String requestBody = "{"
                + "\"employeeName\":\"attacker_spoofed_name\","
                + "\"startDate\":\"2026-12-15\","
                + "\"endDate\":\"2026-12-16\","
                + "\"reason\":\"Personal time\""
                + "}";

        mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + tokenReal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName").value(realEmp)); // Must be realEmp, not spoofed
    }

    @Test
    @DisplayName("24. Invalid or expired JWT token is rejected with 401 Unauthorized")
    void testInvalidTokenRejected() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer malformed_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("25. Approved leave deductions immediately appear in /auth/me profile")
    void testApprovedLeaveDeductionsAppearInAuthMe() throws Exception {
        String empUsername = "ess_deduct_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-12-21");
        leave.setEndDate("2026-12-23"); // 3 working days (Mon-Wed)
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Approve leave
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Employee checks /auth/me -> balance must be 17
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveBalance").value(17));
    }

    @Test
    @DisplayName("26. Cancellation refunds immediately appear in /auth/me profile")
    void testCancellationRefundsAppearInAuthMe() throws Exception {
        String empUsername = "ess_refund_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-12-24");
        leave.setEndDate("2026-12-25"); // 2 days
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Approve leave -> balance 18
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Cancel leave -> balance restored to 20
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Employee checks /auth/me -> balance must be 20
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveBalance").value(20));
    }

    @Test
    @DisplayName("27. Rejected requests display rejection reason and correct status in history")
    void testRejectedRequestsDisplayRejectionReason() throws Exception {
        String empUsername = "ess_reject_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-12-28");
        leave.setEndDate("2026-12-29");
        leave.setStatus("Pending");
        Leave savedLeave = leaveRepository.save(leave);

        // Admin rejects with reason
        String rejectBody = "{\"rejectionReason\":\"Team sprint deadline\"}";
        mockMvc.perform(put("/leaves/" + savedLeave.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Rejected"))
                .andExpect(jsonPath("$.rejectionReason").value("Team sprint deadline"));

        // Employee fetches their leaves via GET /leaves
        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("Rejected"))
                .andExpect(jsonPath("$[0].rejectionReason").value("Team sprint deadline"));
    }

    // =========================================================================
    // PHASE 5: Audit-Trail & Governance Persistence Tests (28 - 31)
    // =========================================================================

    @Test
    @DisplayName("28. Leave creation sets createdAt audit timestamp")
    void testLeaveCreationSetsCreatedAtTimestamp() throws Exception {
        String empUsername = "audit_create_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        String requestBody = "{"
                + "\"startDate\":\"2026-12-01\","
                + "\"endDate\":\"2026-12-02\","
                + "\"reason\":\"Audit trail verification\""
                + "}";

        mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("29. Leave approval records approvedBy, actionBy, and actionAt in audit trail")
    void testLeaveApprovalRecordsAuditTrail() throws Exception {
        String empUsername = "audit_approve_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-12-04");
        leave.setEndDate("2026-12-05");
        leave.setStatus("Pending");
        Leave saved = leaveRepository.save(leave);

        mockMvc.perform(put("/leaves/" + saved.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved"))
                .andExpect(jsonPath("$.approvedBy").value("admin"))
                .andExpect(jsonPath("$.actionBy").value("admin"))
                .andExpect(jsonPath("$.actionAt").isNotEmpty());
    }

    @Test
    @DisplayName("30. Leave rejection records rejectedBy, actionBy, and actionAt in audit trail")
    void testLeaveRejectionRecordsAuditTrail() throws Exception {
        String empUsername = "audit_reject_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        userRepository.save(testEmp);

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-12-08");
        leave.setEndDate("2026-12-09");
        leave.setStatus("Pending");
        Leave saved = leaveRepository.save(leave);

        String rejectBody = "{\"rejectionReason\":\"Coverage unavailable\"}";
        mockMvc.perform(put("/leaves/" + saved.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Rejected"))
                .andExpect(jsonPath("$.rejectedBy").value("admin"))
                .andExpect(jsonPath("$.actionBy").value("admin"))
                .andExpect(jsonPath("$.actionAt").isNotEmpty());
    }

    @Test
    @DisplayName("31. Leave cancellation records cancelledBy, actionBy, and actionAt in audit trail")
    void testLeaveCancellationRecordsAuditTrail() throws Exception {
        String empUsername = "audit_cancel_user_" + System.currentTimeMillis();
        User testEmp = new User(empUsername, passwordEncoder.encode("pass123"), "employee");
        testEmp.setLeaveBalance(20);
        userRepository.save(testEmp);

        String userToken = jwtUtil.generateToken(empUsername, "employee");

        Leave leave = new Leave();
        leave.setEmployeeName(empUsername);
        leave.setStartDate("2026-12-12");
        leave.setEndDate("2026-12-13");
        leave.setStatus("Pending");
        Leave saved = leaveRepository.save(leave);

        mockMvc.perform(put("/leaves/" + saved.getId() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"))
                .andExpect(jsonPath("$.cancelledBy").value(empUsername))
                .andExpect(jsonPath("$.actionBy").value(empUsername))
                .andExpect(jsonPath("$.actionAt").isNotEmpty());
    }
}
