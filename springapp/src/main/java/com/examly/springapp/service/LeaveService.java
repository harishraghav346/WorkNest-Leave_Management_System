package com.examly.springapp.service;

import java.util.List;
import org.springframework.data.domain.Page;
import com.examly.springapp.model.Leave;

public interface LeaveService {
  Leave createLeave(Leave leave);
  List<Leave> getAllLeave();
  Leave getLeaveById(Long id);
  Leave updateLeave(Long id, Leave leave);
  boolean deleteLeave(Long id);
  Page<Leave> getLeavesPaginated(int page, int size);
  Leave updateLeaveStatus(Long id, String status);
}