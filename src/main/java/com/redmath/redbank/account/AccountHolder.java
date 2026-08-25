package com.redmath.redbank.account;

import com.redmath.redbank.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account_holders")
public class AccountHolder {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_holders_seq")
  @SequenceGenerator(name = "account_holders_seq", sequenceName = "account_holders_id_seq", allocationSize = 50)
  private Long id;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @NotNull
  @Column(name = "account_number", nullable = false, unique = true, length = 50)
  private String accountNumber;

  @NotNull
  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", nullable = false, length = 20)
  private AccountStatus accountStatus;

  @NotNull
  @Column(name = "approved_at", nullable = false)
  private OffsetDateTime approvedAt;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}