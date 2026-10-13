package com.examly.springapp.controller;

import java.util.List;
import java.util.Map;

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
import com.examly.springapp.service.LeaveService;

@RestController
@RequestMapping("/leaves")
public class LeaveController {

  @Autowired
  private LeaveService leaveService;

  @GetMapping
  public List<Leave> getAllLeave(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (!isAdmin) {
        return leaveService.getLeavesForUser(authentication.getName());
      }
    }
    return leaveService.getAllLeave();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Leave> getLeaveById(@PathVariable Long id, Authentication authentication) {
    Leave leave = leaveService.getLeaveById(id);
    if (leave == null) {
      return ResponseEntity.notFound().build();
    }
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
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
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (!isAdmin) {
        leave.setEmployeeName(authentication.getName());
      } else if (leave.getEmployeeName() == null || leave.getEmployeeName().isBlank()) {
        leave.setEmployeeName(authentication.getName());
      }
    }
    return leaveService.createLeave(leave);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Leave> updateLeave(@PathVariable Long id, @RequestBody Leave leave, Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (!isAdmin) {
        Leave existing = leaveService.getLeaveById(id);
        if (existing == null) {
          return ResponseEntity.notFound().build();
        }
        if (existing.getEmployeeName() != null && !existing.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot edit another employee's leave request.");
        }
        leave.setEmployeeName(authentication.getName());
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
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (!isAdmin) {
        Leave existing = leaveService.getLeaveById(id);
        if (existing == null) {
          return ResponseEntity.notFound().build();
        }
        if (existing.getEmployeeName() != null && !existing.getEmployeeName().equalsIgnoreCase(authentication.getName())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot delete another employee's leave request.");
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
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (!isAdmin) {
        return leaveService.getLeavesForUserPaginated(authentication.getName(), page, size);
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
    boolean isAdmin = authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    if (!isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privileges required to view audit logs.");
    }
    int constrainedSize = Math.max(1, Math.min(size, 100));
    int constrainedPage = Math.max(0, page);
    Page<LeaveAuditEventDTO> auditTrail = leaveService.getAuditTrailForLeave(id, eventType, constrainedPage, constrainedSize);
    return ResponseEntity.ok(auditTrail);
  }
}
