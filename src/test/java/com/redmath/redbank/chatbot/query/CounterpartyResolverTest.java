package com.redmath.redbank.chatbot.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
class CounterpartyResolverTest {

  @Autowired
  private CounterpartyResolver counterpartyResolver;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  private AccountHolder me;
  private AccountHolder alice;
  private AccountHolder bob;
  private AccountHolder bob2;

  @BeforeEach
  void setUp() {
    me = createAccountHolder("me@test.com", "Me User", "ACC-ME");
    alice = createAccountHolder("alice@test.com", "Alice Smith", "ACC-ALICE");
    bob = createAccountHolder("bob1@test.com", "Bob Jones", "ACC-BOB1");
    bob2 = createAccountHolder("bob2@test.com", "Bob Brown", "ACC-BOB2");

    createTransaction(me, alice);
    createTransaction(me, bob);
    createTransaction(me, bob2);
    // Note: 'me' has NO transactions with 'Charlie'
    AccountHolder charlie = createAccountHolder("charlie@test.com", "Charlie", "ACC-CHARLIE");
  }

  @Test
  @DisplayName("Resolve exactly one match among transacted users")
  void resolveExactMatch() {
    var result = counterpartyResolver.resolve("Alice", me.getId());
    assertEquals(CounterpartyResolver.ResolutionResult.Status.RESOLVED, result.status);
    assertEquals(alice.getId(), result.accountHolderId);
  }

  @Test
  @DisplayName("Resolve ambiguous match among transacted users")
  void resolveAmbiguousMatch() {
    var result = counterpartyResolver.resolve("Bob", me.getId());
    assertEquals(CounterpartyResolver.ResolutionResult.Status.AMBIGUOUS, result.status);
    assertEquals(2, result.candidates.size());
  }

  @Test
  @DisplayName("Resolve NOT_FOUND for user not in transaction history (even if in DB)")
  void resolveNotFoundForNonTransactedUser() {
    var result = counterpartyResolver.resolve("Charlie", me.getId());
    assertEquals(CounterpartyResolver.ResolutionResult.Status.NOT_FOUND, result.status);
  }

  private AccountHolder createAccountHolder(String email, String name, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name(name)
        .address("Address")
        .phoneNumber("+1999000" + Math.abs(accountNumber.hashCode()))
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

  private BankTransaction createTransaction(AccountHolder src, AccountHolder dest) {
    BankTransaction tx = new BankTransaction();
    tx.setTransactionReference("REF-" + src.getId() + "-" + dest.getId());
    tx.setSourceAccountHolder(src);
    tx.setDestinationAccountHolder(dest);
    tx.setType(TransactionType.TRANSFER);
    tx.setAmount(new BigDecimal("10.00"));
    tx.setStatus(TransactionStatus.COMPLETED);
    tx.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return bankTransactionRepository.save(tx);
  }
}
