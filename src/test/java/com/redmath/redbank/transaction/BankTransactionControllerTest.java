package com.redmath.redbank.transaction;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static com.redmath.redbank.common.AuthUtilities.withPendingUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.common.MockMvcSecurityTestConfig;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.transaction.request.WithdrawalRequest;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(MockMvcSecurityTestConfig.class)
class BankTransactionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  private User adminUser;
  private User johnUser;
  private User janeUser;
  private AccountHolder johnAccount;
  private AccountHolder janeAccount;

  @BeforeEach
  void setUp() throws Exception {
    adminUser = userRepository.findByEmailIgnoreCase("admin@redbank.com").orElseThrow();
    johnUser = createOrGetAccountHolder("john.txn.test@example.com", "John Doe", "03001234567");
    janeUser = createOrGetAccountHolder("jane.txn.test@example.com", "Jane Doe", "03007654321");

    johnAccount = accountHolderRepository.findByUserId(johnUser.getId()).orElseThrow();
    janeAccount = accountHolderRepository.findByUserId(janeUser.getId()).orElseThrow();
  }

  private User createOrGetAccountHolder(String email, String name, String phone) throws Exception {
    return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
      try {
        RegisterRequest registerRequest = new RegisterRequest(email, phone, "password123", name, "123 Test Street");
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

  // --- Authentication & Authorization ---

  @Test
  @DisplayName("GET /api/accounts/me/transactions - Returns UNAUTHORIZED when unauthenticated")
  void endpointsWithoutAuthReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/accounts/me/transactions"))
        .andExpect(status().isUnauthorized());

    WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
    withdrawalRequest.setAmount(new BigDecimal("50.00"));

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(withdrawalRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/accounts/me/transactions - Returns FORBIDDEN for non-account holder role")
  void endpointsWithForbiddenRoleReturnForbidden() throws Exception {
    mockMvc.perform(get("/api/accounts/me/transactions")
            .with(withPendingUser(johnUser.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/accounts/me/transactions - Returns BAD_REQUEST when JWT userId claim is missing")
  void endpointReturnsBadRequestWhenUserIdClaimMissing() throws Exception {
    mockMvc.perform(get("/api/accounts/me/transactions")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER"))))
        .andExpect(status().isBadRequest());
  }

  // --- GET /api/accounts/me/transactions ---

  @Test
  @DisplayName("GET /api/accounts/me/transactions - Success for ACCOUNT_HOLDER")
  void getMyTransactionsAsAccountHolderReturnsOk() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("500.00"));

    mockMvc.perform(get("/api/accounts/me/transactions")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
        .andExpect(jsonPath("$.content[0].amount").value(500.00));
  }

  @Test
  @DisplayName("GET /api/accounts/me/transactions - Supports pagination and sorting parameters")
  void getMyTransactionsWithPaginationParameters() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("100.00"));
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("200.00"));

    mockMvc.perform(get("/api/accounts/me/transactions")
            .param("page", "0")
            .param("size", "1")
            .param("sort", "createdAt,desc")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  @DisplayName("GET /api/accounts/me/transactions - Supports filter query parameters")
  void getMyTransactionsWithFilters() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("100.00"));

    // Filter by type
    mockMvc.perform(get("/api/accounts/me/transactions")
            .param("type", "DEPOSIT")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));

    // Filter by status
    mockMvc.perform(get("/api/accounts/me/transactions")
            .param("status", "COMPLETED")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

    // Filter by accountNumber
    mockMvc.perform(get("/api/accounts/me/transactions")
            .param("accountNumber", johnAccount.getAccountNumber())
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].destinationAccountNumber").value(johnAccount.getAccountNumber()));

    // Filter by date range
    java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
    mockMvc.perform(get("/api/accounts/me/transactions")
            .param("fromDate", now.minusDays(1).toString())
            .param("toDate", now.plusDays(1).toString())
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("GET /api/accounts/me/transactions/{id} - Success for transaction owner with account holder names")
  void getMyTransactionByIdSuccess() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("300.00"));

    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("150.00"));
    request.setDestinationAccountNumber(janeAccount.getAccountNumber());
    request.setDescription("Test transfer for detail view");

    MvcResult result = mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    Number txIdNum = readJson(result, "$.id");
    Long txId = txIdNum.longValue();

    mockMvc.perform(get("/api/accounts/me/transactions/{id}", txId)
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(txId))
        .andExpect(jsonPath("$.sourceAccountHolderName").value("John Doe"))
        .andExpect(jsonPath("$.destinationAccountHolderName").value("Jane Doe"))
        .andExpect(jsonPath("$.amount").value(150.00));
  }

  @Test
  @DisplayName("GET /api/accounts/me/transactions/{id} - Returns NOT_FOUND when transaction does not belong to user")
  void getMyTransactionByIdNotOwnerReturnsNotFound() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("300.00"));

    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("50.00"));
    request.setDescription("John's withdrawal");

    MvcResult result = mockMvc.perform(post("/api/accounts/me/withdrawals")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    Number txIdNum = readJson(result, "$.id");
    Long txId = txIdNum.longValue();

    mockMvc.perform(get("/api/accounts/me/transactions/{id}", txId)
            .with(withAccountHolder(janeUser.getId())))
        .andExpect(status().isNotFound());
  }

  // --- POST /api/accounts/me/withdrawals ---

  @Test
  @DisplayName("POST /api/accounts/me/withdrawals - Success with valid amount")
  void withdrawalWithValidAmountReturnsCreated() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("300.00"));

    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("100.00"));
    request.setDescription("ATM withdrawal");

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
        .andExpect(jsonPath("$.amount").value(100.00))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.sourceAccountNumber").value(johnAccount.getAccountNumber()));
  }

  @Test
  @DisplayName("POST /api/accounts/me/withdrawals - Returns BAD_REQUEST for invalid amount")
  void withdrawalWithInvalidAmountReturnsBadRequest() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("-10.00"));

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/accounts/me/withdrawals - Returns BAD_REQUEST for insufficient balance")
  void withdrawalWithInsufficientBalanceReturnsBadRequest() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("1000.00"));
    request.setDescription("Overdraft attempt");

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Insufficient funds for this transaction"));
  }

  // --- POST /api/accounts/me/transfers ---

  @Test
  @DisplayName("POST /api/accounts/me/transfers - Success with valid request")
  void transferWithValidRequestReturnsCreated() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("500.00"));

    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("200.00"));
    request.setDestinationAccountNumber(janeAccount.getAccountNumber());
    request.setDescription("Rent payment");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andExpect(jsonPath("$.amount").value(200.00))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.sourceAccountNumber").value(johnAccount.getAccountNumber()))
        .andExpect(jsonPath("$.destinationAccountNumber").value(janeAccount.getAccountNumber()));
  }

  @Test
  @DisplayName("POST /api/accounts/me/transfers - Returns BAD_REQUEST when source and destination match")
  void transferToSameAccountReturnsBadRequest() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("500.00"));

    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("100.00"));
    request.setDestinationAccountNumber(johnAccount.getAccountNumber());
    request.setDescription("Transfer to self");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Source and destination accounts must differ"));
  }

  @Test
  @DisplayName("POST /api/accounts/me/transfers - Returns NOT_FOUND for non-existent destination account")
  void transferToNonExistentAccountReturnsNotFound() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("500.00"));

    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("100.00"));
    request.setDestinationAccountNumber("RB9999999999");
    request.setDescription("Transfer to unknown account");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Destination account not found"));
  }

  @Test
  @DisplayName("POST /api/accounts/me/transfers - Returns BAD_REQUEST for insufficient balance")
  void transferWithInsufficientBalanceReturnsBadRequest() throws Exception {
    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("50000.00"));
    request.setDestinationAccountNumber(janeAccount.getAccountNumber());
    request.setDescription("Excessive transfer");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Insufficient funds for this transaction"));
  }

  // --- Helper methods ---

  private void depositToAccount(String accountNumber, BigDecimal amount) throws Exception {
    DepositRequest request = new DepositRequest();
    request.setAccountNumber(accountNumber);
    request.setAmount(amount);
    request.setDescription("Initial test deposit");

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
