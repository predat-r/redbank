package com.redmath.redbank.user;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redmath.redbank.common.MockMvcSecurityTestConfig;
import com.redmath.redbank.user.dto.UpdateMyProfileRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(MockMvcSecurityTestConfig.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void accountHolderCanUpdateOwnProfile() throws Exception {
    User user = createUser("profile@example.com", "03001112222");
    UpdateMyProfileRequest request = new UpdateMyProfileRequest(
        "Updated.Profile@Example.com", "03009998888", "Updated Name", "Updated Address");

    mockMvc.perform(patch("/api/users/me")
            .with(withAccountHolder(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getId()))
        .andExpect(jsonPath("$.email").value("updated.profile@example.com"))
        .andExpect(jsonPath("$.phoneNumber").value("03009998888"))
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.address").value("Updated Address"));
  }

  @Test
  void unauthenticatedUserCannotUpdateProfile() throws Exception {
    mockMvc.perform(patch("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new UpdateMyProfileRequest("new@example.com", "03009998888", "Test Name",
                    "Test Address"))))
        .andExpect(status().isUnauthorized());
  }

  private User createUser(String email, String phoneNumber) {
    Instant now = Instant.now();
    return userRepository.save(User.builder()
        .email(email)
        .phoneNumber(phoneNumber)
        .name("Original Name")
        .address("Original Address")
        .status(UserStatus.ACTIVE)
        .createdAt(now)
        .updatedAt(now)
        .build());
  }
}
