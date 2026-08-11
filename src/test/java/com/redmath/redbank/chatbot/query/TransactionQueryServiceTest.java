package com.redmath.redbank.chatbot.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.enums.Direction;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionCategory;
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
class TransactionQueryServiceTest {

  @Autowired
  private TransactionQueryService transactionQueryService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  private AccountHolder me;
  private AccountHolder shop;
  private BankTransaction earlierTx;
  private BankTransaction laterTx;

  @BeforeEach
  void setUp() {
    me = createAccountHolder("tx.me@test.com", "Tx Me", "ACC-TX-ME");
    shop = createAccountHolder("shop@test.com", "Shop", "ACC-SHOP");

    earlierTx = createTransaction(me, shop, new BigDecimal("10.00"), TransactionCategory.GROCERY, OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    laterTx = createTransaction(shop, me, new BigDecimal("50.00"), TransactionCategory.OTHER, OffsetDateTime.of(2026, 8, 10, 10, 0, 0, 0, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("execute() correctly aggregates DEBIT transactions")
  void executeDebit() {
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setDirection(Direction.DEBIT);

    var result = transactionQueryService.execute(intent, me.getId());
    
    assertEquals(1, result.count());
    assertEquals(0, new BigDecimal("10.00").compareTo(result.sum()));
  }

  @Test
  @DisplayName("execute() correctly filters by category and date")
  void executeFilterCategoryAndDate() {
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setCategory(TransactionCategory.OTHER);
    intent.setStartDate(LocalDate.of(2026, 8, 5));
    intent.setEndDate(LocalDate.of(2026, 8, 15));

    var result = transactionQueryService.execute(intent, me.getId());
    
    assertEquals(1, result.count());
    assertEquals(0, new BigDecimal("50.00").compareTo(result.sum()));
  }

  @Test
  @DisplayName("findLatestOrEarliest() returns EARLIEST transaction")
  void findEarliest() {
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setSortOrder("EARLIEST");

    var result = transactionQueryService.findLatestOrEarliest(intent, me.getId());
    assertTrue(result.isPresent());
    assertEquals(earlierTx.getId(), result.get().getId());
  }

  @Test
  @DisplayName("findLatestOrEarliest() returns LATEST transaction")
  void findLatest() {
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setSortOrder("LATEST");

    var result = transactionQueryService.findLatestOrEarliest(intent, me.getId());
    assertTrue(result.isPresent());
    assertEquals(laterTx.getId(), result.get().getId());
  }

  private AccountHolder createAccountHolder(String email, String name, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name(name)
        .address("Address")
        .phoneNumber("+1999111" + Math.abs(accountNumber.hashCode()))
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

  private BankTransaction createTransaction(AccountHolder src, AccountHolder dest, BigDecimal amount, TransactionCategory category, OffsetDateTime createdAt) {
    BankTransaction tx = new BankTransaction();
    tx.setTransactionReference("REF-" + src.getId() + "-" + dest.getId() + "-" + createdAt.toEpochSecond());
    tx.setSourceAccountHolder(src);
    tx.setDestinationAccountHolder(dest);
    tx.setType(TransactionType.TRANSFER);
    tx.setAmount(amount);
    tx.setStatus(TransactionStatus.COMPLETED);
    tx.setCategory(category);
    tx.setCreatedAt(createdAt);
    return bankTransactionRepository.save(tx);
  }
}
