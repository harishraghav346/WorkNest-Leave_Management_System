package com.examly.springapp.model;

/**
 * Privacy-preserving DTO for the Team Availability Calendar.
 * Excludes sensitive fields such as personal leave reasons, medical notes,
 * AI recommendation data, rejection justifications, and authentication info.
 */
public class TeamCalendarLeaveDTO {
    private Long id;
    private String employeeName;
    private String startDate;
    private String endDate;
    private Integer daysCount;
    private String status;
    private String displayLabel;

    public TeamCalendarLeaveDTO() {
    }

    public TeamCalendarLeaveDTO(Long id, String employeeName, String startDate, String endDate, Integer daysCount, String status, String displayLabel) {
        this.id = id;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysCount = daysCount;
        this.status = status;
        this.displayLabel = (displayLabel != null && !displayLabel.isBlank()) ? displayLabel : "Out of Office";
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

    public Integer getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(Integer daysCount) {
        this.daysCount = daysCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }
}
