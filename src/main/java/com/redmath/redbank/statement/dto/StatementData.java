package com.redmath.redbank.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StatementData {

  private String accountHolderName;
  private String accountNumber;
  private String address;
  private String currency;
  private LocalDate fromDate;
  private LocalDate toDate;

  private BigDecimal openingBalance;
  private BigDecimal closingBalance;
  private BigDecimal totalCredits;
  private BigDecimal totalDebits;
  private int transactionCount;

  private OffsetDateTime generationTimestamp;

  private List<StatementTransactionData> transactions;

  @Getter
  @Setter
  @Builder
  public static class StatementTransactionData {

    private OffsetDateTime dateTime;
    private String reference;
    private String type;
    private String category;
    private String counterparty;
    private String status;
    private BigDecimal amount;
    private BigDecimal runningBalance;
  }
}
