package com.examly.springapp.service;

import com.examly.springapp.model.Notification;

import java.util.List;

public interface NotificationService {
  Notification createNotification(String message);
  List<Notification> getNotifications();
}