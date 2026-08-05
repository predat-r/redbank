package com.redmath.redbank.account.admin;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
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
class AdminAccountHolderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Test
  @DisplayName("GET /api/admin/accounts/{accountId} - Success for ADMIN")
  void getAccountHolderSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("adm.get@example.com", "RB-ADM-ACC-001");

    mockMvc.perform(get("/api/admin/accounts/{accountId}", accountHolder.getId())
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(accountHolder.getId()))
        .andExpect(jsonPath("$.accountNumber").value("RB-ADM-ACC-001"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
  }

  @Test
  @DisplayName("GET /api/admin/accounts/{accountId} - Returns NOT_FOUND when missing")
  void getAccountHolderNotFound() throws Exception {
    mockMvc.perform(get("/api/admin/accounts/{accountId}", 999999L)
            .with(withAdmin(1L)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/admin/accounts/{accountId} - Returns FORBIDDEN for non-admin")
  void getAccountHolderForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/accounts/{accountId}", 1L)
            .with(withAccountHolder(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/admin/accounts/{accountId} - Returns UNAUTHORIZED when unauthenticated")
  void getAccountHolderUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/admin/accounts/{accountId}", 1L))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/admin/accounts - Success with pagination for ADMIN")
  void getAllAccountHoldersSuccess() throws Exception {
    createAccountHolder("adm.all1@example.com", "RB-ADM-ALL-001");
    createAccountHolder("adm.all2@example.com", "RB-ADM-ALL-002");

    mockMvc.perform(get("/api/admin/accounts")
            .param("page", "0")
            .param("size", "10")
            .param("sort", "id,desc")
            .with(withAdmin(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("GET /api/admin/accounts - Returns FORBIDDEN for non-admin")
  void getAllAccountHoldersForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/accounts")
            .with(withAccountHolder(1L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PATCH /api/admin/accounts/freeze/{accountId} - Success for ADMIN")
  void freezeAccountHolderSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("adm.frz@example.com", "RB-ADM-FRZ-001");

    mockMvc.perform(patch("/api/admin/accounts/freeze/{accountId}", accountHolder.getId())
            .with(withAdmin(1L)))
        .andExpect(status().isNoContent());

    AccountHolder updated = accountHolderRepository.findById(accountHolder.getId()).orElseThrow();
    assertEquals(AccountStatus.FROZEN, updated.getAccountStatus());
  }

  @Test
  @DisplayName("PATCH /api/admin/accounts/freeze/{accountId} - Returns CONFLICT for CLOSED account")
  void freezeAccountHolderConflictWhenClosed() throws Exception {
    AccountHolder accountHolder = createAccountHolder("adm.frzclose@example.com", "RB-ADM-FRZ-002");
    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);

    mockMvc.perform(patch("/api/admin/accounts/freeze/{accountId}", accountHolder.getId())
            .with(withAdmin(1L)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("PATCH /api/admin/accounts/freeze/{accountId} - BAD_REQUEST when admin userId missing")
  void freezeAccountHolderMissingUserId() throws Exception {
    mockMvc.perform(patch("/api/admin/accounts/freeze/{accountId}", 1L)
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PATCH /api/admin/accounts/deactivate/{accountId} - Success for ADMIN")
  void deactivateAccountHolderSuccess() throws Exception {
    AccountHolder accountHolder = createAccountHolder("adm.deact@example.com", "RB-ADM-DEA-001");

    mockMvc.perform(patch("/api/admin/accounts/deactivate/{accountId}", accountHolder.getId())
            .with(withAdmin(1L)))
        .andExpect(status().isNoContent());

    AccountHolder updated = accountHolderRepository.findById(accountHolder.getId()).orElseThrow();
    assertEquals(AccountStatus.CLOSED, updated.getAccountStatus());
    assertEquals(UserStatus.DEACTIVATED, updated.getUser().getStatus());
  }

  @Test
  @DisplayName("PATCH /api/admin/accounts/deactivate/{accountId} - BAD_REQUEST when admin userId missing")
  void deactivateAccountHolderMissingUserId() throws Exception {
    mockMvc.perform(patch("/api/admin/accounts/deactivate/{accountId}", 1L)
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isBadRequest());
  }

  private AccountHolder createAccountHolder(String email, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name("Admin Account Test User")
        .address("123 Main St, NY")
        .phoneNumber("+1999555" + Math.abs(accountNumber.hashCode()))
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
