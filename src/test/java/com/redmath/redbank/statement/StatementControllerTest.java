package com.redmath.redbank.statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.statement.dto.StatementRequest;
import com.redmath.redbank.statement.dto.StatementResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StatementRequestService statementRequestService;

  @MockitoBean
  private AccountHolderService accountHolderService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void shouldRequestStatement() throws Exception {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now().minusDays(30));
    request.setToDate(LocalDate.now());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(10L);
    when(accountHolderService.getAccountHolderByUserId(5L)).thenReturn(accountHolder);

    StatementResponse response = new StatementResponse("Your statement will be sent");
    when(statementRequestService.requestStatement(any(), eq(10L))).thenReturn(response);

    mockMvc.perform(post("/api/accounts/me/statement")
            .with(com.redmath.redbank.common.AuthUtilities.withAccountHolder(5L))
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Your statement will be sent"));
  }
}
