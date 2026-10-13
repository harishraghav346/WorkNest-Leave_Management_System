package com.examly.springapp.model;

public class AiChatRequest {
    private String message;
    private String employeeName;

    public AiChatRequest() {
    }

    public AiChatRequest(String message, String employeeName) {
        this.message = message;
        this.employeeName = employeeName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
