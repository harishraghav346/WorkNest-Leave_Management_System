package com.examly.springapp.controller;

import com.examly.springapp.model.AiChatRequest;
import com.examly.springapp.model.AiChatResponse;
import com.examly.springapp.model.AiImpactResponse;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.TenantContext;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.service.AiService;
import com.examly.springapp.service.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private TenantContextService tenantContextService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chatWithAssistant(@RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiService.processChat(request));
    }

    @GetMapping("/impact/{leaveId}")
    public ResponseEntity<AiImpactResponse> getImpactAnalysis(@PathVariable Long leaveId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
        Leave leave = leaveRepository.findById(leaveId).orElse(null);
        if (leave == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
        }
        if (contextOpt.isPresent()) {
            TenantContext ctx = contextOpt.get();
            if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
            }
        } else {
            if (leave.getOrganization() != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
            }
            boolean isPlatformAdmin = authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
            if (isPlatformAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot access tenant AI data");
            }
        }
        return ResponseEntity.ok(aiService.analyzeImpact(leaveId));
    }

    @GetMapping("/ooo/{leaveId}")
    public ResponseEntity<Map<String, String>> getOutOfOfficeTemplate(@PathVariable Long leaveId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
        Leave leave = leaveRepository.findById(leaveId).orElse(null);
        if (leave == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
        }
        if (contextOpt.isPresent()) {
            TenantContext ctx = contextOpt.get();
            if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
            }
        } else {
            if (leave.getOrganization() != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found");
            }
            boolean isPlatformAdmin = authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
            if (isPlatformAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot access tenant AI data");
            }
        }
        String draft = aiService.generateOutOfOffice(leaveId);
        return ResponseEntity.ok(Map.of("template", draft));
    }
}
