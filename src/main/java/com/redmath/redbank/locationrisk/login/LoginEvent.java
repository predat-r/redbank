package com.redmath.redbank.locationrisk.login;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "login_events")
@Getter
@Setter
@NoArgsConstructor
public class LoginEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "login_events_seq")
  @SequenceGenerator(name = "login_events_seq", sequenceName = "login_events_id_seq", allocationSize = 50)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @NotBlank
  @Size(max = 45)
  @Column(name = "ip_address", nullable = false, length = 45)
  private String ipAddress;

  @Size(max = 1000)
  @Column(name = "user_agent", length = 1000)
  private String userAgent;

  @Size(max = 255)
  @Column(name = "device_identifier", length = 255)
  private String deviceIdentifier;

  @NotNull
  @Column(name = "successful", nullable = false)
  private Boolean successful;

  @Size(max = 255)
  @Column(name = "failure_reason", length = 255)
  private String failureReason;

  @Size(max = 150)
  @Column(name = "city", length = 150)
  private String city;

  @Size(max = 100)
  @Column(name = "country", length = 100)
  private String country;

  @Size(max = 255)
  @Column(name = "access_token_jti", length = 255)
  private String accessTokenJti;


  @NotNull
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;


}
