package com.redmath.redbank.account;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static com.redmath.redbank.common.AuthUtilities.withPendingUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
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
class AccountHolderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Test
  @DisplayName("GET /api/accounts/me - Success for ACCOUNT_HOLDER")
  void getMyAccountHolderSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("acc.me@example.com", "RB-ACC-ME-001");

    mockMvc.perform(get("/api/accounts/me")
            .with(withAccountHolder(accountHolder.getUser().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(accountHolder.getId()))
        .andExpect(jsonPath("$.userId").value(accountHolder.getUser().getId()))
        .andExpect(jsonPath("$.accountNumber").value("RB-ACC-ME-001"))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
  }

  @Test
  @DisplayName("GET /api/accounts/me - Returns BAD_REQUEST when userId claim is missing")
  void getMyAccountHolderMissingUserIdClaim() throws Exception {
    mockMvc.perform(get("/api/accounts/me")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/accounts/me - Returns UNAUTHORIZED when unauthenticated")
  void getMyAccountHolderUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/accounts/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/accounts/me - Returns FORBIDDEN for PENDING_USER")
  void getMyAccountHolderForbidden() throws Exception {
    mockMvc.perform(get("/api/accounts/me")
            .with(withPendingUser(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/accounts/name/{accountNumber} - Success for ACCOUNT_HOLDER or ADMIN")
  void getAccountHolderByAccountNumberSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("acc.name@example.com", "RB-ACC-NAME-001");

    mockMvc.perform(get("/api/accounts/name/{accountNumber}", "RB-ACC-NAME-001")
            .with(withAccountHolder(99L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Account Test User"))
        .andExpect(jsonPath("$.accountNumber").value("RB-ACC-NAME-001"));

    mockMvc.perform(get("/api/accounts/name/{accountNumber}", "RB-ACC-NAME-001")
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Account Test User"));
  }

  @Test
  @DisplayName("GET /api/accounts/name/{accountNumber} - Returns NOT_FOUND for unknown account")
  void getAccountHolderByAccountNumberNotFound() throws Exception {
    mockMvc.perform(get("/api/accounts/name/{accountNumber}", "RB-NON-EXISTENT")
            .with(withAccountHolder(1L)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/accounts/name/{accountNumber} - Returns FORBIDDEN for PENDING_USER")
  void getAccountHolderByAccountNumberForbidden() throws Exception {
    mockMvc.perform(get("/api/accounts/name/{accountNumber}", "RB-ACC-NAME-001")
            .with(withPendingUser(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PATCH /api/accounts/freeze/me - Success for ACTIVE account")
  void freezeMyAccountHolderSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("acc.freeze@example.com", "RB-ACC-FRZ-001");

    mockMvc.perform(patch("/api/accounts/freeze/me")
            .with(withAccountHolder(accountHolder.getUser().getId())))
        .andExpect(status().isNoContent());

    AccountHolder updated = accountHolderRepository.findById(accountHolder.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(AccountStatus.FROZEN, updated.getAccountStatus());
  }

  @Test
  @DisplayName("PATCH /api/accounts/freeze/me - Returns CONFLICT for CLOSED account")
  void freezeMyAccountHolderConflictWhenClosed() throws Exception {
    AccountHolder accountHolder = createAccountHolder("acc.frzclose@example.com", "RB-ACC-FRZ-002");
    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);

    mockMvc.perform(patch("/api/accounts/freeze/me")
            .with(withAccountHolder(accountHolder.getUser().getId())))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("PATCH /api/accounts/freeze/me - Returns BAD_REQUEST when userId claim is missing")
  void freezeMyAccountHolderMissingUserIdClaim() throws Exception {
    mockMvc.perform(patch("/api/accounts/freeze/me")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PATCH /api/accounts/deactivate/me - Success for ACTIVE account")
  void deactivateMyAccountHolderSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("acc.deact@example.com", "RB-ACC-DEA-001");

    mockMvc.perform(patch("/api/accounts/deactivate/me")
            .with(withAccountHolder(accountHolder.getUser().getId())))
        .andExpect(status().isNoContent());

    AccountHolder updated = accountHolderRepository.findById(accountHolder.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(AccountStatus.CLOSED, updated.getAccountStatus());
    org.junit.jupiter.api.Assertions.assertEquals(UserStatus.DEACTIVATED, updated.getUser().getStatus());
  }

  @Test
  @DisplayName("PATCH /api/accounts/deactivate/me - Returns BAD_REQUEST when userId claim is missing")
  void deactivateMyAccountHolderMissingUserIdClaim() throws Exception {
    mockMvc.perform(patch("/api/accounts/deactivate/me")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER"))))
        .andExpect(status().isBadRequest());
  }

  private AccountHolder createAccountHolder(String email, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name("Account Test User")
        .address("123 Main St, NY")
        .phoneNumber("+1999666" + Math.abs(accountNumber.hashCode()))
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
}
