package com.redmath.redbank.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_logs_seq")
  @SequenceGenerator(name = "audit_logs_seq", sequenceName = "audit_logs_id_seq", allocationSize = 50)
  private Long id;

  @Column(name = "actor_user_id", nullable = false, updatable = false)
  private Long actorUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 100, updatable = false)
  private AuditAction action;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 50, updatable = false)
  private AuditTargetType targetType;

  @Column(name = "target_identifier", nullable = false, length = 100, updatable = false)
  private String targetIdentifier;

  @Column(length = 1000, updatable = false)
  private String details;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

}