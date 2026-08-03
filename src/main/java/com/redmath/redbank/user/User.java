package com.redmath.redbank.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Builder
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "phone_number", nullable = false, unique = true, length = 20)
  private String phoneNumber;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "refresh_token_version", nullable = false)
  private long refreshTokenVersion;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 500)
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserStatus status;

  @Column(name = "oauth_provider", length = 50)
  private String oauthProvider;

  @Column(name = "oauth_provider_id", length = 255)
  private String oauthProviderId;

  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_user_id")
  private User approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public void incrementRefreshTokenVersion(Instant now) {
    this.refreshTokenVersion++;
    this.updatedAt = now;
  }
}