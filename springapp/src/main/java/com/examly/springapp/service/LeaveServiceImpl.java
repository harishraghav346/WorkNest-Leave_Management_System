package com.examly.springapp.service;

import com.examly.springapp.model.Department;
import com.examly.springapp.model.EmployeeProfile;
import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.LeaveAuditEventDTO;
import com.examly.springapp.model.Organization;
import com.examly.springapp.model.TeamCalendarLeaveDTO;
import com.examly.springapp.model.TenantContext;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.DepartmentRepository;
import com.examly.springapp.repository.EmployeeProfileRepository;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.LeaveRepository;
import com.examly.springapp.repository.OrganizationRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

  @Autowired
  private LeaveRepository leaveRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LeaveAuditEventRepository leaveAuditEventRepository;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private TenantContextService tenantContextService;

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private DepartmentRepository departmentRepository;

  @Autowired
  private EmployeeProfileRepository employeeProfileRepository;

  @Override
  public Leave createLeave(Leave leave) {
    if (leave.getStatus() == null || leave.getStatus().isBlank()) {
      leave.setStatus("Pending");
    }
    if (leave.getCreatedAt() == null || leave.getCreatedAt().isBlank()) {
      leave.setCreatedAt(java.time.LocalDateTime.now().toString());
    }
    int calculatedDays = calculateDays(leave.getStartDate(), leave.getEndDate(), leave.getDaysCount());
    if (calculatedDays <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid leave request: Selected date range contains 0 working days (weekends are excluded).");
    }
    leave.setDaysCount(calculatedDays);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    Long orgId = null;
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      orgId = ctx.getOrganizationId();
      Organization org = organizationRepository.findById(orgId).orElse(null);
      leave.setOrganization(org);

      // Department scoping: resolve from employee profile first
      Optional<EmployeeProfile> profileOpt = employeeProfileRepository
          .findByOrganizationIdAndMembershipUserUsername(orgId, leave.getEmployeeName());
      if (profileOpt.isPresent() && profileOpt.get().getDepartmentRef() != null) {
        leave.setDepartment(profileOpt.get().getDepartmentRef());
      } else if (leave.getDepartment() != null && leave.getDepartment().getId() != null) {
        // Client supplied department ID - verify it belongs to this tenant organization
        Department clientDept = departmentRepository.findByIdAndOrganizationId(leave.getDepartment().getId(), orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department does not belong to this organization."));
        leave.setDepartment(clientDept);
      }
    }

    Leave newLeave = leaveRepository.save(leave);
    notificationService.createNotification(orgId, newLeave.getEmployeeName(), "New leave request submitted by " + leave.getEmployeeName());

    // Record persistent append-only audit event in the same transaction
    String actor = resolveActor(newLeave.getEmployeeName());
    String role = resolveActorRole(actor);
    Integer userBalance = null;
    if (newLeave.getEmployeeName() != null) {
      User u = userRepository.findByUsername(newLeave.getEmployeeName());
      if (u != null) {
        userBalance = u.getLeaveBalance();
      }
    }
    recordAuditEvent(orgId, newLeave.getId(), "CREATED", actor, role, null, newLeave.getStatus(), userBalance, userBalance,
        "Leave request created for " + newLeave.getDaysCount() + " day(s) (" + newLeave.getLeaveType() + ")");

    return newLeave;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Leave> getAllLeave() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (ctx.isManager()) {
        List<Department> managedDepts = departmentRepository.findByOrganizationIdAndManagerMembershipId(ctx.getOrganizationId(), ctx.getMembershipId());
        if (!managedDepts.isEmpty()) {
          List<Long> deptIds = managedDepts.stream().map(Department::getId).toList();
          return leaveRepository.findByOrganizationIdAndDepartmentIdInOrEmployeeNameIgnoreCase(ctx.getOrganizationId(), deptIds, ctx.getUsername());
        } else if (departmentRepository.countByOrganizationId(ctx.getOrganizationId()) > 0) {
          return leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(ctx.getOrganizationId(), ctx.getUsername());
        }
      }
      if (!ctx.hasAdminOrHrPrivileges()) {
        return leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(ctx.getOrganizationId(), ctx.getUsername());
      }
      return leaveRepository.findByOrganizationId(ctx.getOrganizationId());
    }
    // Missing tenant context: fail closed against tenant data
    if (auth != null && auth.getAuthorities() != null) {
      boolean isPlatformAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
      if (isPlatformAdmin) {
        return Collections.emptyList();
      }
      boolean isLegacyAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (isLegacyAdmin) {
        return leaveRepository.findByOrganizationIsNull();
      }
    }
    return Collections.emptyList();
  }

  @Override
  @Transactional(readOnly = true)
  public Leave getLeaveById(Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    Leave leave = leaveRepository.findById(id).orElse(null);
    if (leave == null) {
      return null;
    }
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
        return null;
      }
    } else {
      if (leave.getOrganization() != null) {
        return null;
      }
      boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
          auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
      if (isPlatformAdmin) {
        return null;
      }
    }
    return leave;
  }

  @Override
  public Leave updateLeave(Long id, Leave leave) {
    Optional<Leave> existingLeave = leaveRepository.findById(id);
    if (existingLeave.isPresent()) {
      Leave l = existingLeave.get();
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
      Long orgId = l.getOrganization() != null ? l.getOrganization().getId() : null;
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        if (l.getOrganization() == null || !l.getOrganization().getId().equals(ctx.getOrganizationId())) {
          return null;
        }
      } else {
        if (l.getOrganization() != null) {
          return null;
        }
        boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
        if (isPlatformAdmin) {
          return null;
        }
        boolean isLegacyAdmin = (auth != null && auth.getAuthorities() != null &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")));
        boolean isOwner = (auth != null && l.getEmployeeName() != null && l.getEmployeeName().equalsIgnoreCase(auth.getName()));
        if (!isLegacyAdmin && !isOwner) {
          return null;
        }
      }
      String previousStatus = l.getStatus();
      if (leave.getEmployeeName() != null && !leave.getEmployeeName().isBlank()) {
        l.setEmployeeName(leave.getEmployeeName());
      }
      if (leave.getStartDate() != null && !leave.getStartDate().isBlank()) {
        l.setStartDate(leave.getStartDate());
      }
      if (leave.getEndDate() != null && !leave.getEndDate().isBlank()) {
        l.setEndDate(leave.getEndDate());
      }
      if (leave.getReason() != null) {
        l.setReason(leave.getReason());
      }
      if (leave.getStatus() != null && !leave.getStatus().isBlank()) {
        l.setStatus(leave.getStatus());
      }
      if (leave.getLeaveType() != null && !leave.getLeaveType().isBlank()) {
        l.setLeaveType(leave.getLeaveType());
      }
      if (leave.getImpactScore() != null) {
        l.setImpactScore(leave.getImpactScore());
      }
      if (leave.getAiRecommendation() != null) {
        l.setAiRecommendation(leave.getAiRecommendation());
      }
      if (leave.getRejectionReason() != null) {
        l.setRejectionReason(leave.getRejectionReason());
      }
      int calculatedDays = calculateDays(l.getStartDate(), l.getEndDate(), l.getDaysCount());
      if (calculatedDays <= 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Invalid leave request: Selected date range contains 0 working days (weekends are excluded).");
      }
      l.setDaysCount(calculatedDays);
      Leave updatedLeave = leaveRepository.save(l);
      notificationService.createNotification(orgId, "Leave request #" + id + " has been updated. Status: " + l.getStatus());

      // Record update audit event
      String actor = resolveActor(l.getEmployeeName());
      String role = resolveActorRole(actor);
      recordAuditEvent(orgId, l.getId(), "UPDATED", actor, role, previousStatus, l.getStatus(), null, null,
          "Leave request updated. Status: " + l.getStatus());

      return updatedLeave;
    }
    return null;
  }

  @Override
  public boolean deleteLeave(Long id) {
    Optional<Leave> opt = leaveRepository.findById(id);
    if (opt.isPresent()) {
      Leave l = opt.get();
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        if (l.getOrganization() == null || !l.getOrganization().getId().equals(ctx.getOrganizationId())) {
          return false;
        }
      } else {
        if (l.getOrganization() != null) {
          return false;
        }
        boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
        if (isPlatformAdmin) {
          return false;
        }
        boolean isLegacyAdmin = (auth != null && auth.getAuthorities() != null &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")));
        boolean isOwner = (auth != null && l.getEmployeeName() != null && l.getEmployeeName().equalsIgnoreCase(auth.getName()));
        if (!isLegacyAdmin && !isOwner) {
          return false;
        }
      }
      leaveRepository.deleteById(id);
      Long orgId = l.getOrganization() != null ? l.getOrganization().getId() : null;
      notificationService.createNotification(orgId, "Leave request #" + id + " has been deleted.");
      return true;
    }
    return false;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Leave> getLeavesPaginated(int page, int size) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      PageRequest pageRequest = PageRequest.of(page, size);
      if (ctx.isManager()) {
        List<Department> managedDepts = departmentRepository.findByOrganizationIdAndManagerMembershipId(ctx.getOrganizationId(), ctx.getMembershipId());
        if (!managedDepts.isEmpty()) {
          List<Long> deptIds = managedDepts.stream().map(Department::getId).toList();
          return leaveRepository.findByOrganizationIdAndDepartmentIdInOrEmployeeNameIgnoreCase(ctx.getOrganizationId(), deptIds, ctx.getUsername(), pageRequest);
        } else if (departmentRepository.countByOrganizationId(ctx.getOrganizationId()) > 0) {
          return leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(ctx.getOrganizationId(), ctx.getUsername(), pageRequest);
        }
      }
      if (!ctx.hasAdminOrHrPrivileges()) {
        return leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(ctx.getOrganizationId(), ctx.getUsername(), pageRequest);
      }
      return leaveRepository.findByOrganizationId(ctx.getOrganizationId(), pageRequest);
    }
    if (auth != null && auth.getAuthorities() != null) {
      boolean isPlatformAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
      if (isPlatformAdmin) {
        return Page.empty(PageRequest.of(page, size));
      }
      boolean isLegacyAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (isLegacyAdmin) {
        return leaveRepository.findByOrganizationIsNull(PageRequest.of(page, size));
      }
    }
    return Page.empty(PageRequest.of(page, size));
  }

  @Override
  public Leave updateLeaveStatus(Long id, String status) {
    if ("Approved".equalsIgnoreCase(status)) {
      return approveLeave(id, null);
    } else if ("Rejected".equalsIgnoreCase(status)) {
      return rejectLeave(id, "Rejected by administrator", null);
    } else if ("Cancelled".equalsIgnoreCase(status)) {
      return cancelLeave(id, null);
    }

    Optional<Leave> existingLeave = leaveRepository.findById(id);
    if (existingLeave.isPresent()) {
      Leave leave = existingLeave.get();
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
      Long orgId = leave.getOrganization() != null ? leave.getOrganization().getId() : null;
      if (contextOpt.isPresent()) {
        TenantContext ctx = contextOpt.get();
        if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
          return null;
        }
      } else {
        if (leave.getOrganization() != null) {
          return null;
        }
        boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
        if (isPlatformAdmin) {
          return null;
        }
        boolean isLegacyAdmin = (auth != null && auth.getAuthorities() != null &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")));
        if (!isLegacyAdmin) {
          return null;
        }
      }
      String prevStatus = leave.getStatus();
      leave.setStatus(status);
      Leave updatedLeave = leaveRepository.save(leave);
      notificationService.createNotification(orgId, "Leave request #" + id + " has been " + status.toLowerCase() + ".");

      String actor = resolveActor(leave.getEmployeeName());
      String role = resolveActorRole(actor);
      recordAuditEvent(orgId, id, "UPDATED", actor, role, prevStatus, status, null, null,
          "Leave status changed to " + status);

      return updatedLeave;
    }
    return null;
  }

  @Override
  public Leave approveLeave(Long id, String approver) {
    Leave leave = leaveRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found"));

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    Long orgId = leave.getOrganization() != null ? leave.getOrganization().getId() : null;
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found");
      }
      if (!ctx.hasApprovalPrivileges()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Unauthorized: Only administrators or managers can approve leave requests.");
      }
      if (ctx.isManager()) {
        List<Department> managedDepts = departmentRepository.findByOrganizationIdAndManagerMembershipId(ctx.getOrganizationId(), ctx.getMembershipId());
        if (!managedDepts.isEmpty()) {
          // Authoritative department resolution: resolve current department from EmployeeProfile
          Optional<EmployeeProfile> applicantProf = employeeProfileRepository
              .findByOrganizationIdAndMembershipUserUsername(ctx.getOrganizationId(), leave.getEmployeeName());
          Department currentDept = applicantProf.map(EmployeeProfile::getDepartmentRef).orElse(null);
          if (currentDept == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: Applicant has no current department assignment. Manager cannot approve.");
          }
          boolean authorized = managedDepts.stream().anyMatch(d -> d.getId().equals(currentDept.getId()));
          if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: Manager is not authorized to approve leave requests outside their currently managed department(s).");
          }
        } else if (departmentRepository.countByOrganizationId(ctx.getOrganizationId()) > 0) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Manager is not assigned to manage any department.");
        }
      }
    } else {
      if (leave.getOrganization() != null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found");
      }
      boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
          auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
      if (isPlatformAdmin) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot perform tenant HR operations");
      }
    }

    if (!"Pending".equalsIgnoreCase(leave.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Leave request #" + id + " is already " + leave.getStatus());
    }

    if (approver == null || approver.isBlank()) {
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        approver = auth.getName();
      }
    }

    if (approver != null) {
      if (approver.equalsIgnoreCase(leave.getEmployeeName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Self-approval is strictly prohibited. Applicant cannot approve their own leave request.");
      }
      if (contextOpt.isEmpty()) {
        User approverUser = userRepository.findByUsername(approver);
        if (approverUser != null && !"admin".equalsIgnoreCase(approverUser.getRole())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN,
              "Unauthorized: Only administrators/managers can approve leave requests.");
        }
      }
    }

    int requestedDays = calculateDays(leave.getStartDate(), leave.getEndDate(), leave.getDaysCount());
    if (requestedDays <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid leave request: Selected date range contains 0 working days (weekends are excluded).");
    }
    leave.setDaysCount(requestedDays);

    Integer previousBalance = null;
    Integer newBalance = null;
    if (leave.getEmployeeName() != null) {
      User employee = userRepository.findByUsername(leave.getEmployeeName());
      if (employee != null) {
        int currentBalance = employee.getLeaveBalance() != null ? employee.getLeaveBalance() : 0;
        previousBalance = currentBalance;
        if (currentBalance < requestedDays) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Insufficient leave balance. Required: " + requestedDays + ", Available: " + currentBalance);
        }
        newBalance = currentBalance - requestedDays;
        employee.setLeaveBalance(newBalance);
        userRepository.save(employee);
      }
    }

    leave.setStatus("Approved");
    String actor = (approver != null && !approver.isBlank()) ? approver : "Admin";
    leave.setApprovedBy(actor);
    leave.setActionBy(actor);
    leave.setActionAt(java.time.LocalDateTime.now().toString());
    Leave savedLeave = leaveRepository.save(leave);

    recordAuditEvent(orgId, savedLeave.getId(), "APPROVED", actor, "ADMIN", "Pending", "Approved",
        previousBalance, newBalance,
        "Leave request approved. " + requestedDays + " day(s) deducted from leave balance.");

    notificationService.createNotification(orgId, leave.getEmployeeName(),
        "Leave request #" + id + " for " + leave.getEmployeeName() + " (" + requestedDays + " days) has been approved by " + actor + ".");

    return savedLeave;
  }

  @Override
  public Leave rejectLeave(Long id, String rejectionReason, String approver) {
    Leave leave = leaveRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found"));

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    Long orgId = leave.getOrganization() != null ? leave.getOrganization().getId() : null;
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found");
      }
      if (!ctx.hasApprovalPrivileges()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Unauthorized: Only administrators or managers can reject leave requests.");
      }
      if (ctx.isManager()) {
        List<Department> managedDepts = departmentRepository.findByOrganizationIdAndManagerMembershipId(ctx.getOrganizationId(), ctx.getMembershipId());
        if (!managedDepts.isEmpty()) {
          // Authoritative department resolution: resolve current department from EmployeeProfile
          Optional<EmployeeProfile> applicantProf = employeeProfileRepository
              .findByOrganizationIdAndMembershipUserUsername(ctx.getOrganizationId(), leave.getEmployeeName());
          Department currentDept = applicantProf.map(EmployeeProfile::getDepartmentRef).orElse(null);
          if (currentDept == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: Applicant has no current department assignment. Manager cannot reject.");
          }
          boolean authorized = managedDepts.stream().anyMatch(d -> d.getId().equals(currentDept.getId()));
          if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: Manager is not authorized to reject leave requests outside their currently managed department(s).");
          }
        } else if (departmentRepository.countByOrganizationId(ctx.getOrganizationId()) > 0) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Manager is not assigned to manage any department.");
        }
      }
    } else {
      if (leave.getOrganization() != null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found");
      }
      boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
          auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
      if (isPlatformAdmin) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot perform tenant HR operations");
      }
    }

    if (!"Pending".equalsIgnoreCase(leave.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Leave request #" + id + " is already " + leave.getStatus());
    }

    if (approver == null || approver.isBlank()) {
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        approver = auth.getName();
      }
    }

    if (approver != null) {
      if (approver.equalsIgnoreCase(leave.getEmployeeName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Self-action is strictly prohibited. Applicant cannot reject their own leave request.");
      }
      if (contextOpt.isEmpty()) {
        User approverUser = userRepository.findByUsername(approver);
        if (approverUser != null && !"admin".equalsIgnoreCase(approverUser.getRole())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN,
              "Unauthorized: Only administrators/managers can reject leave requests.");
        }
      }
    }

    leave.setStatus("Rejected");
    if (rejectionReason != null && !rejectionReason.isBlank()) {
      leave.setRejectionReason(rejectionReason);
    } else {
      leave.setRejectionReason("Rejected by approver");
    }
    String actor = (approver != null && !approver.isBlank()) ? approver : "Admin";
    leave.setRejectedBy(actor);
    leave.setActionBy(actor);
    leave.setActionAt(java.time.LocalDateTime.now().toString());

    Leave savedLeave = leaveRepository.save(leave);

    Integer empBal = null;
    if (leave.getEmployeeName() != null) {
      User emp = userRepository.findByUsername(leave.getEmployeeName());
      if (emp != null) {
        empBal = emp.getLeaveBalance();
      }
    }
    recordAuditEvent(orgId, savedLeave.getId(), "REJECTED", actor, "ADMIN", "Pending", "Rejected",
        empBal, empBal, "Leave request rejected. Reason: " + leave.getRejectionReason());

    notificationService.createNotification(orgId, leave.getEmployeeName(),
        "Leave request #" + id + " for " + leave.getEmployeeName() + " was rejected. Reason: " + leave.getRejectionReason());

    return savedLeave;
  }

  @Override
  public Leave cancelLeave(Long id, String currentUser) {
    Leave leave = leaveRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found"));

    String currentStatus = leave.getStatus();
    if ("Cancelled".equalsIgnoreCase(currentStatus)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Leave request #" + id + " is already Cancelled");
    }

    if (!"Pending".equalsIgnoreCase(currentStatus) &&
        !"Approved".equalsIgnoreCase(currentStatus) &&
        !"Rejected".equalsIgnoreCase(currentStatus)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Cannot cancel leave request with status " + currentStatus);
    }

    if (currentUser == null || currentUser.isBlank()) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        currentUser = auth.getName();
      }
    }

    if (currentUser == null || currentUser.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to cancel leave request");
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    Long orgId = leave.getOrganization() != null ? leave.getOrganization().getId() : null;
    boolean isApplicant = leave.getEmployeeName() != null && leave.getEmployeeName().equalsIgnoreCase(currentUser);

    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found");
      }
      if (!ctx.hasAdminOrHrPrivileges() && !isApplicant) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Unauthorized: You cannot cancel another employee's leave request.");
      }
    } else {
      if (leave.getOrganization() != null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found");
      }
      boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
          auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
      if (isPlatformAdmin) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot perform tenant HR operations");
      }
      User callerUser = userRepository.findByUsername(currentUser);
      boolean isAdmin = (callerUser != null && "admin".equalsIgnoreCase(callerUser.getRole()));
      if (!isAdmin) {
        if (auth != null && auth.getAuthorities() != null) {
          isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        }
      }
      if (!isAdmin && !isApplicant) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Unauthorized: You cannot cancel another employee's leave request.");
      }
    }

    Integer prevBalance = null;
    Integer newBalance = null;
    int refundedDays = 0;
    if ("Approved".equalsIgnoreCase(currentStatus)) {
      refundedDays = (leave.getDaysCount() != null && leave.getDaysCount() > 0)
          ? leave.getDaysCount()
          : calculateDays(leave.getStartDate(), leave.getEndDate(), 1);
      if (leave.getEmployeeName() != null) {
        User employee = userRepository.findByUsername(leave.getEmployeeName());
        if (employee != null) {
          int currentBalance = employee.getLeaveBalance() != null ? employee.getLeaveBalance() : 0;
          prevBalance = currentBalance;
          newBalance = currentBalance + refundedDays;
          employee.setLeaveBalance(newBalance);
          userRepository.save(employee);
        }
      }
    } else {
      if (leave.getEmployeeName() != null) {
        User employee = userRepository.findByUsername(leave.getEmployeeName());
        if (employee != null) {
          prevBalance = employee.getLeaveBalance();
          newBalance = employee.getLeaveBalance();
        }
      }
    }

    leave.setStatus("Cancelled");
    leave.setCancelledBy(currentUser);
    leave.setActionBy(currentUser);
    leave.setActionAt(java.time.LocalDateTime.now().toString());
    Leave savedLeave = leaveRepository.save(leave);

    boolean isAdminCaller = contextOpt.map(TenantContext::hasAdminOrHrPrivileges).orElse(false);
    if (contextOpt.isEmpty() && auth != null && auth.getAuthorities() != null) {
      isAdminCaller = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }
    String callerRole = isAdminCaller ? "ADMIN" : "EMPLOYEE";
    recordAuditEvent(orgId, savedLeave.getId(), "CANCELLED", currentUser, callerRole, currentStatus, "Cancelled",
        prevBalance, newBalance,
        "Leave request cancelled by " + currentUser +
        ("Approved".equalsIgnoreCase(currentStatus) ? ". " + refundedDays + " day(s) refunded to leave balance." : "."));

    notificationService.createNotification(orgId, leave.getEmployeeName(),
        "Leave request #" + id + " for " + leave.getEmployeeName() + " has been cancelled" +
        ("Approved".equalsIgnoreCase(currentStatus) ? " and leave balance has been refunded." : "."));

    return savedLeave;
  }

  private void recordAuditEvent(Long organizationId, Long leaveId, String eventType, String actor, String role,
                                 String fromStatus, String toStatus,
                                 Integer prevBalance, Integer newBalance, String details) {
    LeaveAuditEvent event = new LeaveAuditEvent(
        leaveId,
        eventType,
        (actor != null && !actor.isBlank()) ? actor : "system",
        (role != null && !role.isBlank()) ? role : "SYSTEM",
        Instant.now(),
        fromStatus,
        toStatus,
        prevBalance,
        newBalance,
        details
    );
    event.setOrganizationId(organizationId);
    leaveAuditEventRepository.save(event);
  }

  private String resolveActor(String fallback) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
      return auth.getName();
    }
    return (fallback != null && !fallback.isBlank()) ? fallback : "system";
  }

  private String resolveActorRole(String actor) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && auth.getName().equalsIgnoreCase(actor)) {
      return auth.getAuthorities().stream()
          .map(a -> a.getAuthority().replace("ROLE_", ""))
          .findFirst()
          .orElse("EMPLOYEE");
    }
    if (actor != null) {
      User user = userRepository.findByUsername(actor);
      if (user != null && user.getRole() != null) {
        return user.getRole().toUpperCase();
      }
    }
    return "EMPLOYEE";
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LeaveAuditEventDTO> getAuditTrailForLeave(Long leaveId, String eventType, int page, int size) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    
    Leave leave = leaveRepository.findById(leaveId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + leaveId + " not found"));
    
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      if (leave.getOrganization() == null || !leave.getOrganization().getId().equals(ctx.getOrganizationId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + leaveId + " not found");
      }
    } else {
      if (leave.getOrganization() != null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + leaveId + " not found");
      }
      boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
          auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
      if (isPlatformAdmin) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform administrators cannot access tenant audit data");
      }
    }

    PageRequest pageRequest = PageRequest.of(page, size);
    Page<LeaveAuditEvent> events;
    if (contextOpt.isPresent()) {
      Long orgId = contextOpt.get().getOrganizationId();
      if (eventType != null && !eventType.isBlank()) {
        events = leaveAuditEventRepository.findByOrganizationIdAndLeaveIdAndEventTypeOrderByEventTimestampDesc(orgId, leaveId, eventType.toUpperCase(), pageRequest);
      } else {
        events = leaveAuditEventRepository.findByOrganizationIdAndLeaveIdOrderByEventTimestampDesc(orgId, leaveId, pageRequest);
      }
    } else {
      if (eventType != null && !eventType.isBlank()) {
        events = leaveAuditEventRepository.findByLeaveIdAndEventTypeOrderByEventTimestampDesc(leaveId, eventType.toUpperCase(), pageRequest);
      } else {
        events = leaveAuditEventRepository.findByLeaveIdOrderByEventTimestampDesc(leaveId, pageRequest);
      }
    }
    return events.map(LeaveAuditEventDTO::new);
  }

  @Override
  public int calculateWorkingDays(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date must not be null.");
    }
    if (end.isBefore(start)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid date range: End date (" + end + ") cannot be before start date (" + start + ").");
    }
    long calendarDays = ChronoUnit.DAYS.between(start, end) + 1;
    if (calendarDays > 366) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Leave request duration cannot exceed 366 calendar days in a single request.");
    }
    int workingDays = 0;
    LocalDate curr = start;
    while (!curr.isAfter(end)) {
      DayOfWeek dow = curr.getDayOfWeek();
      if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
        workingDays++;
      }
      curr = curr.plusDays(1);
    }
    return workingDays;
  }

  @Override
  public int calculateDays(String startDate, String endDate, Integer fallbackDays) {
    if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
      if (fallbackDays != null && fallbackDays > 0) {
        return fallbackDays;
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date are required.");
    }
    LocalDate start;
    LocalDate end;
    try {
      start = LocalDate.parse(startDate.trim());
      end = LocalDate.parse(endDate.trim());
    } catch (DateTimeParseException dtpe) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid date format: " + dtpe.getMessage() + ". Expected format is YYYY-MM-DD.");
    }
    return calculateWorkingDays(start, end);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Leave> getLeavesForUser(String username) {
    if (username == null || username.isBlank()) {
      return List.of();
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    if (contextOpt.isPresent()) {
      return leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(contextOpt.get().getOrganizationId(), username);
    }
    return leaveRepository.findByOrganizationIsNullAndEmployeeNameIgnoreCase(username);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Leave> getLeavesForUserPaginated(String username, int page, int size) {
    if (username == null || username.isBlank()) {
      return Page.empty();
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    if (contextOpt.isPresent()) {
      return leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(contextOpt.get().getOrganizationId(), username, PageRequest.of(page, size));
    }
    return leaveRepository.findByOrganizationIsNullAndEmployeeNameIgnoreCase(username, PageRequest.of(page, size));
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeamCalendarLeaveDTO> getTeamCalendar(String start, String end, String statusFilter, String currentUser, boolean isAdmin) {
    if (start == null || start.isBlank() || end == null || end.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date parameters are required in YYYY-MM-DD format.");
    }

    LocalDate startDate;
    LocalDate endDate;
    try {
      startDate = LocalDate.parse(start.trim());
      endDate = LocalDate.parse(end.trim());
    } catch (DateTimeParseException dtpe) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format. Expected YYYY-MM-DD: " + dtpe.getMessage());
    }

    if (endDate.isBefore(startDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid date range: End date (" + endDate + ") cannot be before start date (" + startDate + ").");
    }

    long calendarDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    if (calendarDays > 93) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Requested calendar range cannot exceed 93 calendar days (requested: " + calendarDays + " days).");
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Optional<TenantContext> contextOpt = (tenantContextService != null) ? tenantContextService.resolveTenantContext(auth) : Optional.empty();
    List<Leave> leaves;
    if (contextOpt.isPresent()) {
      TenantContext ctx = contextOpt.get();
      Long orgId = ctx.getOrganizationId();
      if (ctx.isManager()) {
        List<Department> managedDepts = departmentRepository.findByOrganizationIdAndManagerMembershipId(orgId, ctx.getMembershipId());
        if (!managedDepts.isEmpty()) {
          List<Long> deptIds = managedDepts.stream().map(Department::getId).toList();
          leaves = leaveRepository.findOverlappingActiveLeavesByOrgAndDepartmentsOrEmployee(orgId, deptIds, ctx.getUsername(), start.trim(), end.trim());
        } else if (departmentRepository.countByOrganizationId(orgId) > 0) {
          leaves = leaveRepository.findByOrganizationIdAndEmployeeNameIgnoreCase(orgId, ctx.getUsername());
        } else {
          leaves = leaveRepository.findOverlappingActiveLeavesByOrg(orgId, start.trim(), end.trim());
        }
      } else {
        leaves = leaveRepository.findOverlappingActiveLeavesByOrg(orgId, start.trim(), end.trim());
      }
      isAdmin = ctx.hasAdminOrHrPrivileges();
    } else {
      boolean isPlatformAdmin = (auth != null && auth.getAuthorities() != null &&
          auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")));
      if (isPlatformAdmin) {
        leaves = Collections.emptyList();
      } else {
        leaves = leaveRepository.findOverlappingActiveLeavesUnassigned(start.trim(), end.trim());
      }
    }

    List<TeamCalendarLeaveDTO> result = new ArrayList<>();
    for (Leave leave : leaves) {
      String status = leave.getStatus();
      if (status == null || "Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
        continue;
      }

      boolean isPending = "Pending".equalsIgnoreCase(status);

      // Pending visibility rules:
      // Administrators and authorized department managers can see pending requests.
      // Regular employees can see all approved team requests, plus their own pending requests.
      if (isPending) {
        if (!isAdmin) {
          boolean isManagerOfLeave = false;
          if (contextOpt.isPresent() && contextOpt.get().isManager() && leave.getDepartment() != null) {
            List<Department> managedDepts = departmentRepository.findByOrganizationIdAndManagerMembershipId(contextOpt.get().getOrganizationId(), contextOpt.get().getMembershipId());
            isManagerOfLeave = managedDepts.stream().anyMatch(d -> d.getId().equals(leave.getDepartment().getId()));
          }
          if (!isManagerOfLeave) {
            if (currentUser == null || leave.getEmployeeName() == null || !currentUser.equalsIgnoreCase(leave.getEmployeeName())) {
              continue; // Exclude colleague's pending leaves from non-admin employee view
            }
          }
        }
      }

      // Optional status filter
      if (statusFilter != null && !statusFilter.isBlank()) {
        if (!statusFilter.equalsIgnoreCase(status)) {
          continue;
        }
      }

      result.add(new TeamCalendarLeaveDTO(
          leave.getId(),
          leave.getEmployeeName(),
          leave.getStartDate(),
          leave.getEndDate(),
          leave.getDaysCount(),
          leave.getStatus(),
          "Out of Office"
      ));
    }

    return result;
  }
}