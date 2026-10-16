package com.examly.springapp.service;

import com.examly.springapp.model.Notification;
import com.examly.springapp.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

  @Autowired
  private NotificationRepository notificationRepository;

  @Override
  public Notification createNotification(String message) {
    return createNotification(null, null, message);
  }

  @Override
  public Notification createNotification(Long organizationId, String message) {
    return createNotification(organizationId, null, message);
  }

  @Override
  public Notification createNotification(Long organizationId, String recipientUsername, String message) {
    Notification notification = new Notification();
    notification.setOrganizationId(organizationId);
    notification.setRecipientUsername(recipientUsername);
    notification.setMessage(message);
    notification.setDate(LocalDateTime.now());
    notification.setRead(false);
    return notificationRepository.save(notification);
  }

  @Override
  public List<Notification> getNotifications() {
    return notificationRepository.findByOrganizationIdIsNullOrderByDateDesc();
  }

  @Override
  public List<Notification> getNotifications(Long organizationId) {
    if (organizationId == null) {
      return notificationRepository.findByOrganizationIdIsNullOrderByDateDesc();
    }
    return notificationRepository.findByOrganizationIdOrderByDateDesc(organizationId);
  }

  @Override
  public List<Notification> getNotificationsForUser(Long organizationId, String username, boolean hasAdminPrivileges) {
    if (organizationId == null) {
      return notificationRepository.findByOrganizationIdIsNullOrderByDateDesc();
    }
    if (hasAdminPrivileges) {
      return notificationRepository.findByOrganizationIdOrderByDateDesc(organizationId);
    }
    return notificationRepository.findVisibleForUser(organizationId, username);
  }
}