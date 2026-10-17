package com.examly.springapp.controller;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.OrganizationRepository;
import com.examly.springapp.service.OnboardingService;
import com.examly.springapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private OnboardingService onboardingService;

    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesConfig;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()) != null) {
            return new ResponseEntity<>("Username is already taken", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Security hardening: Force employee role for all public registrations
        user.setRole("employee");
        if (user.getLeaveBalance() == null) {
            user.setLeaveBalance(20);
        }
        User savedUser = userService.save(user);
        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole());
        return new ResponseEntity<>(new AuthResponse(token, savedUser.getId(), savedUser.getUsername(), savedUser.getRole()), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        User existingUser = userService.findByUsername(user.getUsername());
        if (existingUser == null || !passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        List<OrganizationMembership> allMemberships = membershipRepository.findAllByUserId(existingUser.getId());
        List<OrganizationMembership> activeMemberships = membershipRepository.findByUserIdAndStatus(existingUser.getId(), "ACTIVE").stream()
                .filter(m -> m.getOrganization() != null && "ACTIVE".equalsIgnoreCase(m.getOrganization().getStatus()))
                .collect(Collectors.toList());

        boolean isExplicitPlatformAdmin = existingUser.getRole() != null && (
                existingUser.getRole().equalsIgnoreCase("PLATFORM_ADMIN") ||
                existingUser.getRole().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")
        );

        // If user has memberships recorded, but none are currently active (deactivated membership or suspended org)
        if (!allMemberships.isEmpty() && activeMemberships.isEmpty() && !isExplicitPlatformAdmin) {
            boolean isPendingVerif = allMemberships.stream()
                    .anyMatch(m -> (m.getOrganization() != null && "PENDING_VERIFICATION".equalsIgnoreCase(m.getOrganization().getStatus())) ||
                            "PENDING_VERIFICATION".equalsIgnoreCase(m.getStatus()));
            if (isPendingVerif) {
                return new ResponseEntity<>("Organization workspace is pending email verification. Please verify your email before signing in.", HttpStatus.FORBIDDEN);
            }
            return new ResponseEntity<>("Account has no active organization memberships. Your membership or organization may be inactive or suspended.", HttpStatus.FORBIDDEN);
        }

        // If user belongs to multiple active organizations, return tenant selection requirement
        if (activeMemberships.size() > 1) {
            String token = jwtUtil.generateToken(existingUser.getUsername(), existingUser.getRole());
            AuthResponse response = new AuthResponse(token, existingUser.getId(), existingUser.getUsername(), existingUser.getRole());
            response.setRequiresTenantSelection(true);
            response.setMemberships(activeMemberships.stream().map(OrganizationMembershipDTO::fromEntity).collect(Collectors.toList()));
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        // If user has exactly one active organization membership, auto-select it safely
        if (activeMemberships.size() == 1) {
            OrganizationMembership mem = activeMemberships.get(0);
            Organization org = mem.getOrganization();
            String token = jwtUtil.generateToken(existingUser.getUsername(), mem.getRole(), org.getId(), org.getSlug());
            AuthResponse response = new AuthResponse(token, existingUser.getId(), existingUser.getUsername(), mem.getRole(), org);
            response.setRequiresTenantSelection(false);
            response.setMemberships(List.of(OrganizationMembershipDTO.fromEntity(mem)));
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        // Standalone user without organizational memberships (e.g. platform admin or legacy test user)
        String token = jwtUtil.generateToken(existingUser.getUsername(), existingUser.getRole());
        AuthResponse response = new AuthResponse(token, existingUser.getId(), existingUser.getUsername(), existingUser.getRole());
        response.setRequiresTenantSelection(false);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/select-tenant")
    public ResponseEntity<?> selectTenant(@RequestBody TenantSelectionRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        User existingUser = userService.findByUsername(authentication.getName());
        if (existingUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        if (request == null || (request.getOrganizationId() == null && (request.getOrganizationSlug() == null || request.getOrganizationSlug().isBlank()))) {
            return new ResponseEntity<>("Organization ID or slug is required for tenant selection.", HttpStatus.BAD_REQUEST);
        }

        Long targetOrgId = request.getOrganizationId();
        if (targetOrgId == null && request.getOrganizationSlug() != null) {
            Optional<Organization> orgOpt = organizationRepository.findBySlug(request.getOrganizationSlug().trim().toLowerCase());
            if (orgOpt.isEmpty()) {
                return new ResponseEntity<>("Target organization not found.", HttpStatus.NOT_FOUND);
            }
            targetOrgId = orgOpt.get().getId();
        }

        Optional<OrganizationMembership> memOpt = membershipRepository.findByOrganizationIdAndUserId(targetOrgId, existingUser.getId());
        if (memOpt.isEmpty()) {
            return new ResponseEntity<>("User does not belong to the requested organization.", HttpStatus.FORBIDDEN);
        }

        OrganizationMembership membership = memOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(membership.getStatus())) {
            return new ResponseEntity<>("Organization membership is inactive or deactivated.", HttpStatus.FORBIDDEN);
        }

        Organization org = membership.getOrganization();
        if (org == null || !"ACTIVE".equalsIgnoreCase(org.getStatus())) {
            return new ResponseEntity<>("Target organization is inactive or suspended.", HttpStatus.FORBIDDEN);
        }

        String token = jwtUtil.generateToken(existingUser.getUsername(), membership.getRole(), org.getId(), org.getSlug());
        AuthResponse response = new AuthResponse(token, existingUser.getId(), existingUser.getUsername(), membership.getRole(), org);
        response.setRequiresTenantSelection(false);
        response.setMemberships(List.of(OrganizationMembershipDTO.fromEntity(membership)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/memberships")
    public ResponseEntity<?> getMemberships(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        User existingUser = userService.findByUsername(authentication.getName());
        if (existingUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        List<OrganizationMembership> memberships = membershipRepository.findByUserIdAndStatus(existingUser.getId(), "ACTIVE");
        List<OrganizationMembershipDTO> dtos = memberships.stream()
                .filter(m -> m.getOrganization() != null && "ACTIVE".equalsIgnoreCase(m.getOrganization().getStatus()))
                .map(OrganizationMembershipDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping({"/onboard-organization", "/onboard"})
    public ResponseEntity<?> onboardOrganization(@RequestBody OnboardingRequest request, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        AuthResponse response = onboardingService.onboardOrganization(request, clientIp);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping({"/verify-organization", "/verify", "/onboarding/verify"})
    public ResponseEntity<?> verifyOrganization(@RequestBody OrganizationVerificationRequest request, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        AuthResponse response = onboardingService.verifyOrganization(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerificationRequest request, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        onboardingService.resendVerification(request, clientIp);
        return ResponseEntity.ok(Map.of("message", "If an unverified organization exists for this email, a verification link has been sent."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        User existingUser = userService.findByUsername(authentication.getName());
        if (existingUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(existingUser, HttpStatus.OK);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }

        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            Set<String> trusted = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            if (trusted.contains(remoteAddr)) {
                String xf = request.getHeader("X-Forwarded-For");
                if (xf != null && !xf.isBlank()) {
                    return xf.split(",")[0].trim();
                }
            }
        }
        return remoteAddr;
    }
}