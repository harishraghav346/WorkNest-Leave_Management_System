package com.examly.springapp.service;

import com.examly.springapp.model.Notification;

import java.util.List;

public interface NotificationService {
  Notification createNotification(String message);
  Notification createNotification(Long organizationId, String message);
  Notification createNotification(Long organizationId, String recipientUsername, String message);
  List<Notification> getNotifications();
  List<Notification> getNotifications(Long organizationId);
  List<Notification> getNotificationsForUser(Long organizationId, String username, boolean hasAdminPrivileges);
}