package com.redmath.redbank.balance.admin;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminBalanceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  @Autowired
  private BalanceRepository balanceRepository;

  @Test
  @DisplayName("GET /api/admin/balance/{accountId}/latest - Success for ADMIN")
  void getLatestBalanceSuccess() throws Exception {
    User user = userRepository.save(User.builder()
        .email("admin.bal.user@example.com")
        .name("Admin Bal User")
        .address("123 Main St, NY")
        .phoneNumber("+19998887773")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber("RB-ADM-BAL-001");
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder = accountHolderRepository.save(accountHolder);

    BankTransaction transaction = new BankTransaction();
    transaction.setTransactionReference("TX-ADM-BAL-001");
    transaction.setSourceAccountHolder(accountHolder);
    transaction.setDestinationAccountHolder(accountHolder);
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setDescription("Admin balance deposit");
    transaction.setAmount(new BigDecimal("500.00"));
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    transaction = bankTransactionRepository.save(transaction);

    Balance balance = new Balance();
    balance.setAccountHolder(accountHolder);
    balance.setTransactionId(transaction.getId());
    balance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    balance.setAmount(new BigDecimal("500.00"));
    balance.setIndicator(BalanceIndicator.CREDIT);
    balance.setRunningBalance(new BigDecimal("500.00"));
    balance = balanceRepository.save(balance);

    mockMvc.perform(get("/api/admin/balance/{accountId}/latest", accountHolder.getId())
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(balance.getId()))
        .andExpect(jsonPath("$.accountHolderId").value(accountHolder.getId()))
        .andExpect(jsonPath("$.amount").value(500.00))
        .andExpect(jsonPath("$.runningBalance").value(500.00));
  }

  @Test
  @DisplayName(
      "GET /api/admin/balance/{accountId}/latest - Returns NOT_FOUND for invalid accountId")
  void getLatestBalanceNotFound() throws Exception {
    mockMvc.perform(get("/api/admin/balance/{accountId}/latest", 999999L)
            .with(withAdmin(1L)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/admin/balance/{accountId}/latest - Returns FORBIDDEN for ACCOUNT_HOLDER")
  void getLatestBalanceForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/balance/{accountId}/latest", 1L)
            .with(withAccountHolder(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName(
      "GET /api/admin/balance/{accountId}/latest - Returns UNAUTHORIZED when unauthenticated")
  void getLatestBalanceUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/admin/balance/{accountId}/latest", 1L))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/admin/balance/{accountId}/ledger - Success with pagination for ADMIN")
  void getBalanceLedgerSuccess() throws Exception {
    User user = userRepository.save(User.builder()
        .email("admin.ledger.user@example.com")
        .name("Admin Ledger User")
        .address("123 Main St, NY")
        .phoneNumber("+19998887774")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber("RB-ADM-BAL-002");
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder = accountHolderRepository.save(accountHolder);

    BankTransaction tx1 = new BankTransaction();
    tx1.setTransactionReference("TX-ADM-BAL-002");
    tx1.setSourceAccountHolder(accountHolder);
    tx1.setDestinationAccountHolder(accountHolder);
    tx1.setType(TransactionType.DEPOSIT);
    tx1.setAmount(new BigDecimal("100.00"));
    tx1.setStatus(TransactionStatus.COMPLETED);
    tx1.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    tx1 = bankTransactionRepository.save(tx1);

    Balance b1 = new Balance();
    b1.setAccountHolder(accountHolder);
    b1.setTransactionId(tx1.getId());
    b1.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    b1.setAmount(new BigDecimal("100.00"));
    b1.setIndicator(BalanceIndicator.CREDIT);
    b1.setRunningBalance(new BigDecimal("100.00"));
    balanceRepository.save(b1);

    BankTransaction tx2 = new BankTransaction();
    tx2.setTransactionReference("TX-ADM-BAL-003");
    tx2.setSourceAccountHolder(accountHolder);
    tx2.setDestinationAccountHolder(accountHolder);
    tx2.setType(TransactionType.WITHDRAWAL);
    tx2.setAmount(new BigDecimal("40.00"));
    tx2.setStatus(TransactionStatus.COMPLETED);
    tx2.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    tx2 = bankTransactionRepository.save(tx2);

    Balance b2 = new Balance();
    b2.setAccountHolder(accountHolder);
    b2.setTransactionId(tx2.getId());
    b2.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    b2.setAmount(new BigDecimal("40.00"));
    b2.setIndicator(BalanceIndicator.DEBIT);
    b2.setRunningBalance(new BigDecimal("60.00"));
    balanceRepository.save(b2);

    mockMvc.perform(get("/api/admin/balance/{accountId}/ledger", accountHolder.getId())
            .param("page", "0")
            .param("size", "10")
            .param("sort", "id,desc")
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].runningBalance").value(60.00))
        .andExpect(jsonPath("$.content[1].runningBalance").value(100.00));
  }

  @Test
  @DisplayName("GET /api/admin/balance/{accountId}/ledger - Success with custom ascending sort")
  void getBalanceLedgerCustomSort() throws Exception {
    User user = userRepository.save(User.builder()
        .email("admin.sort.user@example.com")
        .name("Admin Sort User")
        .address("123 Main St, NY")
        .phoneNumber("+19998887775")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber("RB-ADM-BAL-003");
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder = accountHolderRepository.save(accountHolder);

    BankTransaction tx1 = new BankTransaction();
    tx1.setTransactionReference("TX-ADM-BAL-004");
    tx1.setSourceAccountHolder(accountHolder);
    tx1.setDestinationAccountHolder(accountHolder);
    tx1.setType(TransactionType.DEPOSIT);
    tx1.setAmount(new BigDecimal("100.00"));
    tx1.setStatus(TransactionStatus.COMPLETED);
    tx1.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    tx1 = bankTransactionRepository.save(tx1);

    Balance b1 = new Balance();
    b1.setAccountHolder(accountHolder);
    b1.setTransactionId(tx1.getId());
    b1.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    b1.setAmount(new BigDecimal("100.00"));
    b1.setIndicator(BalanceIndicator.CREDIT);
    b1.setRunningBalance(new BigDecimal("100.00"));
    balanceRepository.save(b1);

    mockMvc.perform(get("/api/admin/balance/{accountId}/ledger", accountHolder.getId())
            .param("page", "0")
            .param("size", "5")
            .param("sort", "id,asc")
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].runningBalance").value(100.00));
  }

  @Test
  @DisplayName(
      "GET /api/admin/balance/{accountId}/ledger - Returns empty content for account with no entries")
  void getBalanceLedgerEmptyContent() throws Exception {
    mockMvc.perform(get("/api/admin/balance/{accountId}/ledger", 999999L)
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/admin/balance/{accountId}/ledger - Returns FORBIDDEN for non-admin user")
  void getBalanceLedgerForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/balance/{accountId}/ledger", 1L)
            .with(withAccountHolder(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName(
      "GET /api/admin/balance/{accountId}/ledger - Returns UNAUTHORIZED when unauthenticated")
  void getBalanceLedgerUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/admin/balance/{accountId}/ledger", 1L))
        .andExpect(status().isUnauthorized());
  }
}
