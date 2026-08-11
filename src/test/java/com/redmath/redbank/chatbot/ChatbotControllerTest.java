package com.redmath.redbank.chatbot;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redmath.redbank.chatbot.dto.ChatRequest;
import com.redmath.redbank.chatbot.dto.ChatResponse;
import com.redmath.redbank.common.MockMvcSecurityTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MockMvcSecurityTestConfig.class)
class ChatbotControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ChatbotService chatbotService;

  @Test
  @DisplayName("POST /api/accounts/me/chat - Success for ACCOUNT_HOLDER")
  void chatSuccess() throws Exception {
    ChatResponse mockResponse = new ChatResponse("Here is your balance.", false);
    when(chatbotService.handle(anyString(), anyLong())).thenReturn(mockResponse);

    String requestBody = "{\"message\":\"What is my balance?\"}";

    mockMvc.perform(post("/api/accounts/me/chat")
            .with(withAccountHolder(1L))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reply").value("Here is your balance."))
        .andExpect(jsonPath("$.needsClarification").value(false));
  }

  @Test
  @DisplayName("POST /api/accounts/me/chat - Returns BAD_REQUEST when message is blank")
  void chatBlankMessage() throws Exception {
    String requestBody = "{\"message\":\"\"}";

    mockMvc.perform(post("/api/accounts/me/chat")
            .with(withAccountHolder(1L))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/accounts/me/chat - Returns BAD_REQUEST when userId claim is missing")
  void chatMissingUserIdClaim() throws Exception {
    String requestBody = "{\"message\":\"What is my balance?\"}";

    mockMvc.perform(post("/api/accounts/me/chat")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/accounts/me/chat - Returns UNAUTHORIZED when unauthenticated")
  void chatUnauthenticated() throws Exception {
    String requestBody = "{\"message\":\"What is my balance?\"}";

    mockMvc.perform(post("/api/accounts/me/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isUnauthorized());
  }
}
