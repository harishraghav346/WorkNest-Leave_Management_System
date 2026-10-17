package com.examly.springapp.service;

import com.examly.springapp.model.EmailDeliveryResult;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailDeliveryServiceImpl implements EmailDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(EmailDeliveryServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.mail.from:noreply@worknest.io}")
    private String mailFrom;

    @Value("${app.mail.from-name:WorkNest}")
    private String mailFromName;

    @Override
    public boolean isMailTransportConfigured() {
        return mailSender != null && mailHost != null && !mailHost.trim().isEmpty();
    }

    @Override
    public EmailDeliveryResult sendOrganizationVerificationEmail(String recipientEmail, String organizationName, String rawToken) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required for organization verification delivery.");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Verification token is required.");
        }

        String safeOrgName = HtmlUtils.htmlEscape(organizationName != null ? organizationName : "Your Organization");
        String cleanBaseUrl = sanitizeBaseUrl(baseUrl);
        String verificationUrl = cleanBaseUrl + "/verify-organization#token=" + rawToken;

        String subject = "Verify your WorkNest organization: " + (organizationName != null ? organizationName : "Workspace");

        String textContent = "Welcome to WorkNest!\n\n"
                + "Please verify your organization workspace by visiting the following link:\n"
                + verificationUrl + "\n\n"
                + "This verification link is valid for 24 hours and can only be used once.\n\n"
                + "If you did not initiate this registration, please ignore this email.\n";

        String htmlContent = "<!DOCTYPE html><html><body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 24px;\">"
                + "<div style=\"background: #2563eb; color: #ffffff; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;\">"
                + "<h1 style=\"margin: 0; font-size: 24px;\">WorkNest</h1>"
                + "<p style=\"margin: 4px 0 0 0; opacity: 0.9; font-size: 14px;\">Organization Workspace Verification</p>"
                + "</div>"
                + "<div style=\"background: #ffffff; border: 1px solid #e2e8f0; border-top: none; padding: 24px; border-radius: 0 0 8px 8px;\">"
                + "<h2 style=\"margin-top: 0; color: #0f172a; font-size: 18px;\">Activate your workspace for " + safeOrgName + "</h2>"
                + "<p>Thank you for creating an organization on WorkNest. Click the button below to confirm your email and activate your company workspace:</p>"
                + "<div style=\"text-align: center; margin: 28px 0;\">"
                + "<a href=\"" + verificationUrl + "\" style=\"background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; display: inline-block;\">Verify Organization</a>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b;\">Or paste this link into your browser:<br/><code style=\"background: #f1f5f9; padding: 4px 8px; border-radius: 4px; word-break: break-all;\">" + verificationUrl + "</code></p>"
                + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\"/>"
                + "<p style=\"font-size: 12px; color: #94a3b8;\">This link expires in 24 hours. If you did not create this account, no further action is needed.</p>"
                + "</div></body></html>";

        return dispatchEmail(recipientEmail, subject, textContent, htmlContent);
    }

    @Override
    public EmailDeliveryResult sendEmployeeInvitationEmail(String recipientEmail, String organizationName, String role, String rawToken) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required for invitation delivery.");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invitation token is required.");
        }

        String safeOrgName = HtmlUtils.htmlEscape(organizationName != null ? organizationName : "WorkNest Workspace");
        String safeRole = HtmlUtils.htmlEscape(role != null ? role : "EMPLOYEE");
        String cleanBaseUrl = sanitizeBaseUrl(baseUrl);
        String activationUrl = cleanBaseUrl + "/activate#token=" + rawToken;

        String subject = "You're invited to join " + (organizationName != null ? organizationName : "WorkNest");

        String textContent = "Hello,\n\n"
                + "You have been invited to join " + organizationName + " on WorkNest as a " + safeRole + ".\n\n"
                + "To accept your invitation and activate your account, please visit:\n"
                + activationUrl + "\n\n"
                + "This invitation link is valid for 24 hours.\n";

        String htmlContent = "<!DOCTYPE html><html><body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 24px;\">"
                + "<div style=\"background: #2563eb; color: #ffffff; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;\">"
                + "<h1 style=\"margin: 0; font-size: 24px;\">WorkNest</h1>"
                + "<p style=\"margin: 4px 0 0 0; opacity: 0.9; font-size: 14px;\">Team Member Invitation</p>"
                + "</div>"
                + "<div style=\"background: #ffffff; border: 1px solid #e2e8f0; border-top: none; padding: 24px; border-radius: 0 0 8px 8px;\">"
                + "<h2 style=\"margin-top: 0; color: #0f172a; font-size: 18px;\">Join " + safeOrgName + "</h2>"
                + "<p>You have been invited to join <strong>" + safeOrgName + "</strong> with the role of <strong>" + safeRole + "</strong>.</p>"
                + "<p>Click the button below to set up your password and complete your profile:</p>"
                + "<div style=\"text-align: center; margin: 28px 0;\">"
                + "<a href=\"" + activationUrl + "\" style=\"background: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; display: inline-block;\">Activate Account</a>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b;\">Or paste this link into your browser:<br/><code style=\"background: #f1f5f9; padding: 4px 8px; border-radius: 4px; word-break: break-all;\">" + activationUrl + "</code></p>"
                + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\"/>"
                + "<p style=\"font-size: 12px; color: #94a3b8;\">This invitation expires in 24 hours.</p>"
                + "</div></body></html>";

        return dispatchEmail(recipientEmail, subject, textContent, htmlContent);
    }

    private EmailDeliveryResult dispatchEmail(String recipientEmail, String subject, String textContent, String htmlContent) {
        String redactedEmail = redactEmail(recipientEmail);

        if (!isMailTransportConfigured()) {
            logger.info("Transactional email accepted: subject='{}', recipient='{}' [SMTP transport not configured; delivery skipped]",
                    subject, redactedEmail);
            return EmailDeliveryResult.skippedNoTransport(recipientEmail, subject);
        }

        // Bounded retry for transient transport errors (up to 2 attempts)
        int maxAttempts = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                helper.setFrom(mailFrom, mailFromName);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(textContent, htmlContent);

                mailSender.send(message);

                logger.info("Transactional email successfully sent: subject='{}', recipient='{}', attempt={}",
                        subject, redactedEmail, attempt);
                return EmailDeliveryResult.delivered(recipientEmail, subject);
            } catch (Exception ex) {
                lastException = ex;
                logger.warn("Transient failure sending transactional email (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.error("Failed to deliver transactional email to '{}' after {} attempts: {}",
                redactedEmail, maxAttempts, lastException != null ? lastException.getMessage() : "Unknown error");
        return EmailDeliveryResult.failed(recipientEmail, subject,
                "Email delivery failed: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private String sanitizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.trim().replaceAll("/+$", "");
    }

    public static String redactEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    // Setters for test injection
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
