package com.redmath.redbank.chatbot.query;

import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class BalanceQueryService {

  private final BalanceRepository balanceRepository;

  public BalanceQueryService(BalanceRepository balanceRepository) {
    this.balanceRepository = balanceRepository;
  }

  public Optional<BigDecimal> getBalanceAsOf(Long accountHolderId, LocalDate asOfDate) {
    OffsetDateTime cutoff = asOfDate.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
    return balanceRepository
        .findTopByAccountHolderIdAndEntryDateLessThanEqualOrderByEntryDateDesc(accountHolderId, cutoff)
        .map(Balance::getRunningBalance);
  }

  /** Simple projection: current balance + (avg daily net flow over last 30 days * days remaining in month). */
  public BigDecimal projectMonthEndBalance(Long accountHolderId) {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime thirtyDaysAgo = now.minusDays(30);

    var recentEntries = balanceRepository.findByAccountHolderIdAndEntryDateAfter(accountHolderId, thirtyDaysAgo);

    BigDecimal currentBalance = balanceRepository
        .findTopByAccountHolderIdOrderByEntryDateDesc(accountHolderId)
        .map(Balance::getRunningBalance)
        .orElse(BigDecimal.ZERO);

    if (recentEntries.isEmpty()) return currentBalance; // not enough history to project

    BigDecimal netFlow = recentEntries.stream()
        .map(e -> e.getIndicator().name().equals("CREDIT") ? e.getAmount() : e.getAmount().negate())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal avgDailyNetFlow = netFlow.divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);

    int daysRemaining = now.toLocalDate().lengthOfMonth() - now.toLocalDate().getDayOfMonth();
    return currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(daysRemaining)));
  }
}