package com.examly.springapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leave_request")
public class Leave {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String employeeName;
  private String startDate;
  private String endDate;
  private String reason;
  private String status = "Pending"; // Pending, Approved, Rejected
  private String leaveType = "Casual Leave"; // Sick Leave, Casual Leave, Annual Leave, Maternity, Emergency
  private Integer daysCount = 1;
  private String impactScore = "Low"; // Low, Medium, High (Calculated by AI or rules)
  private String aiRecommendation; // AI analysis / recommendation summary
  private String rejectionReason;
  private String createdAt;
  private String actionBy;
  private String actionAt;
  private String approvedBy;
  private String rejectedBy;
  private String cancelledBy;

  public Leave() {
  }

  public Leave(Long id, String employeeName, String startDate, String endDate, String reason, String status) {
    this.id = id;
    this.employeeName = employeeName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.reason = reason;
    this.status = status;
  }

  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }
  public String getEmployeeName() {
    return employeeName;
  }
  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }
  public String getStartDate() {
    return startDate;
  }
  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }
  public String getEndDate() {
    return endDate;
  }
  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }
  public String getReason() {
    return reason;
  }
  public void setReason(String reason) {
    this.reason = reason;
  }
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public String getLeaveType() {
    return leaveType;
  }
  public void setLeaveType(String leaveType) {
    this.leaveType = leaveType;
  }
  public Integer getDaysCount() {
    return daysCount;
  }
  public void setDaysCount(Integer daysCount) {
    this.daysCount = daysCount;
  }
  public String getImpactScore() {
    return impactScore;
  }
  public void setImpactScore(String impactScore) {
    this.impactScore = impactScore;
  }
  public String getAiRecommendation() {
    return aiRecommendation;
  }
  public void setAiRecommendation(String aiRecommendation) {
    this.aiRecommendation = aiRecommendation;
  }
  public String getRejectionReason() {
    return rejectionReason;
  }
  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }
  public String getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
  public String getActionBy() {
    return actionBy;
  }
  public void setActionBy(String actionBy) {
    this.actionBy = actionBy;
  }
  public String getActionAt() {
    return actionAt;
  }
  public void setActionAt(String actionAt) {
    this.actionAt = actionAt;
  }
  public String getApprovedBy() {
    return approvedBy;
  }
  public void setApprovedBy(String approvedBy) {
    this.approvedBy = approvedBy;
  }
  public String getRejectedBy() {
    return rejectedBy;
  }
  public void setRejectedBy(String rejectedBy) {
    this.rejectedBy = rejectedBy;
  }
  public String getCancelledBy() {
    return cancelledBy;
  }
  public void setCancelledBy(String cancelledBy) {
    this.cancelledBy = cancelledBy;
  }
}