package com.redmath.redbank.auth;

import static com.redmath.redbank.common.AuthUtilities.withPendingUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.auth.dto.ChangePasswordRequest;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.RefreshTokenRequest;
import com.redmath.redbank.auth.dto.RegisterRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthEndpointTests {

  private static final String PASSWORD = "password123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void registerWithValidRequestReturnsCreated() throws Exception {
    RegisterRequest request = validRegistration();

    register(request)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value(request.email()))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokens.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokens.tokenType").value("Bearer"));
  }

  @Test
  void registerWithoutRequestBodyReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message")
            .value("The request body is missing or contains malformed JSON"))
        .andExpect(jsonPath("$.path").value("/api/auth/register"));
  }

  @Test
  void registerWithInvalidFieldsReturnsBadRequest() throws Exception {
    RegisterRequest request = new RegisterRequest(
        "not-an-email",
        "123",
        "short",
        "",
        "x"
    );

    register(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.path").value("/api/auth/register"));
  }

  @Test
  void registerWithDuplicateEmailReturnsConflict() throws Exception {
    RegisterRequest firstRequest = validRegistration();
    register(firstRequest).andExpect(status().isCreated());

    RegisterRequest duplicateEmailRequest = new RegisterRequest(
        firstRequest.email(),
        "03007654321",
        PASSWORD,
        "Another User",
        "456 Test Street"
    );

    register(duplicateEmailRequest)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("Email is already registered"))
        .andExpect(jsonPath("$.path").value("/api/auth/register"));
  }

  @Test
  void registeredUserCanLogin() throws Exception {
    RegisterRequest registration = validRegistration();
    register(registration).andExpect(status().isCreated());

    LoginRequest request = new LoginRequest(registration.email(), registration.password());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void loginWithIncorrectPasswordReturnsUnauthorized() throws Exception {
    RegisterRequest registration = validRegistration();
    register(registration).andExpect(status().isCreated());

    LoginRequest request = new LoginRequest(registration.email(), "incorrect-password");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Invalid email or password"))
        .andExpect(jsonPath("$.path").value("/api/auth/login"));
  }

  @Test
  void issuedRefreshTokenCanBeRefreshed() throws Exception {
    MvcResult registration = register(validRegistration())
        .andExpect(status().isCreated())
        .andReturn();
    String refreshToken = readJson(registration, "$.tokens.refreshToken");
    RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void malformedRefreshTokenReturnsUnauthorized() throws Exception {
    RefreshTokenRequest request = new RefreshTokenRequest("not-a-jwt");

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"))
        .andExpect(jsonPath("$.path").value("/api/auth/refresh"));
  }

  @Test
  void logoutInvalidatesRefreshToken() throws Exception {
    MvcResult registration = register(validRegistration())
        .andExpect(status().isCreated())
        .andReturn();
    String refreshToken = readJson(registration, "$.tokens.refreshToken");
    RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
    String requestJson = objectMapper.writeValueAsString(request);

    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
  }

  @Test
  void pendingUserCanViewRegistrationStatus() throws Exception {
    MvcResult registration = register(validRegistration())
        .andExpect(status().isCreated())
        .andReturn();
    Number userId = readJson(registration, "$.id");

    mockMvc.perform(get("/api/auth/registration-status")
            .with(withPendingUser(userId.longValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.longValue()))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.rejectionReason").isEmpty());
  }

  @Test
  void registrationStatusWithoutAuthenticationReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/registration-status"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/auth/registration-status"));
  }

  @Test
  void changePasswordWithoutAuthenticationReturnsUnauthorized() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, "new-password-456");

    mockMvc.perform(put("/api/auth/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/auth/password"));
  }

  private ResultActions register(RegisterRequest request) throws Exception {
    return mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }

  private RegisterRequest validRegistration() {
    return new RegisterRequest(
        "new.user@example.com",
        "03001234567",
        PASSWORD,
        "New User",
        "123 Test Street"
    );
  }

  private <T> T readJson(MvcResult result, String path) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
  }
}
