package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.controller.InvitationController;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.InvitationService;
import com.examly.springapp.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.invitation.dev-token-delivery=true"
})
public class WorkNestPhaseD3RemediationTests {

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
    private InvitationService invitationService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private InvitationController invitationController;

    private Organization orgAlpha;
    private Organization orgBeta;

    private User alphaAdmin;
    private User alphaHr;
    private User alphaMgrEng;
    private User alphaMgrSales;
    private User alphaEmp;

    private String alphaHrToken;
    private String alphaMgrEngToken;
    private String alphaMgrSalesToken;
    private String alphaEmpToken;

    private Department deptEngineering;
    private Department deptSales;

    private OrganizationMembership memAlphaMgrEng;
    private OrganizationMembership memAlphaMgrSales;
    private OrganizationMembership memAlphaEmp;

    @BeforeEach
    void setUp() {
        rateLimiterService.clear();

        long ts = System.currentTimeMillis();

        orgAlpha = organizationRepository.save(new Organization("Alpha Remediation " + ts, "alpha-rem-" + ts, "ACTIVE", Instant.now()));
        orgBeta = organizationRepository.save(new Organization("Beta Remediation " + ts, "beta-rem-" + ts, "ACTIVE", Instant.now()));

        deptEngineering = departmentRepository.save(new Department(orgAlpha, "Engineering", "ENG", null));
        deptSales = departmentRepository.save(new Department(orgAlpha, "Sales", "SLS", null));

        alphaAdmin = userRepository.save(new User("rem_admin_" + ts, passwordEncoder.encode("p"), "admin"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaAdmin, "ORG_ADMIN", "ACTIVE", Instant.now()));

        alphaHr = userRepository.save(new User("rem_hr_" + ts, passwordEncoder.encode("p"), "admin"));
        membershipRepository.save(new OrganizationMembership(orgAlpha, alphaHr, "HR_ADMIN", "ACTIVE", Instant.now()));
        alphaHrToken = "Bearer " + jwtUtil.generateToken(alphaHr.getUsername(), "HR_ADMIN");

        alphaMgrEng = userRepository.save(new User("rem_mgreng_" + ts, passwordEncoder.encode("p"), "manager"));
        memAlphaMgrEng = membershipRepository.save(new OrganizationMembership(orgAlpha, alphaMgrEng, "MANAGER", "ACTIVE", Instant.now()));
        alphaMgrEngToken = "Bearer " + jwtUtil.generateToken(alphaMgrEng.getUsername(), "MANAGER");
        deptEngineering.setManagerMembership(memAlphaMgrEng);
        deptEngineering = departmentRepository.save(deptEngineering);

        alphaMgrSales = userRepository.save(new User("rem_mgrsales_" + ts, passwordEncoder.encode("p"), "manager"));
        memAlphaMgrSales = membershipRepository.save(new OrganizationMembership(orgAlpha, alphaMgrSales, "MANAGER", "ACTIVE", Instant.now()));
        alphaMgrSalesToken = "Bearer " + jwtUtil.generateToken(alphaMgrSales.getUsername(), "MANAGER");
        deptSales.setManagerMembership(memAlphaMgrSales);
        deptSales = departmentRepository.save(deptSales);

        alphaEmp = userRepository.save(new User("rem_emp_" + ts, passwordEncoder.encode("p"), "employee"));
        memAlphaEmp = membershipRepository.save(new OrganizationMembership(orgAlpha, alphaEmp, "EMPLOYEE", "ACTIVE", Instant.now()));
        alphaEmpToken = "Bearer " + jwtUtil.generateToken(alphaEmp.getUsername(), "EMPLOYEE");

        EmployeeProfile empProfile = new EmployeeProfile(memAlphaEmp, orgAlpha, "EMP-" + ts, "Emp " + ts, "emp" + ts + "@alpha.test", "Engineering", "Developer", java.time.LocalDate.now(), "FULL_TIME");
        empProfile.setDepartmentRef(deptEngineering);
        employeeProfileRepository.save(empProfile);
    }

    // =========================================================================
    // H-1: SPOOFED IP RATE-LIMIT BYPASS PREVENTED
    // =========================================================================

