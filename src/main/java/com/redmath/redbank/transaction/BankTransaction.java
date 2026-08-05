package com.redmath.redbank.transaction;

import com.redmath.redbank.account.AccountHolder;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "transaction_reference", nullable = false, unique = true, length = 64, updatable = false)
  private String transactionReference;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_account_holder_id")
  private AccountHolder sourceAccountHolder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_account_holder_id")
  private AccountHolder destinationAccountHolder;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20, updatable = false)
  private TransactionType type;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TransactionStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;
}
