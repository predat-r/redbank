package com.redmath.redbank.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StalePendingTransactionSchedulerTest {

  @Mock
  private BankTransactionRepository bankTransactionRepository;

  @Mock
  private BankTransactionService bankTransactionService;

  private StalePendingTransactionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new StalePendingTransactionScheduler(bankTransactionRepository, bankTransactionService);
  }

  @Test
  @DisplayName("cancelStalePendingTransactions handles empty list")
  void cancelStalePendingTransactionsEmpty() {
    when(bankTransactionRepository.findAllByStatusAndCreatedAtBefore(eq(TransactionStatus.PENDING), any(OffsetDateTime.class)))
        .thenReturn(List.of());

    scheduler.cancelStalePendingTransactions();

    verify(bankTransactionRepository).findAllByStatusAndCreatedAtBefore(eq(TransactionStatus.PENDING), any(OffsetDateTime.class));
  }

  @Test
  @DisplayName("cancelStalePendingTransactions cancels stale pending transactions")
  void cancelStalePendingTransactionsCancelsStale() {
    BankTransaction staleTxn = new BankTransaction();
    staleTxn.setId(1L);
    staleTxn.setTransactionReference("TXN-STALE-001");
    staleTxn.setType(TransactionType.TRANSFER);
    staleTxn.setAmount(new BigDecimal("100.00"));
    staleTxn.setStatus(TransactionStatus.PENDING);
    staleTxn.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(25));

    when(bankTransactionRepository.findAllByStatusAndCreatedAtBefore(eq(TransactionStatus.PENDING), any(OffsetDateTime.class)))
        .thenReturn(List.of(staleTxn));

    scheduler.cancelStalePendingTransactions();

    verify(bankTransactionService).reverseTransaction(null, 1L,
        "Auto-cancelled stale pending transaction after timeout");
  }
}
