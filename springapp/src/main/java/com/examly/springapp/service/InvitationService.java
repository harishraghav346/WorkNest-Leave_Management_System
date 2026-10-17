package com.examly.springapp.service;

import com.examly.springapp.model.ActivationRequest;
import com.examly.springapp.model.AuthResponse;
import com.examly.springapp.model.EmployeeInvitation;
import com.examly.springapp.model.InvitationRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface InvitationService {
    EmployeeInvitation createInvitation(InvitationRequest request, Authentication authentication, String clientIp);
    List<EmployeeInvitation> getInvitations(Authentication authentication);
    AuthResponse acceptInvitation(ActivationRequest request, String clientIp);
}
