package com.examly.springapp.service;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class OnboardingServiceImpl implements OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingServiceImpl.class);

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private OrganizationVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Value("${app.invitation.dev-token-delivery:false}")
    private boolean devTokenDelivery;

    @Value("${app.onboarding.email-verification-required:true}")
    private boolean emailVerificationRequired;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AuthResponse onboardOrganization(OnboardingRequest request, String clientIp) {
        // 1. Rate limiting
        if (clientIp != null && !rateLimiterService.isAllowed("onboard:" + clientIp, 5)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many onboarding requests from this IP. Please wait a minute before trying again.");
        }

        // 2. Input validation
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Onboarding request body is required.");
        }

        String orgName = request.getOrganizationName();
        if (orgName == null || orgName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required.");
        }
        orgName = orgName.trim();
        if (orgName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name cannot exceed 100 characters.");
        }

        // Slug derivation & validation
        String slug = request.getOrganizationSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = orgName.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
        } else {
            slug = slug.trim().toLowerCase();
        }

        if (slug.length() < 3 || slug.length() > 50 || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Organization slug must be 3-50 lowercase alphanumeric characters and hyphens, without leading/trailing hyphens.");
        }

        if (organizationRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Organization slug '" + slug + "' is already in use.");
        }

        // Admin credentials validation
        String username = request.getAdminUsername();
        if (username == null || username.trim().isEmpty() || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admin username must be 3-50 alphanumeric characters or underscores.");
        }
        username = username.trim();

        if (userRepository.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username '" + username + "' is already taken.");
        }

        String email = request.getAdminEmail();
        if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid admin email address is required.");
        }
        email = email.trim();

        String fullName = request.getAdminFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin full name is required.");
        }
        fullName = fullName.trim();
        if (fullName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin full name cannot exceed 100 characters.");
        }

        String password = request.getAdminPassword();
        if (password == null || password.trim().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admin password must be at least 8 characters long.");
        }

        // 3. Atomic creation
        String initialStatus = emailVerificationRequired ? "PENDING_VERIFICATION" : "ACTIVE";

        // Organization
        Organization organization = new Organization(orgName, slug, initialStatus, Instant.now());
        organization = organizationRepository.save(organization);

        // Initial Organization Admin user
        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setPassword(passwordEncoder.encode(password.trim()));
        adminUser.setRole("admin");
        adminUser.setLeaveBalance(20);
        adminUser = userRepository.save(adminUser);

        // Membership
        OrganizationMembership membership = new OrganizationMembership(
                organization,
                adminUser,
                "ORG_ADMIN",
                initialStatus,
                Instant.now()
        );
        membership = membershipRepository.save(membership);

        // Employee Profile
        EmployeeProfile profile = new EmployeeProfile(
                membership,
                organization,
                "EMP-001",
                fullName,
                email,
                "Executive",
                "Organization Administrator",
                LocalDate.now(),
                "FULL_TIME"
        );
        employeeProfileRepository.save(profile);

        // 4. Token generation and email dispatch when verification is required
        if (emailVerificationRequired) {
            byte[] tokenBytes = new byte[32];
            secureRandom.nextBytes(tokenBytes);
            String rawToken = HexFormat.of().formatHex(tokenBytes);

            String tokenHash = hashToken(rawToken);
            Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            OrganizationVerificationToken verificationToken = new OrganizationVerificationToken(
                    organization,
                    adminUser,
                    email,
                    tokenHash,
                    expiresAt
            );
            verificationTokenRepository.save(verificationToken);

            // Send verification email
            emailDeliveryService.sendOrganizationVerificationEmail(email, orgName, rawToken);

            // Return response without active tenant token
            AuthResponse response = new AuthResponse();
            response.setId(adminUser.getId());
            response.setUsername(adminUser.getUsername());
            response.setRole("ORG_ADMIN");
            response.setOrganization(organization);
            response.setRequiresEmailVerification(true);
            response.setRequiresTenantSelection(false);
            response.setMessage("Organization registered successfully. Please verify your email before accessing your workspace.");
            response.setMemberships(List.of(OrganizationMembershipDTO.fromEntity(membership)));

            if (devTokenDelivery) {
                response.setRawToken(rawToken);
                response.setVerificationLink(sanitizeBaseUrl(baseUrl) + "/verify-organization#token=" + rawToken);
            }

            logger.info("Onboarded organization '{}' ({}) in PENDING_VERIFICATION state. Verification email dispatched to '{}'.",
                    orgName, slug, EmailDeliveryServiceImpl.redactEmail(email));
            return response;
        }

        // Legacy/Direct active path (when verification is disabled)
        String token = jwtUtil.generateToken(adminUser.getUsername(), "ORG_ADMIN", organization.getId(), organization.getSlug());
        AuthResponse response = new AuthResponse(token, adminUser.getId(), adminUser.getUsername(), "ORG_ADMIN", organization);
        response.setRequiresTenantSelection(false);
        response.setRequiresEmailVerification(false);
        response.setMemberships(List.of(OrganizationMembershipDTO.fromEntity(membership)));
        return response;
    }

    @Override
    public AuthResponse verifyOrganization(OrganizationVerificationRequest request, String clientIp) {
        if (clientIp != null && !rateLimiterService.isAllowed("verify:" + clientIp, 15)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification requests. Please wait.");
        }

        if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required.");
        }

        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        OrganizationVerificationToken tokenRecord = verificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification token."));

        if (!"PENDING".equalsIgnoreCase(tokenRecord.getStatus()) || tokenRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token has expired, been revoked, or already redeemed.");
        }

        // Validate email binding if email was supplied
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (!tokenRecord.getAdminEmail().equalsIgnoreCase(request.getEmail().trim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification email does not match invitation record.");
            }
        }

        // Concurrency-safe atomic check-and-set redemption
        int updated = verificationTokenRepository.updateStatusIfPending(tokenRecord.getId(), Instant.now());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token has already been redeemed.");
        }

        // Activate organization
        Organization organization = tokenRecord.getOrganization();
        organization.setStatus("ACTIVE");
        organization = organizationRepository.save(organization);

        // Activate membership
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserId(organization.getId(), tokenRecord.getAdminUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Membership record missing for verified organization."));
        membership.setStatus("ACTIVE");
        membership = membershipRepository.save(membership);

        // Issue tenant-scoped JWT now that verification has completed
        String token = jwtUtil.generateToken(
                tokenRecord.getAdminUser().getUsername(),
                "ORG_ADMIN",
                organization.getId(),
                organization.getSlug()
        );

        AuthResponse response = new AuthResponse(token, tokenRecord.getAdminUser().getId(), tokenRecord.getAdminUser().getUsername(), "ORG_ADMIN", organization);
        response.setRequiresTenantSelection(false);
        response.setRequiresEmailVerification(false);
        response.setMessage("Organization successfully verified and activated. Welcome to WorkNest!");
        response.setMemberships(List.of(OrganizationMembershipDTO.fromEntity(membership)));

        logger.info("Successfully verified and activated organization '{}' (slug: {}) via token ID #{}",
                organization.getName(), organization.getSlug(), tokenRecord.getId());
        return response;
    }

    @Override
    public void resendVerification(ResendVerificationRequest request, String clientIp) {
        if (clientIp != null && !rateLimiterService.isAllowed("resend:" + clientIp, 3)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many resend requests. Please wait a few minutes.");
        }

        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email address is required.");
        }

        String email = request.getEmail().trim().toLowerCase();

        // Rate limit per email as well to prevent spam flooding to a specific victim
        if (!rateLimiterService.isAllowed("resend-email:" + email, 3)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many resend requests for this email. Please wait.");
        }

        // Look up latest token for email
        Optional<OrganizationVerificationToken> latestOpt = verificationTokenRepository
                .findTopByAdminEmailIgnoreCaseOrderByCreatedAtDesc(email);

        if (latestOpt.isPresent()) {
            OrganizationVerificationToken latest = latestOpt.get();
            Organization org = latest.getOrganization();

            if (org != null && "PENDING_VERIFICATION".equalsIgnoreCase(org.getStatus())) {
                // Revoke prior pending tokens for this organization
                List<OrganizationVerificationToken> priorTokens = verificationTokenRepository
                        .findAllByOrganizationIdAndStatus(org.getId(), "PENDING");
                for (OrganizationVerificationToken pt : priorTokens) {
                    pt.setStatus("REVOKED");
                    verificationTokenRepository.save(pt);
                }

                // Generate new token
                byte[] tokenBytes = new byte[32];
                secureRandom.nextBytes(tokenBytes);
                String newRawToken = HexFormat.of().formatHex(tokenBytes);
                String newTokenHash = hashToken(newRawToken);

                OrganizationVerificationToken newToken = new OrganizationVerificationToken(
                        org,
                        latest.getAdminUser(),
                        email,
                        newTokenHash,
                        Instant.now().plus(24, ChronoUnit.HOURS)
                );
                verificationTokenRepository.save(newToken);

                emailDeliveryService.sendOrganizationVerificationEmail(email, org.getName(), newRawToken);
                logger.info("Resent organization verification email to '{}' for organization '{}'",
                        EmailDeliveryServiceImpl.redactEmail(email), org.getName());
            }
        }
        // Always return 200 OK without disclosing whether an unverified organization exists
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private String sanitizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.trim().replaceAll("/+$", "");
    }

    // Setters for tests
    public void setEmailVerificationRequired(boolean emailVerificationRequired) {
        this.emailVerificationRequired = emailVerificationRequired;
    }

    public void setDevTokenDelivery(boolean devTokenDelivery) {
        this.devTokenDelivery = devTokenDelivery;
    }
}
