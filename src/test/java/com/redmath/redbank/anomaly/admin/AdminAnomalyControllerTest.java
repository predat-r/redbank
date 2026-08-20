package com.redmath.redbank.anomaly.admin;

import static com.redmath.redbank.common.AuthUtilities.withAccountHolder;
import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redmath.redbank.anomaly.AnomalyReport;
import com.redmath.redbank.anomaly.AnomalyReportRepository;
import com.redmath.redbank.common.MockMvcSecurityTestConfig;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(MockMvcSecurityTestConfig.class)
class AdminAnomalyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  @Autowired
  private AnomalyReportRepository anomalyReportRepository;

  private User adminUser;
  private BankTransaction transaction;
  private AnomalyReport anomalyReport;

  @BeforeEach
  void setUp() {
    adminUser = userRepository.findByEmailIgnoreCase("admin@redbank.com").orElseThrow();

    transaction = new BankTransaction();
    transaction.setTransactionReference("TXN-ANOMALY-TEST");
    transaction.setType(TransactionType.WITHDRAWAL);
    transaction.setAmount(new BigDecimal("15000.00"));
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setCreatedAt(OffsetDateTime.now());
    transaction = bankTransactionRepository.saveAndFlush(transaction);

    anomalyReport = new AnomalyReport();
    anomalyReport.setTransaction(transaction);
    anomalyReport.setRiskScore(75);
    anomalyReport.setRecommendation("MANUAL_REVIEW");
    anomalyReport.setReasoning("High transaction amount");
    anomalyReport.setCreatedAt(OffsetDateTime.now());
    anomalyReport = anomalyReportRepository.saveAndFlush(anomalyReport);
  }

  @Test
  void adminCanGetAnomalyReport() throws Exception {
    mockMvc.perform(get("/api/admin/transactions/{id}/anomaly-report", transaction.getId())
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(anomalyReport.getId()))
        .andExpect(jsonPath("$.transactionId").value(transaction.getId()))
        .andExpect(jsonPath("$.riskScore").value(75))
        .andExpect(jsonPath("$.recommendation").value("MANUAL_REVIEW"))
        .andExpect(jsonPath("$.reasoning").value("High transaction amount"));
  }

  @Test
  void nonAdminCannotGetAnomalyReport() throws Exception {
    mockMvc.perform(get("/api/admin/transactions/{id}/anomaly-report", transaction.getId())
            .with(withAccountHolder(adminUser.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void returnsNotFoundForMissingReport() throws Exception {
    mockMvc.perform(get("/api/admin/transactions/{id}/anomaly-report", 999999L)
            .with(withAdmin(adminUser.getId())))
        .andExpect(status().isNotFound());
  }
}
