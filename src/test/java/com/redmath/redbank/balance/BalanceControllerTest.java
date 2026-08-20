package com.redmath.redbank.balance;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withPendingUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BalanceControllerTest {

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
  @DisplayName("GET /api/balance/me/latest - Success for ACCOUNT_HOLDER with existing balance")
  void getMyBalanceSuccess() throws Exception {
    User user = userRepository.save(User.builder()
        .email("balance.test.user@example.com")
        .name("Balance Test User")
        .address("123 Main St, NY")
        .phoneNumber("+19998887771")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber("RB-BAL-TEST-001");
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder = accountHolderRepository.save(accountHolder);

    BankTransaction transaction = new BankTransaction();
    transaction.setTransactionReference("TX-BAL-TEST-001");
    transaction.setSourceAccountHolder(accountHolder);
    transaction.setDestinationAccountHolder(accountHolder);
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setDescription("Test deposit");
    transaction.setAmount(new BigDecimal("250.00"));
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    transaction = bankTransactionRepository.save(transaction);

    Balance balance = new Balance();
    balance.setAccountHolder(accountHolder);
    balance.setTransactionId(transaction.getId());
    balance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    balance.setAmount(new BigDecimal("250.00"));
    balance.setIndicator(BalanceIndicator.CREDIT);
    balance.setRunningBalance(new BigDecimal("250.00"));
    balance = balanceRepository.save(balance);

    mockMvc.perform(get("/api/balance/me/latest")
            .with(withAccountHolder(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(balance.getId()))
        .andExpect(jsonPath("$.accountHolderId").value(accountHolder.getId()))
        .andExpect(jsonPath("$.transactionId").value(transaction.getId()))
        .andExpect(jsonPath("$.amount").value(250.00))
        .andExpect(jsonPath("$.indicator").value("CREDIT"))
        .andExpect(jsonPath("$.runningBalance").value(250.00));
  }

  @Test
  @DisplayName("GET /api/balance/me/latest - Returns zero balance when no entries exist")
  void getMyBalanceReturnsZeroBalanceWhenNoEntries() throws Exception {
    User user = userRepository.save(User.builder()
        .email("balance.nobalance@example.com")
        .name("No Balance User")
        .address("123 Main St, NY")
        .phoneNumber("+19998887772")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber("RB-BAL-TEST-002");
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolderRepository.save(accountHolder);

    mockMvc.perform(get("/api/balance/me/latest")
            .with(withAccountHolder(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(0))
        .andExpect(jsonPath("$.runningBalance").value(0));
  }

  @Test
  @DisplayName("GET /api/balance/me/latest - Returns BAD_REQUEST when JWT userId claim is missing")
  void getMyBalanceReturnsBadRequestWhenUserIdClaimMissing() throws Exception {
    mockMvc.perform(get("/api/balance/me/latest")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/balance/me/latest - Returns UNAUTHORIZED when unauthenticated")
  void getMyBalanceUnauthenticatedReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/balance/me/latest"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/balance/me/latest - Returns FORBIDDEN for non-account holder role")
  void getMyBalanceWithForbiddenRoleReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/balance/me/latest")
            .with(withPendingUser(99L)))
        .andExpect(status().isForbidden());
  }
}
