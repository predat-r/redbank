package com.redmath.redbank.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

  private String testEmail;
  private String testAccountNumber;
  private String testPhoneNumber;
  private User testUser;
  private AccountHolder testAccountHolder;

  @BeforeEach
  void setUp() {
    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    testEmail = "concurrency.test." + uniqueSuffix + "@example.com";
    testAccountNumber = "RB-CONC-" + uniqueSuffix;
    testPhoneNumber = "+1" + (2000000000L + Math.abs(uniqueSuffix.hashCode() % 999999999));
  }

  @AfterEach
  void tearDown() {
    if (testAccountHolder != null) {
      balanceRepository.findAllByAccountHolderId(testAccountHolder.getId())
          .forEach(balanceRepository::delete);
      bankTransactionRepository
          .findBySourceAccountHolderIdOrDestinationAccountHolderId(
              testAccountHolder.getId(), testAccountHolder.getId(),
              org.springframework.data.domain.Pageable.unpaged())
          .forEach(bankTransactionRepository::delete);
      accountHolderRepository.delete(testAccountHolder);
    }
    if (testUser != null) {
      userRepository.delete(testUser);
    }
  }

  @Test
  @DisplayName("Concurrent withdrawals execute safely under PESSIMISTIC_WRITE lock without overdrawing account")
  void concurrentWithdrawalsDoNotOverdrawAccount() throws Exception {
    // 1. Setup User and AccountHolder with initial balance of $100.00
    testUser = userRepository.save(User.builder()
        .email(testEmail)
        .name("Concurrent Test User")
        .address("123 Parallel St")
        .phoneNumber(testPhoneNumber)
        .passwordHash("hashedpass")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    testAccountHolder = new AccountHolder();
    testAccountHolder.setUser(testUser);
    testAccountHolder.setAccountNumber(testAccountNumber);
    testAccountHolder.setCurrency("USD");
    testAccountHolder.setAccountStatus(AccountStatus.ACTIVE);
    testAccountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    testAccountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    testAccountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    testAccountHolder = accountHolderRepository.save(testAccountHolder);

    // Seed initial deposit transaction of $100.00
    BankTransaction initTx = new BankTransaction();
    initTx.setTransactionReference("TXN-INIT-" + UUID.randomUUID().toString().substring(0, 8));
    initTx.setType(TransactionType.DEPOSIT);
    initTx.setAmount(new BigDecimal("100.00"));
    initTx.setDestinationAccountHolder(testAccountHolder);
    initTx.setStatus(TransactionStatus.COMPLETED);
    initTx.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    initTx = bankTransactionRepository.save(initTx);

    Balance initBalance = new Balance();
    initBalance.setAccountHolder(testAccountHolder);
    initBalance.setTransactionId(initTx.getId());
    initBalance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    initBalance.setAmount(new BigDecimal("100.00"));
    initBalance.setIndicator(BalanceIndicator.CREDIT);
    initBalance.setRunningBalance(new BigDecimal("100.00"));
    balanceRepository.save(initBalance);

    // 2. Fire 10 concurrent threads, each attempting to withdraw $20.00 at the same time
    // Total requested: $200.00 — only $100.00 available, so exactly 5 should succeed.
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failedCount = new AtomicInteger(0);

    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final Long userId = testUser.getId();
      futures.add(executorService.submit(() -> {
        readyLatch.countDown();
        try {
          startLatch.await();
          WithdrawalRequest request = new WithdrawalRequest();
          request.setAmount(new BigDecimal("20.00"));
          request.setDescription("Concurrent withdrawal test");

          bankTransactionService.withdraw(userId, request);
          successCount.incrementAndGet();
        } catch (InsufficientFundsException ex) {
          failedCount.incrementAndGet();
        } catch (Exception ex) {
          String msg = ex.getMessage();
          Throwable cause = ex.getCause();
          if ((msg != null && msg.contains("Insufficient funds"))
              || cause instanceof InsufficientFundsException) {
            failedCount.incrementAndGet();
          } else {
            ex.printStackTrace();
          }
        }
      }));
    }

    // Release all threads simultaneously
    readyLatch.await();
    startLatch.countDown();

    for (Future<?> future : futures) {
      future.get();
    }

    executorService.shutdown();

    // 3. Assert: exactly 5 succeed ($100 / $20), 5 fail with InsufficientFunds
    assertEquals(5, successCount.get(), "Exactly 5 withdrawals should succeed");
    assertEquals(5, failedCount.get(),
        "Exactly 5 withdrawals should fail due to insufficient funds");

    // Final running balance must be exactly $0.00 — never negative
    Balance latestBalance = balanceRepository.getLatestBalanceByAccountHolderId(
            testAccountHolder.getId())
        .orElseThrow();
    assertEquals(0, new BigDecimal("0.00").compareTo(latestBalance.getRunningBalance()),
        "Final running balance must be exactly 0.00 and never negative");
  }
}
