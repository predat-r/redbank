package com.redmath.redbank.statement;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.statement.dto.StatementData;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatementProcessorService {

  private final AccountHolderRepository accountHolderRepository;
  private final BalanceRepository balanceRepository;
  private final BankTransactionRepository bankTransactionRepository;
  private final StatementPdfGenerator pdfGenerator;
  private final StatementEmailService emailService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public StatementProcessorService(
      AccountHolderRepository accountHolderRepository,
      BalanceRepository balanceRepository,
      BankTransactionRepository bankTransactionRepository,
      StatementPdfGenerator pdfGenerator,
      StatementEmailService emailService) {
    this.accountHolderRepository = accountHolderRepository;
    this.balanceRepository = balanceRepository;
    this.bankTransactionRepository = bankTransactionRepository;
    this.pdfGenerator = pdfGenerator;
    this.emailService = emailService;
  }

  @Transactional(readOnly = true)
  public void processStatementRequest(StatementRequestedEvent event) {
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

    List<Long> txIds = balances.stream()
        .map(Balance::getTransactionId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();

    java.util.Map<Long, BankTransaction> transactionMap = txIds.isEmpty()
        ? java.util.Collections.emptyMap()
        : bankTransactionRepository.findAllById(txIds).stream()
            .collect(java.util.stream.Collectors.toMap(BankTransaction::getId,
                java.util.function.Function.identity()));

    BigDecimal totalCredits = BigDecimal.ZERO;
    BigDecimal totalDebits = BigDecimal.ZERO;

    List<StatementData.StatementTransactionData> txData = new ArrayList<>();
    for (Balance b : balances) {
      BankTransaction transaction = b.getTransactionId() != null
          ? transactionMap.get(b.getTransactionId())
          : null;

      if (b.getIndicator() == BalanceIndicator.CREDIT) {
        totalCredits = totalCredits.add(b.getAmount());
      } else {
        totalDebits = totalDebits.add(b.getAmount());
      }

      String counterpartyName = null;
      if (transaction != null && transaction.getDestinationAccountHolder() != null
          && transaction.getSourceAccountHolder() != null) {
        if (transaction.getSourceAccountHolder().getId().equals(accountHolder.getId())) {
          counterpartyName = transaction.getDestinationAccountHolder().getUser().getName();
        } else {
          counterpartyName = transaction.getSourceAccountHolder().getUser().getName();
        }
      }

      txData.add(StatementData.StatementTransactionData.builder()
          .dateTime(b.getEntryDate())
          .reference(transaction != null ? transaction.getTransactionReference() : "")
          .type(transaction != null ? transaction.getType().name() : "")
          .category(
              transaction != null && transaction.getCategory() != null ? transaction.getCategory()
                  .name() : "")
          .counterparty(counterpartyName)
          .status(transaction != null ? transaction.getStatus().name() : "")
          .amount(b.getIndicator() == BalanceIndicator.CREDIT ? b.getAmount()
              : b.getAmount().negate())
          .runningBalance(b.getRunningBalance())
          .build());
    }

    StatementData data = StatementData.builder()
        .accountHolderName(accountHolder.getUser().getName())
        .accountNumber(accountHolder.getAccountNumber())
        .address(accountHolder.getUser().getAddress())
        .currency("USD")
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
  }
}
