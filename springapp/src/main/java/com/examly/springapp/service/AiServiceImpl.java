package com.examly.springapp.service;

import com.examly.springapp.model.AiChatRequest;
import com.examly.springapp.model.AiChatResponse;
import com.examly.springapp.model.AiImpactResponse;
import com.examly.springapp.model.Leave;
import com.examly.springapp.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Override
    public AiChatResponse processChat(AiChatRequest request) {
        String query = request.getMessage() != null ? request.getMessage().trim() : "";
        String lower = query.toLowerCase();
        String employeeName = request.getEmployeeName() != null ? request.getEmployeeName() : "Employee";

        if (lower.contains("book") || lower.contains("apply") || lower.contains("take") || lower.contains("leave")) {
            String leaveType = "Casual Leave";
            if (lower.contains("sick") || lower.contains("doctor") || lower.contains("medical") || lower.contains("fever")) {
                leaveType = "Sick Leave";
            } else if (lower.contains("annual") || lower.contains("vacation") || lower.contains("holiday")) {
                leaveType = "Annual Leave";
            } else if (lower.contains("emergency") || lower.contains("urgent")) {
                leaveType = "Emergency Leave";
            }

            LocalDate start = LocalDate.now().plusDays(2);
            LocalDate end = LocalDate.now().plusDays(2);
            if (lower.contains("tomorrow")) {
                start = LocalDate.now().plusDays(1);
                end = LocalDate.now().plusDays(1);
            }

            Leave suggested = new Leave();
            suggested.setEmployeeName(employeeName);
            suggested.setStartDate(start.toString());
            suggested.setEndDate(end.toString());
            suggested.setLeaveType(leaveType);
            suggested.setReason(query);
            suggested.setStatus("Pending");

            String reply = String.format("I've prepared a %s request from %s to %s for '%s'. Would you like to confirm and submit?",
                    leaveType, start, end, query);

            return new AiChatResponse(reply, "LEAVE_BOOKING", suggested);
        }

        if (lower.contains("balance") || lower.contains("quota") || lower.contains("remaining")) {
            return new AiChatResponse("You have 20 annual leave days allocated per calendar year, plus up to 10 days of paid sick leave.", "BALANCE_INQUIRY", null);
        }

        if (lower.contains("policy") || lower.contains("rules") || lower.contains("guideline")) {
            return new AiChatResponse("Company Leave Guidelines: 1) Leaves exceeding 2 days require 3 days advance notice. 2) Sick leaves must be reported by 9:30 AM. 3) Maximum 5 days may carry over to the next year.", "POLICY_QUESTION", null);
        }

        return new AiChatResponse("I can help you apply for leaves, check team coverage, or verify leave balances. Try saying: 'Book sick leave tomorrow' or 'Check my leave balance'.", "GENERAL", null);
    }

    @Override
    public AiImpactResponse analyzeImpact(Long leaveId) {
        Optional<Leave> opt = leaveRepository.findById(leaveId);
        if (opt.isEmpty()) {
            return new AiImpactResponse("UNKNOWN", 100, 0, new ArrayList<>(), "Leave record not found.", "N/A");
        }

        Leave target = opt.get();
        List<Leave> all = leaveRepository.findAll();
        List<String> overlappingPeers = new ArrayList<>();

        LocalDate targetStart;
        LocalDate targetEnd;
        try {
            targetStart = LocalDate.parse(target.getStartDate());
            targetEnd = LocalDate.parse(target.getEndDate());
        } catch (Exception e) {
            targetStart = LocalDate.now();
            targetEnd = LocalDate.now().plusDays(1);
        }

        for (Leave other : all) {
            if (other.getId().equals(target.getId()) || "Rejected".equalsIgnoreCase(other.getStatus())) {
                continue;
            }
            try {
                LocalDate oStart = LocalDate.parse(other.getStartDate());
                LocalDate oEnd = LocalDate.parse(other.getEndDate());
                if (!targetStart.isAfter(oEnd) && !targetEnd.isBefore(oStart)) {
                    overlappingPeers.add(other.getEmployeeName());
                }
            } catch (Exception ignored) {
            }
        }

        long duration;
        if (target.getDaysCount() != null && target.getDaysCount() > 0) {
            duration = target.getDaysCount();
        } else {
            long workDays = 0;
            LocalDate curr = targetStart;
            while (!curr.isAfter(targetEnd)) {
                DayOfWeek dow = curr.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    workDays++;
                }
                curr = curr.plusDays(1);
            }
            duration = Math.max(1, workDays);
        }
        int overlappingCount = overlappingPeers.size();

        String riskLevel = "LOW";
        int capacity = 100 - (overlappingCount * 15);
        if (capacity < 40) capacity = 40;

        String recommendation;
        String summary;

        if (overlappingCount >= 2 || duration > 5) {
            riskLevel = "HIGH";
            recommendation = "High team workload risk: Multiple colleagues are already scheduled away during this window (" + String.join(", ", overlappingPeers) + "). Recommend confirming team coverage before approving.";
            summary = String.format("%d colleague(s) overlapping. Team capacity at ~%d%%.", overlappingCount, capacity);
        } else if (overlappingCount == 1 || duration > 3) {
            riskLevel = "MEDIUM";
            recommendation = "Moderate workload impact: 1 colleague (" + overlappingPeers.get(0) + ") has overlapping leave. Team capacity at ~" + capacity + "%. Safe if critical deadlines are not set.";
            summary = String.format("1 colleague overlapping. Team capacity at ~%d%%.", capacity);
        } else {
            riskLevel = "LOW";
            recommendation = "Minimal operational impact: No schedule conflicts detected. Duration is " + duration + " day(s). Recommended for approval.";
            summary = "No conflicting peer leaves detected. Full team bandwidth available.";
        }

        return new AiImpactResponse(riskLevel, capacity, overlappingCount, overlappingPeers, recommendation, summary);
    }

    @Override
    public String generateOutOfOffice(Long leaveId) {
        Optional<Leave> opt = leaveRepository.findById(leaveId);
        if (opt.isEmpty()) {
            return "Leave not found.";
        }
        Leave leave = opt.get();
        return String.format(
                "Subject: Out of Office: %s (%s to %s)\n\n" +
                "Hello,\n\n" +
                "Thank you for contacting me. I am out of the office on %s from %s through %s.\n\n" +
                "I will have limited access to email during this period. For any urgent inquiries, please reach out to our team lead or department manager.\n\n" +
                "Kind regards,\n%s\nHR Leave Tracker Notification",
                leave.getEmployeeName(), leave.getStartDate(), leave.getEndDate(),
                leave.getLeaveType() != null ? leave.getLeaveType() : "leave",
                leave.getStartDate(), leave.getEndDate(),
                leave.getEmployeeName()
        );
    }
}
