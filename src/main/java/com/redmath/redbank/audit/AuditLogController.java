package com.redmath.redbank.audit;

import com.redmath.redbank.audit.dto.AuditLogResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

  private final AuditService auditService;

  @GetMapping
  public Page<AuditLogResponse> findAuditLogs(
      @PageableDefault(
          size = 20,
          sort = {"createdAt", "id"},
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    return auditService.findAuditLogs(pageable);
  }

  @GetMapping("/{auditLogId}")
  public AuditLogResponse findAuditLog(@PathVariable Long auditLogId) {
    return auditService.findAuditLog(auditLogId);
  }
}