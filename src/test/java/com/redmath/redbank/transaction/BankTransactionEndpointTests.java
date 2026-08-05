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
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.transaction.request.WithdrawalRequest;
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
class BankTransactionEndpointTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  private User johnUser;
  private User janeUser;
  private User adminUser;
  private AccountHolder johnAccount;
  private AccountHolder janeAccount;

  @BeforeEach
  void setUp() throws Exception {
    adminUser = userRepository.findByEmailIgnoreCase("admin@redbank.com").orElseThrow();

    johnUser = createOrGetAccountHolder("john.test@example.com", "John Doe", "03001112233");
    janeUser = createOrGetAccountHolder("jane.test@example.com", "Jane Smith", "03004445566");

    johnAccount = accountHolderRepository.findByUserId(johnUser.getId()).orElseThrow();
    janeAccount = accountHolderRepository.findByUserId(janeUser.getId()).orElseThrow();
  }

  private User createOrGetAccountHolder(String email, String name, String phone) throws Exception {
    return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
      try {
        RegisterRequest registerRequest = new RegisterRequest(email, phone, "password123", name, "123 Street");
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
  void getMyTransactionsWithoutAuthReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/accounts/me/transactions"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createTransferWithoutAuthReturnsUnauthorized() throws Exception {
    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("100.00"));
    request.setDestinationAccountNumber(janeAccount.getAccountNumber());
    request.setDescription("Test transfer");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createWithdrawalWithoutAuthReturnsUnauthorized() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("50.00"));
    request.setDescription("Test withdrawal");

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createTransferWithAdminRoleReturnsForbidden() throws Exception {
    TransferRequest request = new TransferRequest();
    request.setAmount(new BigDecimal("100.00"));
    request.setDestinationAccountNumber(janeAccount.getAccountNumber());
    request.setDescription("Test transfer");

    mockMvc.perform(post("/api/accounts/me/transfers")
            .with(withAdmin(adminUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  // --- GET /api/accounts/me/transactions ---

  @Test
  void getMyTransactionsReturnsPageOfTransactions() throws Exception {
    depositToAccount(johnAccount.getAccountNumber(), new BigDecimal("500.00"));

    mockMvc.perform(get("/api/accounts/me/transactions")
            .with(withAccountHolder(johnUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
        .andExpect(jsonPath("$.content[0].amount").value(500.00));
  }

  // --- POST /api/accounts/me/withdrawals ---

  @Test
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
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.transactionReference").isNotEmpty());
  }

  @Test
  void withdrawalWithInsufficientBalanceReturnsBadRequest() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("10000.00"));
    request.setDescription("Overdraft attempt");

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Insufficient funds for this transaction"));
  }

  @Test
  void withdrawalWithInvalidAmountReturnsBadRequest() throws Exception {
    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("0.00"));
    request.setDescription("Invalid zero amount");

    mockMvc.perform(post("/api/accounts/me/withdrawals")
            .with(withAccountHolder(johnUser.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // --- POST /api/accounts/me/transfers ---

  @Test
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
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.sourceAccountNumber").value(johnAccount.getAccountNumber()))
        .andExpect(jsonPath("$.destinationAccountNumber").value(janeAccount.getAccountNumber()));
  }

  @Test
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
