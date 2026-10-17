package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.EmailDeliveryService;
import com.examly.springapp.service.OnboardingService;
import com.examly.springapp.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkNestPhaseD4RemediationTests {

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
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeInvitationRepository invitationRepository;

    @Autowired
    private OrganizationVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private JavaMailSender mockMailSender;

    @BeforeEach
    void setUp() {
        leaveRepository.deleteAll();
        invitationRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        employeeProfileRepository.deleteAll();
        membershipRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Reset rate limiter cache before each test
        rateLimiterService.clear();

        // Setup mock mail sender
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mockMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // 1. Pending Organization Access Denial Across All Tenant Endpoints
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: New organization registration creates PENDING_VERIFICATION state; login and tenant access are blocked")
    void testPendingOrganizationAccessDenial() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Security Corp",
                "security-corp",
                "secofficer",
                "officer@securitycorp.test",
                "Sec Officer",
                "Password123!"
        );

        mockMvc.perform(post("/auth/onboard-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresEmailVerification").value(true));

        // Verify database state: Organization & Membership are strictly PENDING_VERIFICATION
        Organization org = organizationRepository.findBySlug("security-corp").orElseThrow();
        assertEquals("PENDING_VERIFICATION", org.getStatus());

        User adminUser = userRepository.findByUsername("secofficer");
        assertNotNull(adminUser);

        List<OrganizationMembership> pendingMems = membershipRepository
                .findByUserIdAndStatus(adminUser.getId(), "PENDING_VERIFICATION");
        assertEquals(1, pendingMems.size());
        assertEquals("PENDING_VERIFICATION", pendingMems.get(0).getStatus());

        // Attempting to login with pending organization credentials must be denied
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "secofficer",
                        "password", "Password123!"
                ))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("pending email verification")));

        // Even with a forged token bearing the pending org's ID, tenant endpoints reject access
        String forgedPendingToken = jwtUtil.generateToken(adminUser.getUsername(), "ORG_ADMIN", org.getId(), org.getSlug());
        mockMvc.perform(get("/departments")
                .header("Authorization", "Bearer " + forgedPendingToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/leaves")
                .header("Authorization", "Bearer " + forgedPendingToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 2. Successful Verification and Correct Activation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: Valid verification token activates organization and membership, enabling login")
    void testSuccessfulVerificationAndActivation() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Active Tech",
                "active-tech",
                "alicetech",
                "alice@activetech.test",
                "Alice Tech",
                "Password123!"
        );

        onboardingService.onboardOrganization(req, "127.0.0.1");

        Organization org = organizationRepository.findBySlug("active-tech").orElseThrow();
        assertEquals("PENDING_VERIFICATION", org.getStatus());

        // Retrieve the generated pending token
        OrganizationVerificationToken tokenEntity = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        assertEquals("PENDING", tokenEntity.getStatus());

        // Simulate knowledge of raw token matching the hash
        String knownRawToken = "test-raw-token-1234567890abcdef1234567890";
        tokenEntity.setTokenHash(sha256(knownRawToken));
        verificationTokenRepository.save(tokenEntity);

        // Perform verification
        OrganizationVerificationRequest verifyReq = new OrganizationVerificationRequest();
        verifyReq.setToken(knownRawToken);
        verifyReq.setEmail("alice@activetech.test");

        mockMvc.perform(post("/auth/verify-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.status", is("ACTIVE")))
                .andExpect(jsonPath("$.organization.slug", is("active-tech")))
                .andExpect(jsonPath("$.token", notNullValue()));

        // Check DB: Both Org and Membership are now ACTIVE
        Organization activeOrg = organizationRepository.findBySlug("active-tech").orElseThrow();
        assertEquals("ACTIVE", activeOrg.getStatus());

        User user = userRepository.findByUsername("alicetech");
        OrganizationMembership activeMem = membershipRepository
                .findByOrganizationIdAndUserId(activeOrg.getId(), user.getId()).orElseThrow();
        assertEquals("ACTIVE", activeMem.getStatus());

        // Token record is now marked ACCEPTED
        OrganizationVerificationToken redeemedToken = verificationTokenRepository.findById(tokenEntity.getId()).orElseThrow();
        assertEquals("ACCEPTED", redeemedToken.getStatus());
        assertNotNull(redeemedToken.getVerifiedAt());

        // Now login succeeds
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "alicetech",
                        "password", "Password123!"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.organization.slug", is("active-tech")));
    }

    // -------------------------------------------------------------------------
    // 3. Invalid, Expired, Revoked, and Reused Verification Tokens
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: Invalid, expired, revoked, and reused verification tokens fail safely without disclosing tenant info")
    void testInvalidExpiredRevokedReusedTokens() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Flaky Corp",
                "flaky-corp",
                "frankflaky",
                "frank@flaky.test",
                "Frank Flaky",
                "Password123!"
        );
        onboardingService.onboardOrganization(req, "127.0.0.1");

        Organization org = organizationRepository.findBySlug("flaky-corp").orElseThrow();
        OrganizationVerificationToken tokenEntity = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        String rawToken = "raw-token-flaky-xyz-9876543210123456";
        tokenEntity.setTokenHash(sha256(rawToken));
        verificationTokenRepository.save(tokenEntity);

        // A. Non-existent / Invalid Token
        OrganizationVerificationRequest invalidReq = new OrganizationVerificationRequest();
        invalidReq.setToken("completely-invalid-token");
        invalidReq.setEmail("frank@flaky.test");

        mockMvc.perform(post("/auth/verify-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Invalid or expired")));

        // B. Expired Token
        tokenEntity.setExpiresAt(Instant.now().minus(2, ChronoUnit.HOURS));
        verificationTokenRepository.save(tokenEntity);

        OrganizationVerificationRequest expiredReq = new OrganizationVerificationRequest();
        expiredReq.setToken(rawToken);
        expiredReq.setEmail("frank@flaky.test");

        mockMvc.perform(post("/auth/verify-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expiredReq)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("expired, been revoked, or already redeemed")));

        // C. Revoked Token
        tokenEntity.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenEntity.setStatus("REVOKED");
        verificationTokenRepository.save(tokenEntity);

        OrganizationVerificationRequest revokedReq = new OrganizationVerificationRequest();
        revokedReq.setToken(rawToken);
        revokedReq.setEmail("frank@flaky.test");

        mockMvc.perform(post("/auth/verify-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revokedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("expired, been revoked, or already redeemed")));

        // D. Reused Token
        // Restore to pending and redeem once
        tokenEntity.setStatus("PENDING");
        verificationTokenRepository.save(tokenEntity);

        mockMvc.perform(post("/auth/verify-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revokedReq)))
                .andExpect(status().isOk());

        // Attempting to redeem again fails safely
        mockMvc.perform(post("/auth/verify-organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revokedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("expired, been revoked, or already redeemed")));
    }

    // -------------------------------------------------------------------------
    // 4. Concurrent Verification: Exactly One Request Succeeds
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: Concurrent verification requests race condition test; exactly one request activates token")
    void testConcurrentVerificationSingleWinner() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Race Corp",
                "race-corp",
                "rayracer",
                "ray@racecorp.test",
                "Ray Racer",
                "Password123!"
        );
        onboardingService.onboardOrganization(req, "127.0.0.1");

        Organization org = organizationRepository.findBySlug("race-corp").orElseThrow();
        OrganizationVerificationToken tokenEntity = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        String rawToken = "concurrent-race-token-1234567890abcdef";
        tokenEntity.setTokenHash(sha256(rawToken));
        verificationTokenRepository.save(tokenEntity);

        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    OrganizationVerificationRequest verifyReq = new OrganizationVerificationRequest();
                    verifyReq.setToken(rawToken);
                    verifyReq.setEmail("ray@racecorp.test");
                    onboardingService.verifyOrganization(verifyReq, "127.0.0.1");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one concurrent call must succeed
        assertEquals(1, successCount.get(), "Exactly one concurrent verification call must succeed");
        assertEquals(threadCount - 1, failureCount.get(), "All other concurrent calls must fail");

        // Organization must be ACTIVE
        Organization verifiedOrg = organizationRepository.findBySlug("race-corp").orElseThrow();
        assertEquals("ACTIVE", verifiedOrg.getStatus());
    }

    // -------------------------------------------------------------------------
    // 5. Transaction Rollback after Injected Failures
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: Email mismatch or verification failure rolls back and preserves PENDING state")
    void testTransactionRollbackOnMismatch() {
        OnboardingRequest req = new OnboardingRequest(
                "Rollback Corp",
                "rollback-corp",
                "robroller",
                "rob@rollback.test",
                "Rob Roller",
                "Password123!"
        );
        onboardingService.onboardOrganization(req, "127.0.0.1");

        Organization org = organizationRepository.findBySlug("rollback-corp").orElseThrow();
        OrganizationVerificationToken tokenEntity = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        String rawToken = "rollback-token-1234567890123456";
        tokenEntity.setTokenHash(sha256(rawToken));
        verificationTokenRepository.save(tokenEntity);

        // Attempt verification with an unlinked email address
        OrganizationVerificationRequest mismatchReq = new OrganizationVerificationRequest();
        mismatchReq.setToken(rawToken);
        mismatchReq.setEmail("attacker@evil.test");

        assertThrows(RuntimeException.class, () -> onboardingService.verifyOrganization(mismatchReq, "127.0.0.1"));

        // Assert organization remains in PENDING_VERIFICATION state (no dirty partial update)
        Organization unmutatedOrg = organizationRepository.findBySlug("rollback-corp").orElseThrow();
        assertEquals("PENDING_VERIFICATION", unmutatedOrg.getStatus());

        OrganizationVerificationToken unmutatedToken = verificationTokenRepository.findById(tokenEntity.getId()).orElseThrow();
        assertEquals("PENDING", unmutatedToken.getStatus());
    }

    // -------------------------------------------------------------------------
    // 6. Resend Verification Throttling and Duplicate-Send Prevention
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: Resend verification enforces rate limits and revokes previous pending token")
    void testResendVerificationThrottlingAndRevocation() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Throttled Corp",
                "throttled-corp",
                "tomthrottle",
                "tom@throttled.test",
                "Tom Throttle",
                "Password123!"
        );
        onboardingService.onboardOrganization(req, "127.0.0.1");

        Organization org = organizationRepository.findBySlug("throttled-corp").orElseThrow();
        OrganizationVerificationToken firstToken = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        Long firstTokenId = firstToken.getId();

        // 1st Resend: succeeds
        ResendVerificationRequest resendReq = new ResendVerificationRequest();
        resendReq.setEmail("tom@throttled.test");

        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If an unverified organization exists")));

        // Verify previous token is REVOKED
        OrganizationVerificationToken oldToken = verificationTokenRepository.findById(firstTokenId).orElseThrow();
        assertEquals("REVOKED", oldToken.getStatus());

        // A new PENDING token exists
        OrganizationVerificationToken newToken = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        assertEquals("PENDING", newToken.getStatus());
        assertNotEquals(firstTokenId, newToken.getId());

        // Send 2nd and 3rd resends (reaching capacity limit of 3)
        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk());

        // 4th Resend: throttled with 429 Too Many Requests
        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(status().reason(containsString("Too many resend requests")));
    }

    // -------------------------------------------------------------------------
    // 7. Email Templates, Token Redaction and Mail Transport
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.2: EmailDeliveryService constructs escaped HTML, URL fragment links, and handles transport safely")
    void testEmailDeliveryServiceFormattingAndTransport() {
        // Under default test properties (spring.mail.host empty), delivery returns SKIPPED_NO_TRANSPORT safely
        EmailDeliveryResult result = emailDeliveryService.sendOrganizationVerificationEmail(
                "org-admin@company.test",
                "Test Corp",
                "secret-verification-token-12345"
        );
        assertEquals("SKIPPED_NO_TRANSPORT", result.getDeliveryStatus());
        assertFalse(result.isDelivered());
        assertTrue(result.isAccepted());

        // When recipient is empty, throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            emailDeliveryService.sendOrganizationVerificationEmail(
                    "",
                    "Corp",
                    "token"
            );
        });
    }

    // -------------------------------------------------------------------------
    // 8. Invitation Delivery and Fragment URL Hardening
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.3: Employee invitations generate URL fragment links and hide raw tokens in responses")
    void testEmployeeInvitationFragmentUrl() throws Exception {
        // Setup active org and admin
        Organization org = organizationRepository.save(new Organization("Fragment Corp", "fragment-corp", "ACTIVE", Instant.now()));

        User admin = userRepository.save(new User("adminfrag", passwordEncoder.encode("Password123!"), "ROLE_ADMIN"));
        OrganizationMembership mem = new OrganizationMembership(org, admin, "ORG_ADMIN", "ACTIVE", Instant.now());
        membershipRepository.save(mem);

        String adminToken = jwtUtil.generateToken("adminfrag", "ROLE_ADMIN", org.getId(), org.getSlug());

        InvitationRequest invReq = new InvitationRequest();
        invReq.setEmployeeId("EMP-FRAG-001");
        invReq.setEmail("newhire@fragment.test");
        invReq.setFullName("New Hire");
        invReq.setRole("EMPLOYEE");

        // Send invitation
        mockMvc.perform(post("/invitations")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newhire@fragment.test")))
                .andExpect(jsonPath("$.status", is("PENDING")));

        // Verify stored invitation has hashed token, not raw token
        List<EmployeeInvitation> invitations = invitationRepository.findByOrganizationId(org.getId());
        assertEquals(1, invitations.size());
        EmployeeInvitation inv = invitations.get(0);
        assertNotNull(inv.getTokenHash());
        assertFalse(inv.getTokenHash().contains(" "));
    }

    // -------------------------------------------------------------------------
    // 9. Legacy Active Organizations and Continuity Behavior
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("D4.2.1: Pre-existing ACTIVE organizations continue normal operation without verification requirement")
    void testLegacyActiveOrganizationContinuity() throws Exception {
        Organization legacyOrg = organizationRepository.save(new Organization("Legacy Active Enterprise", "legacy-active", "ACTIVE", Instant.now()));

        User legacyUser = userRepository.save(new User("legacyuser", passwordEncoder.encode("Password123!"), "ROLE_EMPLOYEE"));
        OrganizationMembership mem = new OrganizationMembership(legacyOrg, legacyUser, "EMPLOYEE", "ACTIVE", Instant.now());
        membershipRepository.save(mem);

        // Login succeeds directly
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "legacyuser",
                        "password", "Password123!"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.organization.slug", is("legacy-active")));
    }
}
