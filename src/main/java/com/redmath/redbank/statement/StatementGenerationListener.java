package com.redmath.redbank.statement;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.statement.dto.StatementData;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StatementGenerationListener {

  private static final Logger log = LoggerFactory.getLogger(StatementGenerationListener.class);

  private final AccountHolderRepository accountHolderRepository;
  private final BalanceRepository balanceRepository;
  private final StatementPdfGenerator pdfGenerator;
  private final StatementEmailService emailService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public StatementGenerationListener(
      AccountHolderRepository accountHolderRepository,
      BalanceRepository balanceRepository,
      StatementPdfGenerator pdfGenerator,
      StatementEmailService emailService) {
    this.accountHolderRepository = accountHolderRepository;
    this.balanceRepository = balanceRepository;
    this.pdfGenerator = pdfGenerator;
    this.emailService = emailService;
  }

  @Async("statementTaskExecutor")
  @EventListener
  @Transactional(readOnly = true)
  public void handleStatementRequested(StatementRequestedEvent event) {
    try {
      AccountHolder accountHolder = accountHolderRepository.findById(event.accountHolderId())
          .orElseThrow(() -> new IllegalStateException("Account holder not found"));

      OffsetDateTime startOfDay = event.fromDate().atStartOfDay().atOffset(ZoneOffset.UTC);
      OffsetDateTime endOfDay = event.toDate().atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

      BigDecimal openingBalance = balanceRepository
          .findTopByAccountHolderIdAndEntryDateLessThanOrderByEntryDateDescIdDesc(
              event.accountHolderId(), startOfDay)
          .map(Balance::getRunningBalance)
          .orElse(BigDecimal.ZERO);

      BigDecimal closingBalance = balanceRepository
          .findTopByAccountHolderIdAndEntryDateLessThanEqualOrderByEntryDateDescIdDesc(
              event.accountHolderId(), endOfDay)
          .map(Balance::getRunningBalance)
          .orElse(openingBalance);

      List<Balance> balances = balanceRepository
          .findByAccountHolderIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
              event.accountHolderId(), startOfDay, endOfDay);

      BigDecimal totalCredits = BigDecimal.ZERO;
      BigDecimal totalDebits = BigDecimal.ZERO;

      List<StatementData.StatementTransactionData> txData = new ArrayList<>();
      for (Balance b : balances) {
        if (b.getIndicator() == BalanceIndicator.CREDIT) {
          totalCredits = totalCredits.add(b.getAmount());
        } else {
          totalDebits = totalDebits.add(b.getAmount());
        }
        
        String counterpartyName = null;
        if (b.getTransaction().getDestinationAccountHolder() != null && b.getTransaction().getSourceAccountHolder() != null) {
          if (b.getTransaction().getSourceAccountHolder().getId().equals(accountHolder.getId())) {
             counterpartyName = b.getTransaction().getDestinationAccountHolder().getUser().getName();
          } else {
             counterpartyName = b.getTransaction().getSourceAccountHolder().getUser().getName();
          }
        }

        txData.add(StatementData.StatementTransactionData.builder()
            .dateTime(b.getEntryDate())
            .reference(b.getTransaction().getTransactionReference())
            .type(b.getTransaction().getType().name())
            .category(b.getTransaction().getCategory() != null ? b.getTransaction().getCategory().name() : "")
            .counterparty(counterpartyName)
            .status(b.getTransaction().getStatus().name())
            .amount(b.getIndicator() == BalanceIndicator.CREDIT ? b.getAmount() : b.getAmount().negate())
            .runningBalance(b.getRunningBalance())
            .build());
      }

      StatementData data = StatementData.builder()
          .accountHolderName(accountHolder.getUser().getName())
          .accountNumber(accountHolder.getAccountNumber())
          .address(accountHolder.getUser().getAddress())
          .currency("USD") // Assume USD or derive if app has currency
          .fromDate(event.fromDate())
          .toDate(event.toDate())
          .openingBalance(openingBalance)
          .closingBalance(closingBalance)
          .totalCredits(totalCredits)
          .totalDebits(totalDebits)
          .transactionCount(balances.size())
          .generationTimestamp(OffsetDateTime.now())
          .transactions(txData)
          .build();

      byte[] pdfBytes = pdfGenerator.generatePdf(data);

      emailService.sendStatement(accountHolder, pdfBytes, event.fromDate(), event.toDate());

    } catch (Exception e) {
      log.error("Failed to process StatementRequestedEvent for account holder {}", event.accountHolderId(), e);
    }
  }
}
