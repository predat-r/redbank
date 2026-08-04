package com.redmath.redbank.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  public void changePasswordHash(String passwordHash, Instant now) {
    this.passwordHash = passwordHash;
    this.updatedAt = now;
    this.refreshTokenVersion++;
  }

  public void rejectRegistration(String rejectionReason, Instant now) {
    this.status = UserStatus.REJECTED;
    this.rejectionReason = rejectionReason;
    this.approvedBy = null;
    this.approvedAt = null;
    this.refreshTokenVersion++;
    this.updatedAt = now;
  }

  public void approveRegistration(User admin, Instant now) {
    this.status = UserStatus.ACTIVE;
    this.rejectionReason = null;
    this.approvedBy = admin;
    this.approvedAt = now;
    this.updatedAt = now;
  }

  public void activate(Instant now) {
    this.status = UserStatus.ACTIVE;
    this.updatedAt = now;
  }

  public void deactivate(Instant now) {
    this.status = UserStatus.DEACTIVATED;
    this.refreshTokenVersion++;
    this.updatedAt = now;
  }
}