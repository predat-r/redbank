package com.redmath.redbank.transaction;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
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
class AdminBankTransactionEndpointTests {

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
  private AccountHolder johnAccount;

  @BeforeEach
  void setUp() throws Exception {
    adminUser = userRepository.findByEmailIgnoreCase("admin@redbank.com").orElseThrow();
    johnUser = createOrGetAccountHolder("john.admin.test@example.com", "John Doe", "03009998877");
    johnAccount = accountHolderRepository.findByUserId(johnUser.getId()).orElseThrow();
  }

  private User createOrGetAccountHolder(String email, String name, String phone) throws Exception {
    return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
      try {
        RegisterRequest registerRequest = new RegisterRequest(email, phone, "password123", name, "123 Admin Street");
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
  void adminEndpointsWithoutAuthReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/admin/transactions"))
        .andExpect(status().isUnauthorized());

    DepositRequest depositRequest = new DepositRequest();
    depositRequest.setAccountNumber(johnAccount.getAccountNumber());
    depositRequest.setAmount(new BigDecimal("100.00"));

    mockMvc.perform(post("/api/admin/deposits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(depositRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminEndpointsWithAccountHolderRoleReturnForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/transactions")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isForbidden());
  }

  // --- POST /api/admin/deposits ---

  @Test
  void depositWithValidRequestReturnsCreated() throws Exception {
    DepositRequest request = new DepositRequest();
    request.setAccountNumber(johnAccount.getAccountNumber());
    request.setAmount(new BigDecimal("1000.00"));
    request.setDescription("Initial admin deposit");

    mockMvc.perform(post("/api/admin/deposits")
            .with(withAdmin(adminUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.type").value("DEPOSIT"))
        .andExpect(jsonPath("$.amount").value(1000.00))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.destinationAccountNumber").value(johnAccount.getAccountNumber()));
  }

  @Test
  void depositToNonExistentAccountReturnsNotFound() throws Exception {
    DepositRequest request = new DepositRequest();
    request.setAccountNumber("RB9999999999");
    request.setAmount(new BigDecimal("500.00"));
    request.setDescription("Deposit to ghost account");

    mockMvc.perform(post("/api/admin/deposits")
            .with(withAdmin(adminUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Destination account not found"));
  }

  @Test
  void depositWithInvalidAmountReturnsBadRequest() throws Exception {
    DepositRequest request = new DepositRequest();
    request.setAccountNumber(johnAccount.getAccountNumber());
    request.setAmount(new BigDecimal("-50.00"));
    request.setDescription("Negative deposit");

    mockMvc.perform(post("/api/admin/deposits")
            .with(withAdmin(adminUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // --- GET /api/admin/transactions ---

  @Test
  void getAllTransactionsAsAdminReturnsOk() throws Exception {
    createDeposit(johnAccount.getAccountNumber(), new BigDecimal("250.00"));

    mockMvc.perform(get("/api/admin/transactions")
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
        .andExpect(jsonPath("$.content[0].amount").value(250.00));
  }

  // --- GET /api/admin/accounts/{accountNumber}/transactions ---

  @Test
  void getTransactionsForSpecificAccountAsAdminReturnsOk() throws Exception {
    createDeposit(johnAccount.getAccountNumber(), new BigDecimal("150.00"));

    mockMvc.perform(get("/api/admin/accounts/{accountNumber}/transactions", johnAccount.getAccountNumber())
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].destinationAccountNumber").value(johnAccount.getAccountNumber()));
  }

  // --- GET /api/admin/transactions/{id} ---

  @Test
  void getTransactionByIdAsAdminReturnsOk() throws Exception {
    MvcResult depositResult = createDeposit(johnAccount.getAccountNumber(), new BigDecimal("300.00"));
    Number transactionId = readJson(depositResult, "$.id");

    mockMvc.perform(get("/api/admin/transactions/{id}", transactionId.longValue())
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(transactionId.longValue()))
        .andExpect(jsonPath("$.type").value("DEPOSIT"))
        .andExpect(jsonPath("$.amount").value(300.00))
        .andExpect(jsonPath("$.destinationAccountNumber").value(johnAccount.getAccountNumber()))
        .andExpect(jsonPath("$.destinationUserName").value(johnUser.getName()));
  }

  @Test
  void getTransactionByNonExistentIdReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/admin/transactions/{id}", 999999L)
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Transaction not found with ID: 999999"));
  }

  // --- GET /api/admin/transactions/reference/{reference} ---

  @Test
  void getTransactionByReferenceAsAdminReturnsOk() throws Exception {
    MvcResult depositResult = createDeposit(johnAccount.getAccountNumber(), new BigDecimal("400.00"));
    String reference = readJson(depositResult, "$.transactionReference");

    mockMvc.perform(get("/api/admin/transactions/reference/{reference}", reference)
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionReference").value(reference))
        .andExpect(jsonPath("$.type").value("DEPOSIT"))
        .andExpect(jsonPath("$.amount").value(400.00));
  }

  @Test
  void getTransactionByNonExistentReferenceReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/admin/transactions/reference/{reference}", "TXN-NON-EXISTENT")
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Transaction not found with reference: TXN-NON-EXISTENT"));
  }

  // --- Helper methods ---

  private MvcResult createDeposit(String accountNumber, BigDecimal amount) throws Exception {
    DepositRequest request = new DepositRequest();
    request.setAccountNumber(accountNumber);
    request.setAmount(amount);
    request.setDescription("Test admin deposit");

    return mockMvc.perform(post("/api/admin/deposits")
            .with(withAdmin(adminUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private <T> T readJson(MvcResult result, String path) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
  }
}
