package com.examly.springapp.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEventDTO;
import com.examly.springapp.model.TeamCalendarLeaveDTO;
import com.examly.springapp.model.TenantContext;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.service.LeaveService;
import com.examly.springapp.service.TenantContextService;

@RestController
@RequestMapping("/leaves")
public class LeaveController {

  @Autowired
  private LeaveService leaveService;

  @Autowired
  private LeaveRepository leaveRepository;

  @Autowired
  private TenantContextService tenantContextService;

  @GetMapping
  public List<Leave> getAllLeave(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        if (!ctx.hasAdminOrHrPrivileges()) {
          return leaveService.getLeavesForUser(authentication.getName());
        }
        return leaveService.getAllLeave();
      } else {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        if (!isAdmin) {
          return leaveService.getLeavesForUser(authentication.getName());
        }
      }
    }
    return leaveService.getAllLeave();
  }

  @GetMapping("/calendar")
  public ResponseEntity<List<TeamCalendarLeaveDTO>> getTeamCalendar(
      @RequestParam String start,
      @RequestParam String end,
      @RequestParam(required = false) String status,
      Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to view calendar.");
    }

    boolean isAdmin = false;
    Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
    if (contextOpt.isPresent()) {
      isAdmin = contextOpt.get().hasAdminOrHrPrivileges();
    } else {
      isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    String currentUser = authentication.getName();
    List<TeamCalendarLeaveDTO> calendarLeaves = leaveService.getTeamCalendar(start, end, status, currentUser, isAdmin);
    return ResponseEntity.ok(calendarLeaves);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Leave> getLeaveById(@PathVariable Long id, Authentication authentication) {
    Leave leave = leaveService.getLeaveById(id);
    if (leave == null) {
      return ResponseEntity.notFound().build();
    }
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
      boolean isAdmin = contextOpt.map(TenantContext::hasAdminOrHrPrivileges)
          .orElseGet(() -> authentication.getAuthorities().stream()
              .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")));
      if (!isAdmin && leave.getEmployeeName() != null && !leave.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own leave records.");
      }
    }
    return ResponseEntity.ok(leave);
  }

  @PostMapping
  public Leave createLeave(@RequestBody Leave leave, Authentication authentication) {
    if (leave.getStatus() == null || leave.getStatus().isBlank()) {
      leave.setStatus("Pending");
    }
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        if (!ctx.hasAdminOrHrPrivileges()) {
          leave.setEmployeeName(authentication.getName());
        } else if (leave.getEmployeeName() == null || leave.getEmployeeName().isBlank()) {
          leave.setEmployeeName(authentication.getName());
        }
      } else {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        if (!isAdmin) {
          leave.setEmployeeName(authentication.getName());
        } else if (leave.getEmployeeName() == null || leave.getEmployeeName().isBlank()) {
          leave.setEmployeeName(authentication.getName());
        }
      }
    }
    return leaveService.createLeave(leave);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Leave> updateLeave(@PathVariable Long id, @RequestBody Leave leave, Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        Leave existing = leaveRepository.findById(id).orElse(null);
        if (existing == null || existing.getOrganization() == null || !existing.getOrganization().getId().equals(ctx.getOrganizationId())) {
          return ResponseEntity.notFound().build();
        }
        if (!ctx.hasAdminOrHrPrivileges()) {
          if (existing.getEmployeeName() != null && !existing.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot edit another employee's leave request.");
          }
          leave.setEmployeeName(authentication.getName());
        }
      } else {
        boolean isPlatformAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
        if (isPlatformAdmin) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot perform tenant HR operations");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        Leave existing = leaveRepository.findById(id).orElse(null);
        if (existing == null || existing.getOrganization() != null) {
          return ResponseEntity.notFound().build();
        }
        if (!isAdmin) {
          if (existing.getEmployeeName() != null && !existing.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot edit another employee's leave request.");
          }
          leave.setEmployeeName(authentication.getName());
        }
      }
    }
    Leave updatedLeave = leaveService.updateLeave(id, leave);
    if (updatedLeave == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(updatedLeave);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteLeave(@PathVariable Long id, Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        Leave existing = leaveRepository.findById(id).orElse(null);
        if (existing == null || existing.getOrganization() == null || !existing.getOrganization().getId().equals(ctx.getOrganizationId())) {
          return ResponseEntity.notFound().build();
        }
        if (!ctx.hasAdminOrHrPrivileges()) {
          if (existing.getEmployeeName() != null && !existing.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot delete another employee's leave request.");
          }
        }
      } else {
        boolean isPlatformAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
        if (isPlatformAdmin) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot perform tenant HR operations");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        Leave existing = leaveRepository.findById(id).orElse(null);
        if (existing == null || existing.getOrganization() != null) {
          return ResponseEntity.notFound().build();
        }
        if (!isAdmin) {
          if (existing.getEmployeeName() != null && !existing.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot delete another employee's leave request.");
          }
        }
      }
    }
    boolean isDeleted = leaveService.deleteLeave(id);
    if (isDeleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/page")
  public Page<Leave> getLeavesPaginated(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        if (!ctx.hasAdminOrHrPrivileges()) {
          return leaveService.getLeavesForUserPaginated(authentication.getName(), page, size);
        }
        return leaveService.getLeavesPaginated(page, size);
      } else {
        boolean isPlatformAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
        if (isPlatformAdmin) {
          return Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        if (!isAdmin) {
          return leaveService.getLeavesForUserPaginated(authentication.getName(), page, size);
        }
      }
    }
    return leaveService.getLeavesPaginated(page, size);
  }

  @PutMapping("/{id}/approve")
  public ResponseEntity<Leave> approveLeave(@PathVariable Long id, Authentication authentication) {
    String approver = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
    Leave updatedLeave = leaveService.approveLeave(id, approver);
    return ResponseEntity.ok(updatedLeave);
  }

  @PutMapping("/{id}/reject")
  public ResponseEntity<Leave> rejectLeave(
      @PathVariable Long id, 
      @RequestBody(required = false) Map<String, String> body,
      Authentication authentication) {
    String approver = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
    String rejectionReason = (body != null) ? body.get("rejectionReason") : null;
    Leave updatedLeave = leaveService.rejectLeave(id, rejectionReason, approver);
    return ResponseEntity.ok(updatedLeave);
  }

  @PutMapping("/{id}/cancel")
  public ResponseEntity<Leave> cancelLeave(@PathVariable Long id, Authentication authentication) {
    String currentUser = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
    Leave cancelledLeave = leaveService.cancelLeave(id, currentUser);
    return ResponseEntity.ok(cancelledLeave);
  }

  @GetMapping("/{id}/audit")
  public ResponseEntity<Page<LeaveAuditEventDTO>> getLeaveAuditHistory(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String eventType,
      Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to view audit logs.");
    }

    Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (!ctx.hasAdminOrHrPrivileges()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privileges required to view audit logs.");
      }
      Leave leave = leaveRepository.findById(id).orElse(null);
      if (leave == null || leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
        return ResponseEntity.notFound().build();
      }
    } else {
      Leave leave = leaveRepository.findById(id).orElse(null);
      if (leave == null || leave.getOrganization() != null) {
        return ResponseEntity.notFound().build();
      }
      boolean isPlatformAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
      if (isPlatformAdmin) {
        return ResponseEntity.notFound().build();
      }
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (!isAdmin) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privileges required to view audit logs.");
      }
    }

    int constrainedSize = Math.max(1, Math.min(size, 100));
    int constrainedPage = Math.max(0, page);
    Page<LeaveAuditEventDTO> auditTrail = leaveService.getAuditTrailForLeave(id, eventType, constrainedPage, constrainedSize);
    return ResponseEntity.ok(auditTrail);
  }
}
