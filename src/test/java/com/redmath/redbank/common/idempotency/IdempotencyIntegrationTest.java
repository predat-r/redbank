package com.redmath.redbank.common.idempotency;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.common.MockMvcSecurityTestConfig;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(MockMvcSecurityTestConfig.class)
class IdempotencyIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  private User adminUser;
  private User senderUser;
  private User receiverUser;
  private AccountHolder senderAccount;
  private AccountHolder receiverAccount;

  @BeforeEach
  void setUp() throws Exception {
    adminUser = userRepository.findByEmailIgnoreCase("admin@redbank.com").orElseThrow();
    senderUser = createOrGetAccountHolder("idempotency.sender@example.com", "Sender Doe", "03001112223");
    receiverUser = createOrGetAccountHolder("idempotency.receiver@example.com", "Receiver Doe", "03003334445");

    senderAccount = accountHolderRepository.findByUserId(senderUser.getId()).orElseThrow();
    receiverAccount = accountHolderRepository.findByUserId(receiverUser.getId()).orElseThrow();

    depositToAccount(senderAccount.getAccountNumber(), new BigDecimal("1000.00"));
  }

  @Test
  @DisplayName("Sending duplicate requests with the same idempotency key replays the cached response")
  void testMultipleRequestsWithSameIdempotencyKey() throws Exception {
    String idempotencyKey = "unique-txn-idempotency-key-100";

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAmount(new BigDecimal("100.00"));
    transferRequest.setDestinationAccountNumber(receiverAccount.getAccountNumber());
    transferRequest.setDescription("Idempotent Transfer Test");

    String requestJson = objectMapper.writeValueAsString(transferRequest);

    // 1st Request: Executes the transfer and gets 201 Created without replayed header
    MvcResult firstResult = mockMvc.perform(post("/api/accounts/me/transfers")
            .header(IdempotencyService.IDEMPOTENCY_HEADER, idempotencyKey)
            .with(withAccountHolder(senderUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(header().doesNotExist(IdempotencyService.REPLAYED_HEADER))
        .andExpect(jsonPath("$.amount").value(100.00))
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andReturn();

    Number firstTxId = readJson(firstResult, "$.id");

    // 2nd Request (Duplicate with exact same idempotency key): Replays cached response with X-Idempotent-Replayed: true
    mockMvc.perform(post("/api/accounts/me/transfers")
            .header(IdempotencyService.IDEMPOTENCY_HEADER, idempotencyKey)
            .with(withAccountHolder(senderUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(header().string(IdempotencyService.REPLAYED_HEADER, "true"))
        .andExpect(jsonPath("$.id").value(firstTxId))
        .andExpect(jsonPath("$.amount").value(100.00));

    // 3rd Request (Same idempotency key, but different request payload): Returns 409 Conflict
    TransferRequest modifiedRequest = new TransferRequest();
    modifiedRequest.setAmount(new BigDecimal("200.00"));
    modifiedRequest.setDestinationAccountNumber(receiverAccount.getAccountNumber());
    modifiedRequest.setDescription("Modified Transfer Amount");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .header(IdempotencyService.IDEMPOTENCY_HEADER, idempotencyKey)
            .with(withAccountHolder(senderUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(modifiedRequest)))
        .andExpect(status().isConflict());
  }

  private User createOrGetAccountHolder(String email, String name, String phone) throws Exception {
    return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
      try {
        RegisterRequest registerRequest = new RegisterRequest(email, phone, "password123", name, "123 Test St");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        Number userIdNum = readJson(regResult, "$.id");
        Long userId = userIdNum.longValue();

        mockMvc.perform(post("/api/admin/registrations/{userId}/approve", userId)
                .with(withAdmin(adminUser.getId())))
            .andExpect(status().isNoContent());

        return userRepository.findById(userId).orElseThrow();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void depositToAccount(String accountNumber, BigDecimal amount) throws Exception {
    DepositRequest request = new DepositRequest();
    request.setAccountNumber(accountNumber);
    request.setAmount(amount);
    request.setDescription("Test Deposit");

    mockMvc.perform(post("/api/admin/deposits")
            .with(withAdmin(adminUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  private <T> T readJson(MvcResult result, String path) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
  }
}
