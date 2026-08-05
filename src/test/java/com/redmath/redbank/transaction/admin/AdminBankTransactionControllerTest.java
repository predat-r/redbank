package com.redmath.redbank.transaction.admin;

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
import com.redmath.redbank.transaction.request.TransferRequest;
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
class AdminBankTransactionControllerTest {

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
  @DisplayName("GET /api/admin/transactions - Returns UNAUTHORIZED when unauthenticated")
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
  @DisplayName("GET /api/admin/transactions - Returns FORBIDDEN for ACCOUNT_HOLDER role")
  void adminEndpointsWithAccountHolderRoleReturnForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/transactions")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isForbidden());
  }

  // --- POST /api/admin/deposits ---

  @Test
  @DisplayName("POST /api/admin/deposits - Success for ADMIN")
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
  @DisplayName("POST /api/admin/deposits - Returns NOT_FOUND for non-existent account")
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
  @DisplayName("POST /api/admin/deposits - Returns BAD_REQUEST for invalid amount")
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
  @DisplayName("GET /api/admin/transactions - Success for ADMIN")
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
  @DisplayName("GET /api/admin/accounts/{accountNumber}/transactions - Success for ADMIN")
  void getTransactionsForSpecificAccountAsAdminReturnsOk() throws Exception {
    createDeposit(johnAccount.getAccountNumber(), new BigDecimal("150.00"));

    mockMvc.perform(get("/api/admin/accounts/{accountNumber}/transactions", johnAccount.getAccountNumber())
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].destinationAccountNumber").value(johnAccount.getAccountNumber()));
  }

  @Test
  @DisplayName("GET /api/admin/accounts/{accountNumber}/transactions - Returns NOT_FOUND for non-existent account")
  void getTransactionsForNonExistentAccountReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/admin/accounts/{accountNumber}/transactions", "RB9999999999")
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account holder not found"));
  }

  // --- GET /api/admin/transactions/{id} ---

  @Test
  @DisplayName("GET /api/admin/transactions/{id} - Success for deposit transaction")
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
  @DisplayName("GET /api/admin/transactions/{id} - Success for transfer transaction with full details")
  void getTransactionByIdForTransferAsAdminReturnsSourceAndDestinationDetails() throws Exception {
    User janeUser = createOrGetAccountHolder("jane.admin.test@example.com", "Jane Smith", "03009998866");
    AccountHolder janeAccount = accountHolderRepository.findByUserId(janeUser.getId()).orElseThrow();

    createDeposit(johnAccount.getAccountNumber(), new BigDecimal("500.00"));

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAmount(new BigDecimal("200.00"));
    transferRequest.setDestinationAccountNumber(janeAccount.getAccountNumber());
    transferRequest.setDescription("Admin test transfer");

    MvcResult transferResult = mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(transferRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    Number transferId = readJson(transferResult, "$.id");

    mockMvc.perform(get("/api/admin/transactions/{id}", transferId.longValue())
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(transferId.longValue()))
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andExpect(jsonPath("$.sourceAccountNumber").value(johnAccount.getAccountNumber()))
        .andExpect(jsonPath("$.sourceAccountCurrency").value("USD"))
        .andExpect(jsonPath("$.sourceAccountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.sourceUserName").value(johnUser.getName()))
        .andExpect(jsonPath("$.sourceUserEmail").value(johnUser.getEmail()))
        .andExpect(jsonPath("$.sourceUserPhoneNumber").value(johnUser.getPhoneNumber()))
        .andExpect(jsonPath("$.destinationAccountNumber").value(janeAccount.getAccountNumber()))
        .andExpect(jsonPath("$.destinationAccountCurrency").value("USD"))
        .andExpect(jsonPath("$.destinationAccountStatus").value("ACTIVE"))
        .andExpect(jsonPath("$.destinationUserName").value(janeUser.getName()))
        .andExpect(jsonPath("$.destinationUserEmail").value(janeUser.getEmail()))
        .andExpect(jsonPath("$.destinationUserPhoneNumber").value(janeUser.getPhoneNumber()));
  }

  @Test
  @DisplayName("GET /api/admin/transactions/{id} - Returns NOT_FOUND for non-existent transaction ID")
  void getTransactionByNonExistentIdReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/admin/transactions/{id}", 999999L)
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Transaction not found with ID: 999999"));
  }

  // --- GET /api/admin/transactions/reference/{reference} ---

  @Test
  @DisplayName("GET /api/admin/transactions/reference/{reference} - Success for ADMIN")
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
  @DisplayName("GET /api/admin/transactions/reference/{reference} - Returns NOT_FOUND for non-existent reference")
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
