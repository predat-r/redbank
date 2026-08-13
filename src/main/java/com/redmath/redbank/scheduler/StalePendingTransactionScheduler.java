package com.redmath.redbank.scheduler;

import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.TransactionStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StalePendingTransactionScheduler {

  private static final int PENDING_TIMEOUT_HOURS = 24;

  private final BankTransactionRepository bankTransactionRepository;
  private final BankTransactionService bankTransactionService;

  @Scheduled(cron = "${scheduler.stale-transaction.cron:0 */15 * * * *}")
  @Transactional
  public void cancelStalePendingTransactions() {
    if (log.isInfoEnabled()) {
      log.info("Stale pending transaction cleanup job started");
    }

    OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC)
        .minusHours(PENDING_TIMEOUT_HOURS);

    int[] count = {0};
    bankTransactionRepository.findAllByStatusAndCreatedAtBefore(TransactionStatus.PENDING, cutoff)
        .forEach(transaction -> {
          try {
            bankTransactionService.reverseTransaction(null, transaction.getId(),
                "Auto-cancelled stale pending transaction after timeout");
            count[0]++;
            if (log.isWarnEnabled()) {
              log.warn("Auto-cancelled and reversed stale transaction: ref={}, createdAt={}",
                  transaction.getTransactionReference(), transaction.getCreatedAt());
            }
          } catch (Exception e) {
            if (log.isErrorEnabled()) {
              log.error("Failed to cancel stale transaction {}: {}",
                  transaction.getTransactionReference(), e.getMessage(), e);
            }
          }
        });

    if (log.isInfoEnabled()) {
      log.info("Stale pending transaction cleanup completed: {} transactions cancelled and reversed", count[0]);
    }
  }
}
