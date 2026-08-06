package com.redmath.redbank.user.admin;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import com.redmath.redbank.user.admin.dto.CreateUserRequest;
import com.redmath.redbank.user.admin.dto.UpdateUserRequest;
import com.redmath.redbank.user.role.RoleName;
import com.redmath.redbank.user.role.UserRoleRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserEndpointTests {

  private static final String ADMIN_EMAIL = "admin@redbank.com";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private UserRoleRepository userRoleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void adminCanCreateActiveUserWithEmbeddedAccountHolder() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "Created.User@Example.com",
        " 03005550001 ",
        "password123",
        "  Created User ",
        "  789 Created User Street "
    );

    MvcResult result = mockMvc.perform(post("/api/admin/users")
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value("created.user@example.com"))
        .andExpect(jsonPath("$.user.name").value("Created User"))
        .andExpect(jsonPath("$.user.status").value("ACTIVE"))
        .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.accountHolder.id").isNumber())
        .andExpect(jsonPath("$.accountHolder.accountNumber").isNotEmpty())
        .andExpect(jsonPath("$.accountHolder.currency").value("USD"))
        .andExpect(jsonPath("$.accountHolder.accountStatus").value("ACTIVE"))
        .andReturn();

    long userId = ((Number) readJson(result, "$.user.id")).longValue();
    User createdUser = userRepository.findById(userId).orElseThrow();
    AccountHolder accountHolder = accountHolderRepository.findByUserId(userId).orElseThrow();

    assertThat(passwordEncoder.matches(request.password(), createdUser.getPasswordHash()))
        .isTrue();
    assertThat(createdUser.getApprovedBy().getId()).isEqualTo(adminUserId());
    assertThat(accountHolder.getUser().getId()).isEqualTo(userId);
    assertThat(userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleName.ACCOUNT_HOLDER))
        .isTrue();
  }

  @Test
  void adminCanUpdateUserDetails() throws Exception {
    CreatedUser created = createUserByAdmin("update.user@example.com", "03005550002");
    UpdateUserRequest request = new UpdateUserRequest(
        "Updated.User@Example.com",
        " 03005550003 ",
        "  Updated User ",
        "  321 Updated User Street "
    );

    mockMvc.perform(put("/api/admin/users/{userId}", created.userId())
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(created.userId()))
        .andExpect(jsonPath("$.email").value("updated.user@example.com"))
        .andExpect(jsonPath("$.phoneNumber").value("03005550003"))
        .andExpect(jsonPath("$.name").value("Updated User"))
        .andExpect(jsonPath("$.address").value("321 Updated User Street"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    User updatedUser = userRepository.findById(created.userId()).orElseThrow();
    assertThat(updatedUser.getEmail()).isEqualTo("updated.user@example.com");
    assertThat(updatedUser.getName()).isEqualTo("Updated User");
  }

  @Test
  void duplicateEmailOnUpdateReturnsConflict() throws Exception {
    CreatedUser first = createUserByAdmin("duplicate.first@example.com", "03005550004");
    createUserByAdmin("duplicate.second@example.com", "03005550005");
    UpdateUserRequest request = new UpdateUserRequest(
        "duplicate.second@example.com",
        first.phoneNumber(),
        "Duplicate First",
        "123 Duplicate Street"
    );

    mockMvc.perform(put("/api/admin/users/{userId}", first.userId())
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email is already registered"));
  }

  @Test
  void adminCanDeactivateUserAndCloseAccountHolder() throws Exception {
    CreatedUser created = createUserByAdmin("deactivate.user@example.com", "03005550006");

    mockMvc.perform(patch("/api/admin/users/{userId}/deactivate", created.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNoContent());

    User user = userRepository.findById(created.userId()).orElseThrow();
    AccountHolder accountHolder = accountHolderRepository.findByUserId(created.userId())
        .orElseThrow();
    assertThat(user.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
    assertThat(user.getRefreshTokenVersion()).isEqualTo(1L);
    assertThat(accountHolder.getAccountStatus()).isEqualTo(AccountStatus.CLOSED);
  }

  @Test
  void adminCanReactivateUserAndReopenExistingAccountHolder() throws Exception {
    CreatedUser created = createUserByAdmin("reactivate.user@example.com", "03005550009");
    AccountHolder originalAccountHolder = accountHolderRepository.findByUserId(created.userId())
        .orElseThrow();
    String originalAccountNumber = originalAccountHolder.getAccountNumber();

    mockMvc.perform(patch("/api/admin/users/{userId}/deactivate", created.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNoContent());

    mockMvc.perform(patch("/api/admin/users/{userId}/reactivate", created.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNoContent());

    mockMvc.perform(patch("/api/admin/users/{userId}/reactivate", created.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNoContent());

    User user = userRepository.findById(created.userId()).orElseThrow();
    AccountHolder reactivatedAccountHolder = accountHolderRepository.findByUserId(created.userId())
        .orElseThrow();
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.getRefreshTokenVersion()).isEqualTo(1L);
    assertThat(reactivatedAccountHolder.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(reactivatedAccountHolder.getAccountNumber()).isEqualTo(originalAccountNumber);
  }

  @Test
  void pendingUserCannotBeReactivated() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(patch("/api/admin/users/{userId}/reactivate", registration.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value("Only deactivated users can be reactivated"));
  }

  @Test
  void unknownUserCannotBeUpdatedDeactivatedOrReactivated() throws Exception {
    long unknownUserId = Long.MAX_VALUE;
    UpdateUserRequest request = new UpdateUserRequest(
        "unknown@example.com",
        "03005550007",
        "Unknown User",
        "123 Unknown Street"
    );

    mockMvc.perform(put("/api/admin/users/{userId}", unknownUserId)
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    mockMvc.perform(patch("/api/admin/users/{userId}/deactivate", unknownUserId)
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNotFound());

    mockMvc.perform(patch("/api/admin/users/{userId}/reactivate", unknownUserId)
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminCanListUsers() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(get("/api/admin/users")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.id == " + registration.userId() + ")]")
            .isNotEmpty())
        .andExpect(jsonPath("$.content[?(@.id == " + registration.userId()
            + ")].passwordHash").doesNotExist());
  }

  @Test
  void adminCanPageAndSortUsers() throws Exception {
    registerPendingUser();

    mockMvc.perform(get("/api/admin/users")
            .param("page", "0")
            .param("size", "1")
            .param("sort", "id,desc")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.size").value(1))
        .andExpect(jsonPath("$.page.totalElements").isNumber());
  }

  @Test
  void adminCanViewUser() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(get("/api/admin/users/{userId}", registration.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(registration.userId()))
        .andExpect(jsonPath("$.email").value(registration.email()))
        .andExpect(jsonPath("$.phoneNumber").value("03003334444"))
        .andExpect(jsonPath("$.name").value("Admin View User"))
        .andExpect(jsonPath("$.address").value("456 Admin View Street"))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.updatedAt").isNotEmpty())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.refreshTokenVersion").doesNotExist());
  }

  @Test
  void unknownUserReturnsNotFound() throws Exception {
    long unknownUserId = Long.MAX_VALUE;

    mockMvc.perform(get("/api/admin/users/{userId}", unknownUserId)
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("User was not found"))
        .andExpect(jsonPath("$.path").value("/api/admin/users/" + unknownUserId));
  }

  @Test
  void unsupportedListSortReturnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/admin/users")
            .param("sort", "email,asc")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message")
            .value("Unsupported sort field. Allowed fields are: createdAt, id"))
        .andExpect(jsonPath("$.path").value("/api/admin/users"));
  }

  @Test
  void listingUsersWithoutAuthenticationReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/admin/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/admin/users"));
  }

  @Test
  void accountHolderCannotViewUsers() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(get("/api/admin/users/{userId}", registration.userId())
            .with(withAccountHolder(registration.userId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.path")
            .value("/api/admin/users/" + registration.userId()));
  }

  @Test
  void accountHolderCannotCreateUsers() throws Exception {
    RegisteredUser registration = registerPendingUser();
    CreateUserRequest request = new CreateUserRequest(
        "forbidden.create@example.com",
        "03005550008",
        "password123",
        "Forbidden Create",
        "123 Forbidden Street"
    );

    mockMvc.perform(post("/api/admin/users")
            .with(withAccountHolder(registration.userId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  private CreatedUser createUserByAdmin(String email, String phoneNumber) throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        email,
        phoneNumber,
        "password123",
        "Created Account Holder",
        "123 Created Account Holder Street"
    );
    MvcResult result = mockMvc.perform(post("/api/admin/users")
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    Number userId = readJson(result, "$.user.id");
    return new CreatedUser(userId.longValue(), email, phoneNumber);
  }

  private RegisteredUser registerPendingUser() throws Exception {
    RegisterRequest request = new RegisterRequest(
        "admin.view.user@example.com",
        "03003334444",
        "password123",
        "Admin View User",
        "456 Admin View Street"
    );

    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    Number userId = readJson(result, "$.id");
    return new RegisteredUser(userId.longValue(), request.email());
  }

  private long adminUserId() {
    return userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
        .orElseThrow()
        .getId();
  }

  private <T> T readJson(MvcResult result, String path) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
  }

  private record RegisteredUser(long userId, String email) {

  }

  private record CreatedUser(long userId, String email, String phoneNumber) {

  }
}
