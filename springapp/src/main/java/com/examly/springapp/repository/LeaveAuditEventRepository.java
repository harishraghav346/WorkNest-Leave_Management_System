package com.examly.springapp.repository;

import com.examly.springapp.model.LeaveAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveAuditEventRepository extends JpaRepository<LeaveAuditEvent, Long> {

    List<LeaveAuditEvent> findByLeaveIdOrderByEventTimestampAsc(Long leaveId);

    Page<LeaveAuditEvent> findByLeaveIdOrderByEventTimestampDesc(Long leaveId, Pageable pageable);

    Page<LeaveAuditEvent> findAllByOrderByEventTimestampDesc(Pageable pageable);

    Page<LeaveAuditEvent> findByLeaveIdAndEventTypeOrderByEventTimestampDesc(Long leaveId, String eventType, Pageable pageable);

    // Multi-tenant scoped audit queries
    Page<LeaveAuditEvent> findByOrganizationIdAndLeaveIdOrderByEventTimestampDesc(Long organizationId, Long leaveId, Pageable pageable);

    Page<LeaveAuditEvent> findByOrganizationIdAndLeaveIdAndEventTypeOrderByEventTimestampDesc(Long organizationId, Long leaveId, String eventType, Pageable pageable);

    Page<LeaveAuditEvent> findByOrganizationIdOrderByEventTimestampDesc(Long organizationId, Pageable pageable);

    java.util.Optional<LeaveAuditEvent> findByIdAndOrganizationId(Long id, Long organizationId);
    List<LeaveAuditEvent> findByOrganizationIdIsNull();
}
