package com.redmath.redbank.balance;

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
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "balance")
public class Balance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_holder_id", nullable = false)
  private AccountHolder accountHolder;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transaction_id", nullable = false)
  private BankTransaction transaction;

  @NotNull
  @Column(name = "entry_date", nullable = false)
  private OffsetDateTime entryDate;

  @NotNull
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "indicator", nullable = false, length = 10)
  private BalanceIndicator indicator;

  @NotNull
  @Column(name = "running_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal runningBalance;

  public enum BalanceIndicator {
    DEBIT,
    CREDIT
  }
}