    @Test
    @DisplayName("H-1: Rotating X-Forwarded-For headers cannot bypass rate limits when proxy is untrusted")
    void testRotatingXForwardedForCannotBypassRateLimit() throws Exception {
        // 15 requests allowed per minute on /invitations/activate (default sliding window)
        // An attacker rotates X-Forwarded-For on each request
        for (int i = 1; i <= 15; i++) {
            ActivationRequest req = new ActivationRequest("dummy_tok_" + i, "user_" + i, "Secret123!", "a@b.com");
            mockMvc.perform(post("/invitations/activate")
                    .header("X-Forwarded-For", "198.51.100." + i)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest()); // reaches service validation (token invalid)
        }

        // 16th request with yet another forged IP MUST be blocked by rate limiter (429 Too Many Requests)
        ActivationRequest req16 = new ActivationRequest("dummy_tok_16", "user_16", "Secret123!", "a@b.com");
        mockMvc.perform(post("/invitations/activate")
                .header("X-Forwarded-For", "198.51.100.99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req16)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("H-1: extractClientIp defaults to remoteAddr unless connecting proxy is explicitly trusted")
    void testExtractClientIpTrustedProxyValidation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");

        // Without trusted proxies configured, remoteAddr is used
        ReflectionTestUtils.setField(invitationController, "trustedProxiesConfig", "");
        String ipUntrusted = invitationController.extractClientIp(request);
        assertEquals("192.168.1.100", ipUntrusted, "Untrusted connecting IP must ignore X-Forwarded-For");

        // With explicit trusted proxy configured matching remoteAddr, forwarded IP is extracted
        ReflectionTestUtils.setField(invitationController, "trustedProxiesConfig", "192.168.1.100, 10.0.0.2");
        String ipTrusted = invitationController.extractClientIp(request);
        assertEquals("203.0.113.50", ipTrusted, "Trusted proxy connecting IP must extract first forwarded address");

        // Reset to empty
        ReflectionTestUtils.setField(invitationController, "trustedProxiesConfig", "");
    }

    // =========================================================================
    // H-2: SECURE INVITATION TOKEN DELIVERY
    // =========================================================================

    @Test
    @DisplayName("H-2: Raw token is never returned in API responses when devTokenDelivery is false")
    void testTokenHiddenByDefaultWhenDevDeliveryDisabled() throws Exception {
        // Temporarily set devTokenDelivery = false in service
        ReflectionTestUtils.setField(invitationService, "devTokenDelivery", false);
        try {
            long ts = System.currentTimeMillis();
            InvitationRequest req = new InvitationRequest("EMP-SEC-" + ts, "sec_" + ts + "@alpha.test", "Sec User", "EMPLOYEE", deptEngineering.getId());

            String res = mockMvc.perform(post("/invitations")
                    .header("Authorization", alphaHrToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rawToken").doesNotExist())
                    .andExpect(jsonPath("$.activationLink").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            EmployeeInvitation created = objectMapper.readValue(res, EmployeeInvitation.class);
            assertNull(created.getRawToken(), "Raw token must be null in response");
            assertNull(created.getActivationLink(), "Activation link must be null in response");

            // Database record has hash but no raw token
            EmployeeInvitation dbRecord = invitationRepository.findById(created.getId()).orElseThrow();
            assertNotNull(dbRecord.getTokenHash(), "Database must store token hash");
            assertNull(dbRecord.getRawToken(), "Database must never store raw token");
        } finally {
            ReflectionTestUtils.setField(invitationService, "devTokenDelivery", true);
        }
    }

    @Test
    @DisplayName("H-2: Raw token is populated only when devTokenDelivery is explicitly true")
    void testTokenReturnedWhenDevDeliveryExplicitlyEnabled() throws Exception {
        ReflectionTestUtils.setField(invitationService, "devTokenDelivery", true);
        long ts = System.currentTimeMillis();
        InvitationRequest req = new InvitationRequest("EMP-DEV-" + ts, "dev_" + ts + "@alpha.test", "Dev User", "EMPLOYEE", deptEngineering.getId());

        String res = mockMvc.perform(post("/invitations")
                .header("Authorization", alphaHrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawToken", notNullValue()))
                .andExpect(jsonPath("$.activationLink", anyOf(containsString("/activate#token="), containsString("/activate?token="))))
                .andReturn().getResponse().getContentAsString();

        EmployeeInvitation created = objectMapper.readValue(res, EmployeeInvitation.class);
        assertNotNull(created.getRawToken());
        assertNotNull(created.getActivationLink());
    }

    // =========================================================================
    // H-3: PENDING LEAVE AUTHORIZATION AFTER EMPLOYEE TRANSFER
    // =========================================================================

    @Test
    @DisplayName("H-3: Old manager cannot approve or reject pending leave after employee transfers; new manager can")
    void testDepartmentTransferPendingLeaveAuthorization() throws Exception {
        // 1. Employee in Engineering applies for leave
        Leave leave = new Leave();
        leave.setEmployeeName(alphaEmp.getUsername());
        leave.setStartDate("2026-12-01");
        leave.setEndDate("2026-12-05");
        leave.setReason("Annual break");
        leave.setOrganization(orgAlpha);
        leave.setDepartment(deptEngineering);
        leave = leaveRepository.save(leave);

        // 2. Transfer employee from Engineering to Sales in authoritative EmployeeProfile
        EmployeeProfile profile = employeeProfileRepository
                .findByOrganizationIdAndMembershipUserUsername(orgAlpha.getId(), alphaEmp.getUsername())
                .orElseThrow();
        profile.setDepartmentRef(deptSales);
        employeeProfileRepository.save(profile);

        // 3. Old department manager (Engineering) attempts to approve -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + leave.getId() + "/approve")
                .header("Authorization", alphaMgrEngToken))
                .andExpect(status().isForbidden());

        // 4. Old department manager (Engineering) attempts to reject -> 403 Forbidden
        mockMvc.perform(put("/leaves/" + leave.getId() + "/reject")
                .header("Authorization", alphaMgrEngToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("reason", "Denied"))))
                .andExpect(status().isForbidden());

        // 5. New department manager (Sales) can approve the pending leave
        mockMvc.perform(put("/leaves/" + leave.getId() + "/approve")
                .header("Authorization", alphaMgrSalesToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Approved")));
    }

    @Test
    @DisplayName("H-3: Manager cannot approve or reject leave if employee profile has no department (unassigned)")
    void testUnassignedEmployeeLeaveCannotBeApprovedByManager() throws Exception {
        Leave leave = new Leave();
        leave.setEmployeeName(alphaEmp.getUsername());
        leave.setStartDate("2026-12-10");
        leave.setEndDate("2026-12-12");
        leave.setReason("Vacation");
        leave.setOrganization(orgAlpha);
        leave.setDepartment(deptEngineering);
        leave = leaveRepository.save(leave);

        // Remove employee department from profile (unassigned)
        EmployeeProfile profile = employeeProfileRepository
                .findByOrganizationIdAndMembershipUserUsername(orgAlpha.getId(), alphaEmp.getUsername())
                .orElseThrow();
        profile.setDepartmentRef(null);
        employeeProfileRepository.save(profile);

        // Both managers fail with 403 Forbidden
        mockMvc.perform(put("/leaves/" + leave.getId() + "/approve")
                .header("Authorization", alphaMgrEngToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/leaves/" + leave.getId() + "/approve")
                .header("Authorization", alphaMgrSalesToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // M-1: MANAGER INDIVIDUAL LEAVE RECORD SCOPING (GET /leaves/{id})
    // =========================================================================

    @Test
    @DisplayName("M-1: Manager can view leave in their department but is forbidden from other departments")
    void testManagerGetLeaveByIdScoping() throws Exception {
        // Leave for employee in Engineering
        Leave engLeave = new Leave();
        engLeave.setEmployeeName(alphaEmp.getUsername());
        engLeave.setStartDate("2026-11-20");
        engLeave.setEndDate("2026-11-21");
        engLeave.setOrganization(orgAlpha);
        engLeave.setDepartment(deptEngineering);
        engLeave = leaveRepository.save(engLeave);

        // Engineering manager CAN view leave in their department -> 200 OK
        mockMvc.perform(get("/leaves/" + engLeave.getId())
                .header("Authorization", alphaMgrEngToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(engLeave.getId().intValue())));

        // Sales manager CANNOT view leave in Engineering -> 403 Forbidden
        mockMvc.perform(get("/leaves/" + engLeave.getId())
                .header("Authorization", alphaMgrSalesToken))
                .andExpect(status().isForbidden());

        // Employee themselves CAN view their own leave -> 200 OK
        mockMvc.perform(get("/leaves/" + engLeave.getId())
                .header("Authorization", alphaEmpToken))
                .andExpect(status().isOk());

        // Cross-tenant access to another org -> 404 Not Found
        Leave betaLeave = new Leave();
        betaLeave.setEmployeeName("beta_emp");
        betaLeave.setStartDate("2026-11-22");
        betaLeave.setEndDate("2026-11-23");
        betaLeave.setOrganization(orgBeta);
        betaLeave = leaveRepository.save(betaLeave);

        mockMvc.perform(get("/leaves/" + betaLeave.getId())
                .header("Authorization", alphaMgrEngToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // M-2: MANDATORY EMAIL VERIFICATION DURING ACTIVATION
    // =========================================================================

    @Test
    @DisplayName("M-2: Activation rejects missing, blank, and mismatched emails; accepts matching case-insensitively")
    void testActivationEmailVerification() throws Exception {
        long ts = System.currentTimeMillis();
        InvitationRequest req = new InvitationRequest("EMP-MAIL-" + ts, "verify_" + ts + "@alpha.test", "Verify User", "EMPLOYEE", deptEngineering.getId());

        String res = mockMvc.perform(post("/invitations")
                .header("Authorization", alphaHrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        EmployeeInvitation created = objectMapper.readValue(res, EmployeeInvitation.class);
        String rawToken = created.getRawToken();

        // 1. Missing email (null) -> 400 Bad Request
        ActivationRequest nullEmailReq = new ActivationRequest(rawToken, "user_null_" + ts, "Pass123!", null);
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullEmailReq)))
                .andExpect(status().isBadRequest());

        // 2. Blank email ("  ") -> 400 Bad Request
        ActivationRequest blankEmailReq = new ActivationRequest(rawToken, "user_blank_" + ts, "Pass123!", "   ");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blankEmailReq)))
                .andExpect(status().isBadRequest());

        // 3. Mismatched email -> 400 Bad Request
        ActivationRequest wrongEmailReq = new ActivationRequest(rawToken, "user_wrong_" + ts, "Pass123!", "different@alpha.test");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongEmailReq)))
                .andExpect(status().isBadRequest());

        // 4. Matching email with mixed case and leading/trailing whitespace -> 200 OK
        ActivationRequest matchingReq = new ActivationRequest(rawToken, "user_correct_" + ts, "Pass123!", "  VERIFY_" + ts + "@ALPHA.TEST  ");
        mockMvc.perform(post("/invitations/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matchingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("user_correct_" + ts)));
    }

    // =========================================================================
    // L-1: CONCURRENCY-SAFE INVITATION CONSUMPTION
    // =========================================================================

    @Test
    @DisplayName("L-1: Concurrent activation requests result in exactly one success")
    void testConcurrentActivationSingleWinner() throws Exception {
        long ts = System.currentTimeMillis();
        InvitationRequest req = new InvitationRequest("EMP-CONC-" + ts, "conc_" + ts + "@alpha.test", "Concurrent User", "EMPLOYEE", deptEngineering.getId());

        String res = mockMvc.perform(post("/invitations")
                .header("Authorization", alphaHrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        EmployeeInvitation created = objectMapper.readValue(res, EmployeeInvitation.class);
        String rawToken = created.getRawToken();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger status200 = new AtomicInteger(0);
        AtomicInteger status400 = new AtomicInteger(0);

        for (int i = 1; i <= threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ActivationRequest actReq = new ActivationRequest(rawToken, "user_conc_" + index + "_" + ts, "Pass123!", "conc_" + ts + "@alpha.test");
                    int httpStatus = mockMvc.perform(post("/invitations/activate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actReq)))
                            .andReturn().getResponse().getStatus();

                    if (httpStatus == 200) {
                        status200.incrementAndGet();
                    } else if (httpStatus == 400) {
                        status400.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, status200.get(), "Exactly one concurrent activation must succeed");
        assertEquals(1, status400.get(), "The losing concurrent activation must fail with 400 Bad Request");

        // Verify only 1 user exists with this email
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll().stream()
                .filter(p -> ("conc_" + ts + "@alpha.test").equalsIgnoreCase(p.getWorkEmail()))
                .toList();
        assertEquals(1, profiles.size(), "Only one employee profile must be created");
    }

    // =========================================================================
    // M-3: BOUNDED RATE-LIMITER MEMORY AND EXPIRY
    // =========================================================================

    @Test
    @DisplayName("M-3: RateLimiterService bounds memory capacity and prunes entries")
    void testRateLimiterBoundedCapacityAndExpiry() {
        // Create an isolated limiter with small capacity of 10
        RateLimiterService smallLimiter = new RateLimiterService(10);

        // Insert 20 distinct keys
        for (int i = 0; i < 20; i++) {
            smallLimiter.isAllowed("key-" + i, 10);
        }

        // Bounded map must not exceed capacity 10
        assertTrue(smallLimiter.size() <= 10, "RateLimiter size must be bounded to maxCapacity (was " + smallLimiter.size() + ")");

        // Oldest keys (e.g. key-0..key-9) should have been evicted by LRU
        // Verify limiter still operates correctly for subsequent checks
        assertTrue(smallLimiter.isAllowed("new-key", 5));
    }
}
