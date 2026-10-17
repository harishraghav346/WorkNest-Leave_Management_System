package com.examly.springapp.service;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
public class InvitationServiceImpl implements InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationServiceImpl.class);

    @Autowired
    private EmployeeInvitationRepository invitationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Value("${app.invitation.dev-token-delivery:false}")
    private boolean devTokenDelivery;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public EmployeeInvitation createInvitation(InvitationRequest request, Authentication authentication, String clientIp) {
        if (clientIp != null && !rateLimiterService.isAllowed("invite:" + clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please wait before sending more invitations.");
        }

        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        if (!ctx.hasAdminOrHrPrivileges()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Only organization administrators and HR administrators can invite employees.");
        }

        if (request.getEmployeeId() == null || request.getEmployeeId().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee ID is required.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company email is required.");
        }

        String empId = request.getEmployeeId().trim();
        String email = request.getEmail().trim().toLowerCase();
        String requestedRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "EMPLOYEE";

        // Role escalation defense: cannot invite platform admin
        if (requestedRole.contains("PLATFORM") || requestedRole.equals("ADMIN") ||
                requestedRole.equals("ROLE_ADMIN") || requestedRole.equals("ROLE_PLATFORM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role escalation rejected: Platform administrator invitations cannot be created.");
        }

        // HR Admin cannot invite ORG_ADMIN
        if (ctx.isHrAdmin() && !ctx.isOrgAdmin() && "ORG_ADMIN".equalsIgnoreCase(requestedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role escalation rejected: HR administrators cannot invite organization administrators.");
        }

        // Prevent duplicate employee IDs or emails within the same organization
        if (employeeProfileRepository.existsByOrganizationIdAndEmployeeId(ctx.getOrganizationId(), empId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Employee ID '" + empId + "' is already registered in this organization.");
        }
        if (employeeProfileRepository.existsByOrganizationIdAndWorkEmail(ctx.getOrganizationId(), email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Company email '" + email + "' is already registered in this organization.");
        }

        // Prevent duplicate pending invitations within the same organization
        if (invitationRepository.existsByOrganizationIdAndEmployeeIdAndStatus(ctx.getOrganizationId(), empId, "PENDING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A pending invitation already exists for employee ID '" + empId + "' in this organization.");
        }
        if (invitationRepository.existsByOrganizationIdAndEmailIgnoreCaseAndStatus(ctx.getOrganizationId(), email, "PENDING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A pending invitation already exists for email '" + email + "' in this organization.");
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findByIdAndOrganizationId(request.getDepartmentId(), ctx.getOrganizationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Department ID #" + request.getDepartmentId() + " not found or belongs to another organization."));
        }

        Organization org = organizationRepository.findById(ctx.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Organization record not found."));

        User invitedBy = userRepository.findByUsername(ctx.getUsername());

        // Generate cryptographically secure one-time activation token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = HexFormat.of().formatHex(tokenBytes);

        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

        EmployeeInvitation invitation = new EmployeeInvitation(
                org,
                empId,
                email,
                request.getFullName(),
                requestedRole,
                department,
                tokenHash,
                expiresAt,
                invitedBy
        );

        EmployeeInvitation saved = invitationRepository.save(invitation);

        // Secure invitation token delivery: expose raw token only if devTokenDelivery is explicitly enabled
        if (devTokenDelivery) {
            saved.setRawToken(rawToken);
            saved.setActivationLink(sanitizeBaseUrl(baseUrl) + "/activate#token=" + rawToken);
        }

        // Deliver transactional invitation email via secure transport
        emailDeliveryService.sendEmployeeInvitationEmail(email, org.getName(), requestedRole, rawToken);

        logger.info("Created invitation for email {} with role {} in org ID {}", email, requestedRole, ctx.getOrganizationId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeInvitation> getInvitations(Authentication authentication) {
        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        if (!ctx.hasAdminOrHrPrivileges()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Only organization administrators and HR administrators can view invitations.");
        }
        return invitationRepository.findByOrganizationId(ctx.getOrganizationId());
    }

    @Override
    public AuthResponse acceptInvitation(ActivationRequest request, String clientIp) {
        if (clientIp != null && !rateLimiterService.isAllowed("activate:" + clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please wait before retrying activation.");
        }

        if (request.getToken() == null || request.getToken().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token is required.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long.");
        }

        String token = request.getToken().trim();
        String tokenHash = hashToken(token);

        EmployeeInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unknown activation token."));

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token has already been used or revoked.");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token has expired.");
        }

        // M-2: Make email verification mandatory during activation
        if (request.getEmail() == null || request.getEmail().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company email is required for activation.");
        }
        String providedEmail = request.getEmail().trim();
        if (!invitation.getEmail().equalsIgnoreCase(providedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address does not match invitation record.");
        }

        Organization org = invitation.getOrganization();
        if (org == null || !"ACTIVE".equalsIgnoreCase(org.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Target organization is inactive or suspended.");
        }

        String username = request.getUsername().trim();
        if (userRepository.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username '" + username + "' is already taken.");
        }

        // L-1: Atomic conditional update to prevent concurrent activation race conditions
        Instant acceptedAt = Instant.now();
        int updatedRows = invitationRepository.markAcceptedIfPending(invitation.getId(), acceptedAt);
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token has already been used, expired, or revoked.");
        }

        // Transactional account creation
        User newUser = new User(username, passwordEncoder.encode(request.getPassword()), "employee");
        newUser.setLeaveBalance(20);
        User savedUser = userRepository.save(newUser);

        OrganizationMembership membership = new OrganizationMembership(
                org,
                savedUser,
                invitation.getRole(),
                "ACTIVE",
                Instant.now()
        );
        OrganizationMembership savedMembership = membershipRepository.save(membership);

        EmployeeProfile profile = new EmployeeProfile(
                savedMembership,
                org,
                invitation.getEmployeeId(),
                invitation.getFullName() != null ? invitation.getFullName() : username,
                invitation.getEmail(),
                invitation.getDepartment() != null ? invitation.getDepartment().getName() : null,
                invitation.getRole(),
                java.time.LocalDate.now(),
                "FULL_TIME"
        );
        if (invitation.getDepartment() != null) {
            profile.setDepartmentRef(invitation.getDepartment());
        }
        employeeProfileRepository.save(profile);

        logger.info("Successfully activated account for user {} in organization {}", username, org.getSlug());

        String jwtToken = jwtUtil.generateToken(savedUser.getUsername(), invitation.getRole());
        return new AuthResponse(jwtToken, savedUser.getId(), savedUser.getUsername(), invitation.getRole());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String sanitizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.trim().replaceAll("/+$", "");
    }
}
