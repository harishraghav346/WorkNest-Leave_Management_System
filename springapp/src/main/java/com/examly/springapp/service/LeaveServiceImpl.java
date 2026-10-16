package com.examly.springapp.service;

import com.examly.springapp.model.Leave;
import com.examly.springapp.model.LeaveAuditEvent;
import com.examly.springapp.model.LeaveAuditEventDTO;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveAuditEventRepository;
import com.examly.springapp.repository.LeaveRepository;
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

    Leave newLeave = leaveRepository.save(leave);
    notificationService.createNotification("New leave request submitted by " + leave.getEmployeeName());

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
    recordAuditEvent(newLeave.getId(), "CREATED", actor, role, null, newLeave.getStatus(), userBalance, userBalance,
        "Leave request created for " + newLeave.getDaysCount() + " day(s) (" + newLeave.getLeaveType() + ")");

    return newLeave;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Leave> getAllLeave() {
    return leaveRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Leave getLeaveById(Long id) {
    return leaveRepository.findById(id).orElse(null);
  }

  @Override
  public Leave updateLeave(Long id, Leave leave) {
    Optional<Leave> existingLeave = leaveRepository.findById(id);
    if (existingLeave.isPresent()) {
      Leave l = existingLeave.get();
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
      notificationService.createNotification("Leave request #" + id + " has been updated. Status: " + l.getStatus());

      // Record update audit event
      String actor = resolveActor(l.getEmployeeName());
      String role = resolveActorRole(actor);
      recordAuditEvent(l.getId(), "UPDATED", actor, role, previousStatus, l.getStatus(), null, null,
          "Leave request updated. Status: " + l.getStatus());

      return updatedLeave;
    }
    return null;
  }

  @Override
  public boolean deleteLeave(Long id) {
    if (leaveRepository.existsById(id)) {
      leaveRepository.deleteById(id);
      notificationService.createNotification("Leave request #" + id + " has been deleted.");
      return true;
    }
    return false;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Leave> getLeavesPaginated(int page, int size) {
    return leaveRepository.findAll(PageRequest.of(page, size));
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
      String prevStatus = leave.getStatus();
      leave.setStatus(status);
      Leave updatedLeave = leaveRepository.save(leave);
      notificationService.createNotification("Leave request #" + id + " has been " + status.toLowerCase() + ".");

      String actor = resolveActor(leave.getEmployeeName());
      String role = resolveActorRole(actor);
      recordAuditEvent(id, "UPDATED", actor, role, prevStatus, status, null, null,
          "Leave status changed to " + status);

      return updatedLeave;
    }
    return null;
  }

  @Override
  public Leave approveLeave(Long id, String approver) {
    // 1. Confirm the request exists
    Leave leave = leaveRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found"));

    // 2. Confirm current status permits approval (prevent duplicate processing and invalid status transition)
    if (!"Pending".equalsIgnoreCase(leave.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Leave request #" + id + " is already " + leave.getStatus());
    }

    // 3. Resolve approver from parameter or SecurityContext
    if (approver == null || approver.isBlank()) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        approver = auth.getName();
      }
    }

    // Approver authorization & Self-approval check
    if (approver != null) {
      if (approver.equalsIgnoreCase(leave.getEmployeeName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Self-approval is strictly prohibited. Applicant cannot approve their own leave request.");
      }
      User approverUser = userRepository.findByUsername(approver);
      if (approverUser != null && !"admin".equalsIgnoreCase(approverUser.getRole())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Unauthorized: Only administrators/managers can approve leave requests.");
      }
    }

    // 4. Calculate requested leave duration using SOP rules
    int requestedDays = calculateDays(leave.getStartDate(), leave.getEndDate(), leave.getDaysCount());
    if (requestedDays <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid leave request: Selected date range contains 0 working days (weekends are excluded).");
    }
    leave.setDaysCount(requestedDays);

    // 5. Verify employee balance & deduct
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
        // Deduct correct number of days
        newBalance = currentBalance - requestedDays;
        employee.setLeaveBalance(newBalance);
        userRepository.save(employee);
      }
    }

    // 6. Update leave status & record current-state metadata
    leave.setStatus("Approved");
    String actor = (approver != null && !approver.isBlank()) ? approver : "Admin";
    leave.setApprovedBy(actor);
    leave.setActionBy(actor);
    leave.setActionAt(java.time.LocalDateTime.now().toString());
    Leave savedLeave = leaveRepository.save(leave);

    // 7. Persist append-only audit event in the same transaction
    recordAuditEvent(savedLeave.getId(), "APPROVED", actor, "ADMIN", "Pending", "Approved",
        previousBalance, newBalance,
        "Leave request approved. " + requestedDays + " day(s) deducted from leave balance.");

    // 8. Record notification
    notificationService.createNotification(
        "Leave request #" + id + " for " + leave.getEmployeeName() + " (" + requestedDays + " days) has been approved by " + actor + ".");

    return savedLeave;
  }

  @Override
  public Leave rejectLeave(Long id, String rejectionReason, String approver) {
    // 1. Confirm the request exists
    Leave leave = leaveRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found"));

    // 2. Confirm current status permits rejection
    if (!"Pending".equalsIgnoreCase(leave.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Leave request #" + id + " is already " + leave.getStatus());
    }

    // 3. Resolve approver from parameter or SecurityContext
    if (approver == null || approver.isBlank()) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        approver = auth.getName();
      }
    }

    // Self-action check & approver role check
    if (approver != null) {
      if (approver.equalsIgnoreCase(leave.getEmployeeName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Self-action is strictly prohibited. Applicant cannot reject their own leave request.");
      }
      User approverUser = userRepository.findByUsername(approver);
      if (approverUser != null && !"admin".equalsIgnoreCase(approverUser.getRole())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Unauthorized: Only administrators/managers can reject leave requests.");
      }
    }

    // 4. Update status & persist rejection reason (NO balance deduction)
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

    // 5. Persist append-only audit event in the same transaction
    Integer empBal = null;
    if (leave.getEmployeeName() != null) {
      User emp = userRepository.findByUsername(leave.getEmployeeName());
      if (emp != null) {
        empBal = emp.getLeaveBalance();
      }
    }
    recordAuditEvent(savedLeave.getId(), "REJECTED", actor, "ADMIN", "Pending", "Rejected",
        empBal, empBal, "Leave request rejected. Reason: " + leave.getRejectionReason());

    // 6. Record notification
    notificationService.createNotification(
        "Leave request #" + id + " for " + leave.getEmployeeName() + " was rejected. Reason: " + leave.getRejectionReason());

    return savedLeave;
  }

  @Override
  public Leave cancelLeave(Long id, String currentUser) {
    // 1. Confirm the request exists
    Leave leave = leaveRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request #" + id + " not found"));

    // 2. Prevent repeated cancellation and reject invalid status transitions
    String currentStatus = leave.getStatus();
    if ("Cancelled".equalsIgnoreCase(currentStatus)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Leave request #" + id + " is already Cancelled");
    }

    // Only Pending, Approved, or Rejected leaves can be transitioned to Cancelled
    if (!"Pending".equalsIgnoreCase(currentStatus) &&
        !"Approved".equalsIgnoreCase(currentStatus) &&
        !"Rejected".equalsIgnoreCase(currentStatus)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid status transition: Cannot cancel leave request with status " + currentStatus);
    }

    // 3. Resolve caller identity from parameter or SecurityContext
    if (currentUser == null || currentUser.isBlank()) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
        currentUser = auth.getName();
      }
    }

    if (currentUser == null || currentUser.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to cancel leave request");
    }

    // 4. Authorization check: Employees can only cancel their own request; Admins/HR can cancel any request
    User callerUser = userRepository.findByUsername(currentUser);
    boolean isAdmin = (callerUser != null && "admin".equalsIgnoreCase(callerUser.getRole()));
    if (!isAdmin) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getAuthorities() != null) {
        isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      }
    }

    boolean isApplicant = leave.getEmployeeName() != null && leave.getEmployeeName().equalsIgnoreCase(currentUser);

    if (!isAdmin && !isApplicant) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Unauthorized: You cannot cancel another employee's leave request.");
    }

    // 5. Balance restoration: If previously Approved, restore deducted days; if Pending or Rejected, do not refund
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

    // 6. Update status to Cancelled & record current-state metadata
    leave.setStatus("Cancelled");
    leave.setCancelledBy(currentUser);
    leave.setActionBy(currentUser);
    leave.setActionAt(java.time.LocalDateTime.now().toString());
    Leave savedLeave = leaveRepository.save(leave);

    // 7. Persist append-only audit event in the same transaction
    String callerRole = isAdmin ? "ADMIN" : "EMPLOYEE";
    recordAuditEvent(savedLeave.getId(), "CANCELLED", currentUser, callerRole, currentStatus, "Cancelled",
        prevBalance, newBalance,
        "Leave request cancelled by " + currentUser +
        ("Approved".equalsIgnoreCase(currentStatus) ? ". " + refundedDays + " day(s) refunded to leave balance." : "."));

    // 8. Notification
    notificationService.createNotification(
        "Leave request #" + id + " for " + leave.getEmployeeName() + " has been cancelled" +
        ("Approved".equalsIgnoreCase(currentStatus) ? " and leave balance has been refunded." : "."));

    return savedLeave;
  }

  private void recordAuditEvent(Long leaveId, String eventType, String actor, String role,
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
    PageRequest pageRequest = PageRequest.of(page, size);
    Page<LeaveAuditEvent> events;
    if (eventType != null && !eventType.isBlank()) {
      events = leaveAuditEventRepository.findByLeaveIdAndEventTypeOrderByEventTimestampDesc(leaveId, eventType.toUpperCase(), pageRequest);
    } else {
      events = leaveAuditEventRepository.findByLeaveIdOrderByEventTimestampDesc(leaveId, pageRequest);
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
    return leaveRepository.findByEmployeeNameIgnoreCase(username);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Leave> getLeavesForUserPaginated(String username, int page, int size) {
    if (username == null || username.isBlank()) {
      return Page.empty();
    }
    return leaveRepository.findByEmployeeNameIgnoreCase(username, PageRequest.of(page, size));
  }
}