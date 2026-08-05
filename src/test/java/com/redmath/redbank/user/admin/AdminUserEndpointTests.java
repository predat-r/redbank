package com.redmath.redbank.user.admin;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.user.UserRepository;
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
class AdminUserEndpointTests {

  private static final String ADMIN_EMAIL = "admin@redbank.com";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

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
}
