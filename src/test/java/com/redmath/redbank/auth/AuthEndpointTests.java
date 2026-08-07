package com.redmath.redbank.auth;

import com.redmath.redbank.common.MockMvcSecurityTestConfig;

import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static com.redmath.redbank.common.AuthUtilities.withPendingUser;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.auth.dto.ChangePasswordRequest;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(MockMvcSecurityTestConfig.class)
class AuthEndpointTests {

  private static final String PASSWORD = "password123";
  private static final String FRONTEND_ORIGIN = "http://localhost:3001";
  private static final String REFRESH_COOKIE_NAME = "__Secure-refresh-token";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Test
  void registerWithValidRequestReturnsCreated() throws Exception {
    RegisterRequest request = validRegistration();

    register(request)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value(request.email()))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokens.refreshToken").doesNotExist())
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
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
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
    String refreshToken = refreshToken(registration);

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken))
            .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void malformedRefreshTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, "not-a-jwt"))
            .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
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
    String refreshToken = refreshToken(registration);

    mockMvc.perform(post("/api/auth/logout")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken))
            .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken))
            .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
  }

  @Test
  void refreshFromUntrustedOriginReturnsForbidden() throws Exception {
    MvcResult registration = register(validRegistration())
        .andExpect(status().isCreated())
        .andReturn();

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie(REFRESH_COOKIE_NAME, refreshToken(registration)))
            .header(HttpHeaders.ORIGIN, "https://attacker.example"))
        .andExpect(status().isForbidden());
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

  @Test
  void activeUserCanChangePassword() throws Exception {
    RegisteredUser registeredUser = registerAndActivateUser();
    String newPassword = "new-password-456";
    ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, newPassword);

    mockMvc.perform(put("/api/auth/password")
            .header("Authorization", "Bearer " + registeredUser.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    login(registeredUser.email(), PASSWORD)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));

    login(registeredUser.email(), newPassword)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").doesNotExist());
  }

  @Test
  void changePasswordWithIncorrectCurrentPasswordReturnsBadRequest() throws Exception {
    RegisteredUser registeredUser = registerAndActivateUser();
    ChangePasswordRequest request = new ChangePasswordRequest(
        "incorrect-password",
        "new-password-456"
    );

    mockMvc.perform(put("/api/auth/password")
            .header("Authorization", "Bearer " + registeredUser.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Current password is incorrect"))
        .andExpect(jsonPath("$.path").value("/api/auth/password"));
  }

  @Test
  void changePasswordToCurrentPasswordReturnsBadRequest() throws Exception {
    RegisteredUser registeredUser = registerAndActivateUser();
    ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, PASSWORD);

    mockMvc.perform(put("/api/auth/password")
            .header("Authorization", "Bearer " + registeredUser.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message")
            .value("New password must be different from the current password"))
        .andExpect(jsonPath("$.path").value("/api/auth/password"));
  }

  private ResultActions register(RegisterRequest request) throws Exception {
    return mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }

  private ResultActions login(String email, String password) throws Exception {
    LoginRequest request = new LoginRequest(email, password);
    return mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }

  private RegisteredUser registerAndActivateUser() throws Exception {
    RegisterRequest registration = validRegistration();
    MvcResult result = register(registration)
        .andExpect(status().isCreated())
        .andReturn();
    Number userId = readJson(result, "$.id");
    long adminUserId = userRepository.findByEmailIgnoreCase("admin@redbank.com")
        .orElseThrow()
        .getId();

    mockMvc.perform(post("/api/admin/registrations/{userId}/approve", userId.longValue())
            .with(withAdmin(adminUserId)))
        .andExpect(status().isNoContent());

    MvcResult loginResult = login(registration.email(), registration.password())
        .andExpect(status().isOk())
        .andReturn();
    String accessToken = readJson(loginResult, "$.accessToken");

    return new RegisteredUser(registration.email(), accessToken);
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

  private String refreshToken(MvcResult result) {
    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("HttpOnly"));
    assertTrue(setCookie.contains("Secure"));
    assertTrue(setCookie.contains("SameSite=None"));
    assertTrue(setCookie.contains("Path=/api/auth"));

    String prefix = REFRESH_COOKIE_NAME + "=";
    int valueStart = setCookie.indexOf(prefix);
    assertTrue(valueStart >= 0);
    valueStart += prefix.length();
    int valueEnd = setCookie.indexOf(';', valueStart);
    return setCookie.substring(valueStart, valueEnd);
  }

  private record RegisteredUser(String email, String accessToken) {
  }
}
