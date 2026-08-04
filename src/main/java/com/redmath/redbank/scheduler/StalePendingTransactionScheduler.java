package com.redmath.redbank.scheduler;

import com.redmath.redbank.transaction.BankTransactionRepository;
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

  private static final int PENDING_TIMEOUT_MINUTES = 30;

  private final BankTransactionRepository bankTransactionRepository;

  @Scheduled(cron = "${scheduler.stale-transaction.cron:0 */15 * * * *}")
  @Transactional
  public void cancelStalePendingTransactions() {
    log.info("Stale pending transaction cleanup job started");

    OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC)
        .minusMinutes(PENDING_TIMEOUT_MINUTES);

    int[] count = {0};
    bankTransactionRepository.findAllByStatusAndCreatedAtBefore(TransactionStatus.PENDING, cutoff)
        .forEach(transaction -> {
          transaction.setStatus(TransactionStatus.CANCELLED);
          transaction.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
          count[0]++;
          log.warn("Auto-cancelled stale transaction: ref={}, createdAt={}",
              transaction.getTransactionReference(), transaction.getCreatedAt());
        });

    log.info("Stale pending transaction cleanup completed: {} transactions cancelled", count[0]);
  }
}
