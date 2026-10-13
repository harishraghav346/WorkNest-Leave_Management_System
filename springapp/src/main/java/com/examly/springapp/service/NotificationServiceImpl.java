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
    Notification notification = new Notification();
    notification.setMessage(message);
    notification.setDate(LocalDateTime.now());
    notification.setRead(false);
    return notificationRepository.save(notification);
  }

  @Override
  public List<Notification> getNotifications() {
    return notificationRepository.findByOrderByDateDesc();
  }
}