package com.examly.springapp.model;

import java.time.Instant;

/**
 * Result of an email delivery attempt.
 * Distinguishes between "accepted for delivery" and "physically delivered" via transport.
 */
public class EmailDeliveryResult {

    private boolean accepted;
    private boolean delivered;
    private String recipient;
    private String subject;
    private String deliveryStatus; // DELIVERED, ACCEPTED_QUEUED, FAILED_TRANSIENT, FAILED_PERMANENT, SKIPPED_NO_TRANSPORT
    private String message;
    private int attemptCount;
    private Instant timestamp;

    public EmailDeliveryResult() {
        this.timestamp = Instant.now();
    }

    public EmailDeliveryResult(boolean accepted, boolean delivered, String recipient, String subject, String deliveryStatus, String message) {
        this.accepted = accepted;
        this.delivered = delivered;
        this.recipient = recipient;
        this.subject = subject;
        this.deliveryStatus = deliveryStatus;
        this.message = message;
        this.attemptCount = 1;
        this.timestamp = Instant.now();
    }

    public static EmailDeliveryResult delivered(String recipient, String subject) {
        return new EmailDeliveryResult(true, true, recipient, subject, "DELIVERED", "Email delivered successfully via SMTP transport.");
    }

    public static EmailDeliveryResult accepted(String recipient, String subject, String message) {
        return new EmailDeliveryResult(true, false, recipient, subject, "ACCEPTED_QUEUED", message);
    }

    public static EmailDeliveryResult skippedNoTransport(String recipient, String subject) {
        return new EmailDeliveryResult(true, false, recipient, subject, "SKIPPED_NO_TRANSPORT",
                "SMTP transport is not configured (spring.mail.host is empty). Email delivery was recorded but skipped.");
    }

    public static EmailDeliveryResult failed(String recipient, String subject, String errorReason) {
        return new EmailDeliveryResult(false, false, recipient, subject, "FAILED_PERMANENT", errorReason);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
