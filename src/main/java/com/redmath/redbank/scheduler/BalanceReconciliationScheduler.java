package com.redmath.redbank.scheduler;

import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceReconciliationScheduler {

  private final BalanceRepository balanceRepository;

  @Scheduled(cron = "${scheduler.reconciliation.cron:0 0 0 * * *}")
  @Transactional(readOnly = true)
  public void reconcileBalances() {
    log.info("Balance reconciliation job started");

    List<Balance> allEntries = balanceRepository.findAll();

    Map<Long, List<Balance>> entriesByAccount = allEntries.stream()
        .collect(Collectors.groupingBy(b -> b.getAccountHolder().getId()));

    entriesByAccount.forEach(this::reconcileAccount);

    if (log.isInfoEnabled()) {
      log.info("Balance reconciliation job completed for {} accounts", entriesByAccount.size());
    }
  }

  private void reconcileAccount(Long accountHolderId, List<Balance> entries) {
    BigDecimal expectedBalance = entries.stream()
        .map(entry -> entry.getIndicator() == BalanceIndicator.CREDIT
            ? entry.getAmount()
            : entry.getAmount().negate())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal latestRunningBalance = entries.stream()
        .max(Comparator.comparingLong(Balance::getId))
        .map(Balance::getRunningBalance)
        .orElse(BigDecimal.ZERO);

    if (expectedBalance.compareTo(latestRunningBalance) != 0) {
      log.error(
          "Balance mismatch detected for accountHolderId={}: expected={}, recorded={}",
          accountHolderId, expectedBalance, latestRunningBalance
      );
    } else {
      log.debug("Balance OK for accountHolderId={}: balance={}", accountHolderId,
          latestRunningBalance);
    }
  }
}
