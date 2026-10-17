package com.examly.springapp.service;

import com.examly.springapp.model.EmailDeliveryResult;

/**
 * Service contract for sending transactional emails (Organization Verification, Employee Invitations).
 */
public interface EmailDeliveryService {

    /**
     * Sends an organization verification email with a hardened URL fragment link.
     *
     * @param recipientEmail destination administrator email
     * @param organizationName organization display name
     * @param rawToken raw verification token (transmitted exclusively via secure email, never logged)
     * @return delivery result
     */
    EmailDeliveryResult sendOrganizationVerificationEmail(String recipientEmail, String organizationName, String rawToken);

    /**
     * Sends an employee invitation email with a hardened URL fragment link.
     *
     * @param recipientEmail destination employee email
     * @param organizationName organization display name
     * @param role assigned tenant role (e.g. EMPLOYEE, MANAGER)
     * @param rawToken raw invitation token (transmitted exclusively via secure email, never logged)
     * @return delivery result
     */
    EmailDeliveryResult sendEmployeeInvitationEmail(String recipientEmail, String organizationName, String role, String rawToken);

    /**
     * Indicates whether the underlying SMTP mail transport is configured and operational.
     */
    boolean isMailTransportConfigured();
}
