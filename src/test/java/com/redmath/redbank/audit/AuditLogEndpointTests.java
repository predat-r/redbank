package com.redmath.redbank.audit;

import static com.redmath.redbank.common.AuthUtilities.withAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogEndpointTests {

  private static final String ADMIN_EMAIL = "admin@redbank.com";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AuditLogRepository auditLogRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void adminCanListAuditLogs() throws Exception {
    AuditLog auditLog = saveAuditLog(
        AuditAction.REGISTRATION_APPROVED,
        AuditTargetType.USER,
        "list-user-123",
        "Registration approved from endpoint test"
    );

    mockMvc.perform(get("/api/admin/audit-logs")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.id == " + auditLog.getId() + ")]")
            .isNotEmpty());
  }

  @Test
  void adminCanPageAndSortAuditLogs() throws Exception {
    saveAuditLog(
        AuditAction.ACCOUNT_CREATED,
        AuditTargetType.ACCOUNT,
        "page-account-1",
        null
    );
    AuditLog newestAuditLog = saveAuditLog(
        AuditAction.ACCOUNT_FROZEN,
        AuditTargetType.ACCOUNT,
        "page-account-2",
        "Account frozen"
    );

    mockMvc.perform(get("/api/admin/audit-logs")
            .param("page", "0")
            .param("size", "1")
            .param("sort", "id,desc")
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(newestAuditLog.getId()))
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.size").value(1))
        .andExpect(jsonPath("$.page.totalElements").isNumber());
  }

  @Test
  void adminCanViewAuditLog() throws Exception {
    AuditLog auditLog = saveAuditLog(
        AuditAction.REGISTRATION_REJECTED,
        AuditTargetType.USER,
        "view-user-456",
        "Registration documents were invalid"
    );

    mockMvc.perform(get("/api/admin/audit-logs/{auditLogId}", auditLog.getId())
            .with(withAdmin(adminUserId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(auditLog.getId()))
        .andExpect(jsonPath("$.actorUserId").value(adminUserId()))
        .andExpect(jsonPath("$.actorEmail").value(ADMIN_EMAIL))
        .andExpect(jsonPath("$.action").value("REGISTRATION_REJECTED"))
        .andExpect(jsonPath("$.targetType").value("USER"))
        .andExpect(jsonPath("$.targetIdentifier").value("view-user-456"))
        .andExpect(jsonPath("$.details").value("Registration documents were invalid"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  private AuditLog saveAuditLog(
      AuditAction action,
      AuditTargetType targetType,
      String targetIdentifier,
      String details
  ) {
    AuditLog auditLog = AuditLog.builder()
        .actor(adminUser())
        .action(action)
        .targetType(targetType)
        .targetIdentifier(targetIdentifier)
        .details(details)
        .createdAt(Instant.now())
        .build();

    return auditLogRepository.saveAndFlush(auditLog);
  }

  private User adminUser() {
    return userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseThrow();
  }

  private long adminUserId() {
    return adminUser().getId();
  }
}
