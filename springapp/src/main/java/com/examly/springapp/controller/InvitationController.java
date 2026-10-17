package com.examly.springapp.controller;

import com.examly.springapp.model.ActivationRequest;
import com.examly.springapp.model.AuthResponse;
import com.examly.springapp.model.EmployeeInvitation;
import com.examly.springapp.model.InvitationRequest;
import com.examly.springapp.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesConfig;

    @PostMapping("/invitations")
    public ResponseEntity<EmployeeInvitation> createInvitation(
            @RequestBody InvitationRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        EmployeeInvitation invitation = invitationService.createInvitation(request, authentication, clientIp);
        return new ResponseEntity<>(invitation, HttpStatus.CREATED);
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<EmployeeInvitation>> getInvitations(Authentication authentication) {
        List<EmployeeInvitation> list = invitationService.getInvitations(authentication);
        return ResponseEntity.ok(list);
    }

    @PostMapping({"/invitations/activate", "/auth/invitations/accept"})
    public ResponseEntity<AuthResponse> acceptInvitation(
            @RequestBody ActivationRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        AuthResponse response = invitationService.acceptInvitation(request, clientIp);
        return ResponseEntity.ok(response);
    }

    public String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }

        // Only parse forwarded client headers if the connecting address is an explicitly trusted proxy
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
