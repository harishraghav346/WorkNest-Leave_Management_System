package com.examly.springapp;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.RateLimiterService;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkNestPhaseD4OnboardingAndAuthTests {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrganizationVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        leaveRepository.deleteAll();
        invitationRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        employeeProfileRepository.deleteAll();
        departmentRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        rateLimiterService.clear();
    }

    @Test
    @DisplayName("D4.3.1: Successful self-service organization onboarding creates org, admin, membership, and profile")
    void testSuccessfulOrganizationOnboarding() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Acme Global Inc",
                "acme-global",
                "acme_admin",
                "admin@acmeglobal.com",
                "Alice Acme",
                "SecurePassword123!"
        );

        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresEmailVerification").value(true))
                .andExpect(jsonPath("$.username").value("acme_admin"))
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"))
                .andExpect(jsonPath("$.organization.slug").value("acme-global"))
                .andExpect(jsonPath("$.requiresTenantSelection").value(false));

        // Verify entities exist in database in PENDING_VERIFICATION state before email verification
        assertTrue(organizationRepository.existsBySlug("acme-global"));
        Organization org = organizationRepository.findBySlug("acme-global").orElseThrow();
        assertEquals("PENDING_VERIFICATION", org.getStatus());

        User user = userRepository.findByUsername("acme_admin");
        assertNotNull(user);
        assertTrue(passwordEncoder.matches("SecurePassword123!", user.getPassword()));

        List<OrganizationMembership> pendingMemberships = membershipRepository.findByUserIdAndStatus(user.getId(), "PENDING_VERIFICATION");
        assertEquals(1, pendingMemberships.size());
        assertEquals("ORG_ADMIN", pendingMemberships.get(0).getRole());
        assertEquals("acme-global", pendingMemberships.get(0).getOrganization().getSlug());

        List<EmployeeProfile> profiles = employeeProfileRepository.findByOrganizationId(pendingMemberships.get(0).getOrganization().getId());
        assertEquals(1, profiles.size());
        assertEquals("admin@acmeglobal.com", profiles.get(0).getWorkEmail());
        assertEquals("Alice Acme", profiles.get(0).getFullName());

        // Verify verification token record was generated
        OrganizationVerificationToken verifToken = verificationTokenRepository
                .findTopByOrganizationIdOrderByCreatedAtDesc(org.getId()).orElseThrow();
        assertEquals("PENDING", verifToken.getStatus());
        assertNotNull(verifToken.getTokenHash());
    }

    @Test
    @DisplayName("D4.3.2: Organization onboarding automatically generates valid slug if none provided")
    void testOnboardingAutoGeneratesSlug() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Globex Logistics Corporation",
                null,
                "globex_admin",
                "admin@globex.com",
                "Bob Globex",
                "SecurePassword123!"
        );

        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organization.slug").value("globex-logistics-corporation"));

        assertTrue(organizationRepository.existsBySlug("globex-logistics-corporation"));
    }

    @Test
    @DisplayName("D4.3.3: Organization onboarding rejects duplicate organization slug with 409 Conflict")
    void testOnboardingRejectsDuplicateSlug() throws Exception {
        Organization existing = new Organization("Existing Org", "duplicate-slug", "ACTIVE", Instant.now());
        organizationRepository.save(existing);

        OnboardingRequest req = new OnboardingRequest(
                "Another Org",
                "duplicate-slug",
                "unique_user",
                "admin@other.com",
                "Charlie Admin",
                "SecurePassword123!"
        );

        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("D4.3.4: Organization onboarding rejects duplicate admin username with 400 Bad Request")
    void testOnboardingRejectsDuplicateAdminUsername() throws Exception {
        User existingUser = new User("taken_user", passwordEncoder.encode("Password123!"), "employee");
        userRepository.save(existingUser);

        OnboardingRequest req = new OnboardingRequest(
                "Brand New Org",
                "brand-new",
                "taken_user",
                "admin@neworg.com",
                "David New",
                "SecurePassword123!"
        );

        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertFalse(organizationRepository.existsBySlug("brand-new"));
    }

    @Test
    @DisplayName("D4.3.5: Organization onboarding validates input fields strictly")
    void testOnboardingValidatesInputFields() throws Exception {
        // Short password
        OnboardingRequest reqShortPass = new OnboardingRequest(
                "Short Pass Org",
                "short-pass",
                "short_admin",
                "admin@short.com",
                "Short Pass",
                "short"
        );
        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqShortPass)))
                .andExpect(status().isBadRequest());

        // Invalid email
        OnboardingRequest reqInvalidEmail = new OnboardingRequest(
                "Bad Email Org",
                "bad-email",
                "email_admin",
                "notanemail",
                "Bad Email",
                "SecurePassword123!"
        );
        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalidEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("D4.3.6: Organization onboarding rate limiting blocks excessive requests from same IP")
    void testOnboardingRateLimiting() throws Exception {
        for (int i = 1; i <= 5; i++) {
            OnboardingRequest req = new OnboardingRequest(
                    "Org " + i,
                    "org-rate-" + i,
                    "admin_" + i,
                    "admin" + i + "@test.com",
                    "Admin " + i,
                    "SecurePassword123!"
            );
            mockMvc.perform(post("/auth/onboard-organization")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 6th request from same IP should be blocked with 429
        OnboardingRequest req6 = new OnboardingRequest(
                "Org 6",
                "org-rate-6",
                "admin_6",
                "admin6@test.com",
                "Admin 6",
                "SecurePassword123!"
        );
        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req6)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("D4.3.7: Onboarded Org Admin is strictly prohibited from accessing Platform Admin endpoints")
    void testOnboardedOrgAdminCannotAccessPlatformAdminEndpoints() throws Exception {
        OnboardingRequest req = new OnboardingRequest(
                "Apex Security",
                "apex-sec",
                "apex_admin",
                "admin@apex.com",
                "Apex Administrator",
                "SecurePassword123!"
        );

        mockMvc.perform(post("/auth/onboard-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresEmailVerification").value(true));

        Organization org = organizationRepository.findBySlug("apex-sec").orElseThrow();
        org.setStatus("ACTIVE");
        organizationRepository.save(org);

        User user = userRepository.findByUsername("apex_admin");
        OrganizationMembership mem = membershipRepository.findByOrganizationIdAndUserId(org.getId(), user.getId()).orElseThrow();
        mem.setStatus("ACTIVE");
        membershipRepository.save(mem);

        String token = jwtUtil.generateToken("apex_admin", "ORG_ADMIN", org.getId(), org.getSlug());

        // Attempt to call platform admin endpoint /admin/users
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D4.2.1: User with single active membership auto-selects tenant on login")
    void testSingleMembershipLoginAutoSelectsTenant() throws Exception {
        Organization org = organizationRepository.save(new Organization("Single Org", "single-org", "ACTIVE", Instant.now()));
        User user = userRepository.save(new User("single_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org, user, "EMPLOYEE", "ACTIVE", Instant.now()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "single_user",
                                "password", "Pass1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.requiresTenantSelection").value(false))
                .andExpect(jsonPath("$.organization.slug").value("single-org"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("D4.2.2: User with multiple active memberships requires explicit tenant selection on login")
    void testMultipleMembershipsLoginRequiresTenantSelection() throws Exception {
        Organization org1 = organizationRepository.save(new Organization("Org One", "org-one", "ACTIVE", Instant.now()));
        Organization org2 = organizationRepository.save(new Organization("Org Two", "org-two", "ACTIVE", Instant.now()));

        User user = userRepository.save(new User("multi_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org1, user, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(org2, user, "MANAGER", "ACTIVE", Instant.now()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "multi_user",
                                "password", "Pass1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresTenantSelection").value(true))
                .andExpect(jsonPath("$.memberships", hasSize(2)));
    }

    @Test
    @DisplayName("D4.2.3: Ambiguous tenant access before tenant selection is blocked with 403 Forbidden")
    void testAmbiguousTenantAccessBlockedBeforeSelection() throws Exception {
        Organization org1 = organizationRepository.save(new Organization("Org One", "org-one", "ACTIVE", Instant.now()));
        Organization org2 = organizationRepository.save(new Organization("Org Two", "org-two", "ACTIVE", Instant.now()));

        User user = userRepository.save(new User("ambiguous_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org1, user, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(org2, user, "MANAGER", "ACTIVE", Instant.now()));

        // Token without organization scope
        String ambiguousToken = jwtUtil.generateToken(user.getUsername(), user.getRole());

        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + ambiguousToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D4.2.4: Explicit tenant selection issues tenant-scoped token and grants tenant access")
    void testExplicitTenantSelectionSucceeds() throws Exception {
        Organization org1 = organizationRepository.save(new Organization("Org Alpha", "org-alpha", "ACTIVE", Instant.now()));
        Organization org2 = organizationRepository.save(new Organization("Org Beta", "org-beta", "ACTIVE", Instant.now()));

        User user = userRepository.save(new User("selector_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org1, user, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(org2, user, "MANAGER", "ACTIVE", Instant.now()));

        String baseToken = jwtUtil.generateToken(user.getUsername(), user.getRole());

        // Select Org Alpha
        TenantSelectionRequest req = new TenantSelectionRequest(org1.getId(), null);
        String responseBody = mockMvc.perform(post("/auth/select-tenant")
                        .header("Authorization", "Bearer " + baseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.slug").value("org-alpha"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.requiresTenantSelection").value(false))
                .andReturn().getResponse().getContentAsString();

        AuthResponse alphaAuth = objectMapper.readValue(responseBody, AuthResponse.class);

        // Access tenant endpoint with selected token succeeds
        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + alphaAuth.getToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("D4.2.5: Tenant selection rejects unassigned organization with 403 Forbidden")
    void testTenantSelectionRejectsUnassignedOrganization() throws Exception {
        Organization org1 = organizationRepository.save(new Organization("My Org", "my-org", "ACTIVE", Instant.now()));
        Organization unassignedOrg = organizationRepository.save(new Organization("Foreign Org", "foreign-org", "ACTIVE", Instant.now()));

        User user = userRepository.save(new User("normal_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org1, user, "EMPLOYEE", "ACTIVE", Instant.now()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        TenantSelectionRequest req = new TenantSelectionRequest(unassignedOrg.getId(), null);
        mockMvc.perform(post("/auth/select-tenant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D4.2.6: Tenant selection rejects suspended organization with 403 Forbidden")
    void testTenantSelectionRejectsSuspendedOrganization() throws Exception {
        Organization suspendedOrg = organizationRepository.save(new Organization("Suspended Org", "suspended-org", "SUSPENDED", Instant.now()));
        User user = userRepository.save(new User("suspended_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(suspendedOrg, user, "EMPLOYEE", "ACTIVE", Instant.now()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        TenantSelectionRequest req = new TenantSelectionRequest(suspendedOrg.getId(), null);
        mockMvc.perform(post("/auth/select-tenant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D4.2.7: Tenant selection rejects deactivated membership with 403 Forbidden")
    void testTenantSelectionRejectsDeactivatedMembership() throws Exception {
        Organization org = organizationRepository.save(new Organization("Active Org", "active-org", "ACTIVE", Instant.now()));
        User user = userRepository.save(new User("deactivated_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org, user, "EMPLOYEE", "DEACTIVATED", Instant.now()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        TenantSelectionRequest req = new TenantSelectionRequest(org.getId(), null);
        mockMvc.perform(post("/auth/select-tenant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D4.2.8: GET /auth/memberships returns active memberships for user")
    void testGetMembershipsReturnsActiveOrganizations() throws Exception {
        Organization org1 = organizationRepository.save(new Organization("Org A", "org-a", "ACTIVE", Instant.now()));
        Organization org2 = organizationRepository.save(new Organization("Org B", "org-b", "ACTIVE", Instant.now()));
        Organization suspended = organizationRepository.save(new Organization("Org S", "org-s", "SUSPENDED", Instant.now()));

        User user = userRepository.save(new User("listing_user", passwordEncoder.encode("Pass1234!"), "employee"));
        membershipRepository.save(new OrganizationMembership(org1, user, "EMPLOYEE", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(org2, user, "ORG_ADMIN", "ACTIVE", Instant.now()));
        membershipRepository.save(new OrganizationMembership(suspended, user, "EMPLOYEE", "ACTIVE", Instant.now()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        mockMvc.perform(get("/auth/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Only the 2 active organizations
                .andExpect(jsonPath("$[0].organizationSlug").isNotEmpty())
                .andExpect(jsonPath("$[1].organizationSlug").isNotEmpty());
    }

    @Test
    @DisplayName("D4.2.9: Standalone user without memberships can log in cleanly")
    void testStandaloneUserLoginPreserved() throws Exception {
        userRepository.save(new User("legacy_user", passwordEncoder.encode("Pass1234!"), "employee"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "legacy_user",
                                "password", "Pass1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.requiresTenantSelection").value(false));
    }
}
