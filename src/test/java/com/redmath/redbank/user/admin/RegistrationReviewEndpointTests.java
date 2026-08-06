package com.redmath.redbank.user.admin;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.redmath.redbank.user.admin.dto.RejectRegistrationRequest;
import com.redmath.redbank.user.role.RoleName;
import com.redmath.redbank.user.role.UserRoleRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationReviewEndpointTests {

  private static final String ADMIN_EMAIL = "admin@redbank.com";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserRoleRepository userRoleRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Test
  void adminCanListPendingRegistrations() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(get("/api/admin/registrations")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.id == " + registration.userId() + ")]")
            .isNotEmpty());
  }

  @Test
  void adminCanViewPendingRegistration() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(get("/api/admin/registrations/{userId}", registration.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(registration.userId()))
        .andExpect(jsonPath("$.email").value(registration.email()))
        .andExpect(jsonPath("$.phoneNumber").value("03001112222"))
        .andExpect(jsonPath("$.name").value("Review User"))
        .andExpect(jsonPath("$.address").value("123 Review Street"))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  @Test
  void adminCanApprovePendingRegistration() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(post("/api/admin/registrations/{userId}/approve", registration.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNoContent());

    User approvedUser = userRepository.findById(registration.userId()).orElseThrow();
    AccountHolder accountHolder = accountHolderRepository.findByUserId(registration.userId())
        .orElseThrow();

    assertThat(approvedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(approvedUser.getApprovedBy().getId()).isEqualTo(adminUserId());
    assertThat(approvedUser.getApprovedAt()).isNotNull();
    assertThat(userRoleRepository.existsByUser_IdAndRole_Name(
        registration.userId(), RoleName.ACCOUNT_HOLDER)).isTrue();
    assertThat(userRoleRepository.existsByUser_IdAndRole_Name(
        registration.userId(), RoleName.PENDING_USER)).isFalse();
    assertThat(accountHolder.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void adminCanRejectPendingRegistration() throws Exception {
    RegisteredUser registration = registerPendingUser();
    RejectRegistrationRequest request = new RejectRegistrationRequest("  Invalid documents  ");

    mockMvc.perform(post("/api/admin/registrations/{userId}/reject", registration.userId())
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    User rejectedUser = userRepository.findById(registration.userId()).orElseThrow();

    assertThat(rejectedUser.getStatus()).isEqualTo(UserStatus.REJECTED);
    assertThat(rejectedUser.getRejectionReason()).isEqualTo("Invalid documents");
    assertThat(userRoleRepository.existsByUser_IdAndRole_Name(
        registration.userId(), RoleName.PENDING_USER)).isFalse();
    assertThat(accountHolderRepository.findByUserId(registration.userId())).isEmpty();
  }

  @Test
  void reviewingRegistrationTwiceReturnsConflict() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(post("/api/admin/registrations/{userId}/approve", registration.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/admin/registrations/{userId}/approve", registration.userId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("Registration has already been reviewed"))
        .andExpect(jsonPath("$.path")
            .value("/api/admin/registrations/" + registration.userId() + "/approve"));
  }

  @Test
  void unknownRegistrationReturnsNotFound() throws Exception {
    long unknownUserId = Long.MAX_VALUE;

    mockMvc.perform(get("/api/admin/registrations/{userId}", unknownUserId)
            .with(withAdmin(adminUserId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Registration was not found"))
        .andExpect(jsonPath("$.path")
            .value("/api/admin/registrations/" + unknownUserId));
  }

  @Test
  void rejectionReasonLongerThanLimitReturnsBadRequest() throws Exception {
    RegisteredUser registration = registerPendingUser();
    RejectRegistrationRequest request = new RejectRegistrationRequest("x".repeat(501));

    mockMvc.perform(post("/api/admin/registrations/{userId}/reject", registration.userId())
            .with(withAdmin(adminUserId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.path")
            .value("/api/admin/registrations/" + registration.userId() + "/reject"));
  }

  @Test
  void unsupportedListSortReturnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/admin/registrations")
            .param("sort", "email,asc")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message")
            .value("Unsupported sort field. Allowed fields are: createdAt, id"))
        .andExpect(jsonPath("$.path").value("/api/admin/registrations"));
  }

  @Test
  void registrationReviewWithoutAuthenticationReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/admin/registrations"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/admin/registrations"));
  }

  @Test
  void accountHolderCannotReviewRegistrations() throws Exception {
    RegisteredUser registration = registerPendingUser();

    mockMvc.perform(post("/api/admin/registrations/{userId}/approve", registration.userId())
            .with(withAccountHolder(registration.userId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.path")
            .value("/api/admin/registrations/" + registration.userId() + "/approve"));
  }

  private RegisteredUser registerPendingUser() throws Exception {
    RegisterRequest request = new RegisterRequest(
        "review.user@example.com",
        "03001112222",
        "password123",
        "Review User",
        "123 Review Street"
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
}
