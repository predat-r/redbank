package com.redmath.redbank.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.common.exception.InsufficientFundsException;
import com.redmath.redbank.transaction.request.WithdrawalRequest;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BankTransactionConcurrencyTest {

  @Autowired
  private BankTransactionService bankTransactionService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  @Autowired
  private BalanceRepository balanceRepository;

  @Test
  @DisplayName("Concurrent withdrawals execute safely under PESSIMISTIC_WRITE lock without overdrawing account")
  void concurrentWithdrawalsDoNotOverdrawAccount() throws Exception {
    // 1. Setup User and AccountHolder with initial deposit of $100.00
    User user = userRepository.save(User.builder()
        .email("concurrent.user@example.com")
        .name("Concurrent User")
        .address("123 Parallel St")
        .phoneNumber("+12223334444")
        .passwordHash("hashedpass")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber("RB-CONCURRENCY-001");
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder = accountHolderRepository.save(accountHolder);

    // Initial Deposit Transaction of $100.00
    BankTransaction initTx = new BankTransaction();
    initTx.setTransactionReference("TXN-INIT-100");
    initTx.setType(TransactionType.DEPOSIT);
    initTx.setAmount(new BigDecimal("100.00"));
    initTx.setDestinationAccountHolder(accountHolder);
    initTx.setStatus(TransactionStatus.COMPLETED);
    initTx.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    initTx = bankTransactionRepository.save(initTx);

    Balance initBalance = new Balance();
    initBalance.setAccountHolder(accountHolder);
    initBalance.setTransaction(initTx);
    initBalance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    initBalance.setAmount(new BigDecimal("100.00"));
    initBalance.setIndicator(BalanceIndicator.CREDIT);
    initBalance.setRunningBalance(new BigDecimal("100.00"));
    balanceRepository.save(initBalance);

    // 2. Prepare 10 concurrent threads attempting to withdraw $20.00 each simultaneously
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failedCount = new AtomicInteger(0);

    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final Long userId = user.getId();
      futures.add(executorService.submit(() -> {
        readyLatch.countDown();
        try {
          startLatch.await(); // wait for release signal so all threads execute simultaneously
          WithdrawalRequest request = new WithdrawalRequest();
          request.setAmount(new BigDecimal("20.00"));
          request.setDescription("Concurrent withdrawal");

          bankTransactionService.withdraw(userId, request);
          successCount.incrementAndGet();
        } catch (InsufficientFundsException ex) {
          failedCount.incrementAndGet();
        } catch (Exception ex) {
          // If wrapped in RuntimeException or transaction rollback
          if (ex.getCause() instanceof InsufficientFundsException || ex.getMessage()
              .contains("Insufficient funds")) {
            failedCount.incrementAndGet();
          } else {
            ex.printStackTrace();
          }
        }
      }));
    }

    // Wait until all threads are ready, then fire startLatch simultaneously
    readyLatch.await();
    startLatch.countDown();

    for (Future<?> future : futures) {
      future.get();
    }

    executorService.shutdown();

    // 3. Verify results
    // 10 threads x $20.00 = $200.00 requested, but initial balance is $100.00
    // Exactly 5 withdrawals must succeed ($100 / $20 = 5), and 5 must fail with InsufficientFundsException
    assertEquals(5, successCount.get(), "Exactly 5 withdrawals should succeed");
    assertEquals(5, failedCount.get(),
        "Exactly 5 withdrawals should fail due to insufficient funds");

    // Check final running balance in DB is exactly 0.00
    Balance latestBalance = balanceRepository.getLatestBalanceByAccountHolderId(
            accountHolder.getId())
        .orElseThrow();
    assertEquals(0, new BigDecimal("0.00").compareTo(latestBalance.getRunningBalance()),
        "Final running balance must be exactly 0.00 and never negative");
  }
}
