package com.examly.springapp.model;

import java.util.List;

public class AiImpactResponse {
    private String riskLevel; // "LOW", "MEDIUM", "HIGH"
    private int estimatedCapacityPercent;
    private int overlappingCount;
    private List<String> overlappingEmployees;
    private String recommendation;
    private String summary;

    public AiImpactResponse() {
    }

    public AiImpactResponse(String riskLevel, int estimatedCapacityPercent, int overlappingCount, List<String> overlappingEmployees, String recommendation, String summary) {
        this.riskLevel = riskLevel;
        this.estimatedCapacityPercent = estimatedCapacityPercent;
        this.overlappingCount = overlappingCount;
        this.overlappingEmployees = overlappingEmployees;
        this.recommendation = recommendation;
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getEstimatedCapacityPercent() {
        return estimatedCapacityPercent;
    }

    public void setEstimatedCapacityPercent(int estimatedCapacityPercent) {
        this.estimatedCapacityPercent = estimatedCapacityPercent;
    }

    public int getOverlappingCount() {
        return overlappingCount;
    }

    public void setOverlappingCount(int overlappingCount) {
        this.overlappingCount = overlappingCount;
    }

    public List<String> getOverlappingEmployees() {
        return overlappingEmployees;
    }

    public void setOverlappingEmployees(List<String> overlappingEmployees) {
        this.overlappingEmployees = overlappingEmployees;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
