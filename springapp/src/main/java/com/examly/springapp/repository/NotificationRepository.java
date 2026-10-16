package com.examly.springapp.repository;

import com.examly.springapp.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByOrderByDateDesc();

  List<Notification> findByOrganizationIdOrderByDateDesc(Long organizationId);

  @Query("SELECT n FROM Notification n WHERE n.organizationId = :organizationId AND (n.recipientUsername = :recipientUsername OR n.recipientUsername IS NULL) ORDER BY n.date DESC")
  List<Notification> findVisibleForUser(@Param("organizationId") Long organizationId, @Param("recipientUsername") String recipientUsername);

  Optional<Notification> findByIdAndOrganizationId(Long id, Long organizationId);

  List<Notification> findByOrganizationIdIsNullOrderByDateDesc();

  List<Notification> findByOrganizationIdIsNull();
}