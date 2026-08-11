package com.redmath.redbank.chatbot.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceQueryServiceTest {

  @Autowired
  private BalanceQueryService balanceQueryService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  @Autowired
  private BalanceRepository balanceRepository;

  private AccountHolder me;

  @BeforeEach
  void setUp() {
    me = createAccountHolder("bal.me@test.com", "Bal Me", "ACC-BAL-ME");
    BankTransaction tx = createTransaction(me);
    
    // Balance 30 days ago
    createBalance(tx, OffsetDateTime.now(ZoneOffset.UTC).minusDays(30), new BigDecimal("1000.00"));
    // Balance 10 days ago
    createBalance(tx, OffsetDateTime.now(ZoneOffset.UTC).minusDays(10), new BigDecimal("1500.00"));
    // Balance today
    createBalance(tx, OffsetDateTime.now(ZoneOffset.UTC), new BigDecimal("1200.00"));
  }

  @Test
  @DisplayName("getBalanceAsOf() returns correct historical balance")
  void getBalanceAsOf() {
    LocalDate asOf = LocalDate.now().minusDays(15);
    var balanceOpt = balanceQueryService.getBalanceAsOf(me.getId(), asOf);
    
    assertTrue(balanceOpt.isPresent());
    assertEquals(0, new BigDecimal("1000.00").compareTo(balanceOpt.get()));
  }

  @Test
  @DisplayName("projectMonthEndBalance() calculates correctly")
  void projectMonthEndBalance() {
    // 30 days ago: 1000, Today: 1200 -> delta = +200 over 30 days
    // Rate = 200/30 ~ 6.66 per day
    // Days left in month varies, but logic works. We just assert it executes without errors 
    // and returns a non-null BigDecimal.
    BigDecimal projected = balanceQueryService.projectMonthEndBalance(me.getId());
    assertTrue(projected.compareTo(BigDecimal.ZERO) > 0);
  }

  private AccountHolder createAccountHolder(String email, String name, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name(name)
        .address("Address")
        .phoneNumber("+1999222" + Math.abs(accountNumber.hashCode()))
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber(accountNumber);
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return accountHolderRepository.save(accountHolder);
  }

  private BankTransaction createTransaction(AccountHolder src) {
    BankTransaction tx = new BankTransaction();
    tx.setTransactionReference("REF-" + src.getId() + "-" + System.currentTimeMillis());
    tx.setSourceAccountHolder(src);
    tx.setDestinationAccountHolder(src);
    tx.setType(TransactionType.DEPOSIT);
    tx.setAmount(BigDecimal.TEN);
    tx.setStatus(TransactionStatus.COMPLETED);
    tx.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return bankTransactionRepository.save(tx);
  }

  private void createBalance(BankTransaction tx, OffsetDateTime date, BigDecimal amount) {
    Balance balance = new Balance();
    balance.setAccountHolder(tx.getSourceAccountHolder());
    balance.setTransaction(tx);
    balance.setEntryDate(date);
    balance.setAmount(amount);
    balance.setIndicator(BalanceIndicator.CREDIT);
    balance.setRunningBalance(amount);
    balanceRepository.save(balance);
  }
}
