package com.examly.springapp.service;

import com.examly.springapp.model.Leave;
import com.examly.springapp.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LeaveServiceImpl implements LeaveService {

  @Autowired
  private LeaveRepository leaveRepository;

  @Autowired
  private NotificationService notificationService;

  @Override
  public Leave createLeave(Leave leave) {
    if (leave.getStatus() == null || leave.getStatus().isBlank()) {
      leave.setStatus("Pending");
    }
    Leave newLeave = leaveRepository.save(leave);
    notificationService.createNotification("New leave request submitted by " + leave.getEmployeeName());
    return newLeave;
  }

  @Override
  public List<Leave> getAllLeave() {
    return leaveRepository.findAll();
  }

  @Override
  public Leave getLeaveById(Long id) {
    return leaveRepository.findById(id).orElse(null);
  }

  @Override
  public Leave updateLeave(Long id, Leave leave) {
    Optional<Leave> existingLeave = leaveRepository.findById(id);
    if (existingLeave.isPresent()) {
      Leave l = existingLeave.get();
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
      Leave updatedLeave = leaveRepository.save(l);
      notificationService.createNotification("Leave request #" + id + " has been updated. Status: " + l.getStatus());
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
  public Page<Leave> getLeavesPaginated(int page, int size) {
    return leaveRepository.findAll(PageRequest.of(page, size));
  }

  @Override
  public Leave updateLeaveStatus(Long id, String status) {
    Optional<Leave> existingLeave = leaveRepository.findById(id);
    if (existingLeave.isPresent()) {
      Leave leave = existingLeave.get();
      leave.setStatus(status);
      Leave updatedLeave = leaveRepository.save(leave);
      notificationService.createNotification("Leave request #" + id + " has been " + status.toLowerCase() + ".");
      return updatedLeave;
    }
    return null;
  }
}