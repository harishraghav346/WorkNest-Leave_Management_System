package com.examly.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String message;
  private LocalDateTime date;
  @Column(name = "is_read")
  private boolean read;

  @Column(name = "organization_id")
  private Long organizationId;

  @Column(name = "recipient_username")
  private String recipientUsername;

  public Notification() {}

  public Notification(Long organizationId, String recipientUsername, String message) {
    this.organizationId = organizationId;
    this.recipientUsername = recipientUsername;
    this.message = message;
    this.date = LocalDateTime.now();
    this.read = false;
  }

  // Getters and Setters
  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public String getRecipientUsername() {
    return recipientUsername;
  }

  public void setRecipientUsername(String recipientUsername) {
    this.recipientUsername = recipientUsername;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }
}