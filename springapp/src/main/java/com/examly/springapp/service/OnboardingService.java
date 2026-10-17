package com.examly.springapp.service;

import com.examly.springapp.model.AuthResponse;
import com.examly.springapp.model.OnboardingRequest;
import com.examly.springapp.model.OrganizationVerificationRequest;
import com.examly.springapp.model.ResendVerificationRequest;

public interface OnboardingService {
    AuthResponse onboardOrganization(OnboardingRequest request, String clientIp);
    AuthResponse verifyOrganization(OrganizationVerificationRequest request, String clientIp);
    void resendVerification(ResendVerificationRequest request, String clientIp);
}
