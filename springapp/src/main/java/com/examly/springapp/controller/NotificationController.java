package com.examly.springapp.controller;

import com.examly.springapp.model.Notification;
import com.examly.springapp.model.TenantContext;
import com.examly.springapp.service.NotificationService;
import com.examly.springapp.service.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private TenantContextService tenantContextService;

  @GetMapping
  public List<Notification> getNotifications(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return java.util.Collections.emptyList();
    }

    Optional<TenantContext> contextOpt = tenantContextService.resolveTenantContext(authentication);
    if (contextOpt.isPresent()) {
      TenantContext context = contextOpt.get();
      return notificationService.getNotificationsForUser(
          context.getOrganizationId(),
          authentication.getName(),
          context.hasAdminOrHrPrivileges()
      );
    }

    // Missing tenant context
    if (authentication.getAuthorities() != null) {
      boolean isPlatformAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PLATFORM_ADMIN"));
      if (isPlatformAdmin) {
        return java.util.Collections.emptyList();
      }
      boolean isLegacyAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
      if (isLegacyAdmin) {
        return notificationService.getNotifications();
      }
    }

    return java.util.Collections.emptyList();
  }
}