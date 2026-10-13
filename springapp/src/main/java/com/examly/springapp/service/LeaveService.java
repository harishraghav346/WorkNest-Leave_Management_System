package com.examly.springapp.service;

import java.util.List;
import org.springframework.data.domain.Page;
import com.examly.springapp.model.Leave;

import com.examly.springapp.model.LeaveAuditEventDTO;

public interface LeaveService {
  Leave createLeave(Leave leave);
  List<Leave> getAllLeave();
  Leave getLeaveById(Long id);
  Leave updateLeave(Long id, Leave leave);
  boolean deleteLeave(Long id);
  Page<Leave> getLeavesPaginated(int page, int size);
  Leave updateLeaveStatus(Long id, String status);
  Leave approveLeave(Long id, String approver);
  Leave rejectLeave(Long id, String rejectionReason, String approver);
  Leave cancelLeave(Long id, String currentUser);
  List<Leave> getLeavesForUser(String username);
  Page<Leave> getLeavesForUserPaginated(String username, int page, int size);
  Page<LeaveAuditEventDTO> getAuditTrailForLeave(Long leaveId, String eventType, int page, int size);
}