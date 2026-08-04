package com.redmath.redbank.audit;

import com.redmath.redbank.audit.dto.AuditLogResponse;
import com.redmath.redbank.common.exception.InvalidSortException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "createdAt", "action",
      "targetType");
  private final AuditLogRepository auditLogRepository;
  private final UserService userService;

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Long actorUserId, AuditAction action, AuditTargetType targetType,
      String targetIdentifier, String details) {
    if (actorUserId == null) {
      throw new IllegalArgumentException("Audit actor user id is required");
    }

    if (action == null) {
      throw new IllegalArgumentException("Audit action is required");
    }

    if (targetType == null) {
      throw new IllegalArgumentException("Audit target type is required");
    }

    if (targetIdentifier == null || targetIdentifier.isBlank()) {
      throw new IllegalArgumentException("Audit target identifier is required");
    }

    String normalizedTargetIdentifier = targetIdentifier.trim();

    if (normalizedTargetIdentifier.length() > 100) {
      throw new IllegalArgumentException("Audit target identifier cannot exceed 100 characters");
    }

    String normalizedDetails = details == null ? null : details.trim();

    if (normalizedDetails != null && normalizedDetails.isEmpty()) {
      normalizedDetails = null;
    }

    if (normalizedDetails != null && normalizedDetails.length() > 1000) {
      throw new IllegalArgumentException("Audit details cannot exceed 1000 characters");
    }

    User actor = userService.findById(actorUserId).orElseThrow(UserNotFoundException::new);

    AuditLog auditLog = AuditLog.builder().actor(actor).action(action).targetType(targetType)
        .targetIdentifier(normalizedTargetIdentifier).details(normalizedDetails)
        .createdAt(Instant.now()).build();

    auditLogRepository.save(auditLog);
  }

  @Transactional(readOnly = true)
  public Page<AuditLogResponse> findAuditLogs(Pageable pageable) {
    validateSort(pageable);

    return auditLogRepository.findAll(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public AuditLogResponse findAuditLog(Long auditLogId) {
    AuditLog auditLog = auditLogRepository.findById(auditLogId)
        .orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + auditLogId));

    return toResponse(auditLog);
  }


  private void validateSort(Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
      if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
        throw new InvalidSortException(ALLOWED_SORT_FIELDS);
      }
    }
  }

  private AuditLogResponse toResponse(AuditLog auditLog) {
    User actor = auditLog.getActor();

    return new AuditLogResponse(auditLog.getId(), actor.getId(), actor.getEmail(),
        auditLog.getAction(), auditLog.getTargetType(), auditLog.getTargetIdentifier(),
        auditLog.getDetails(), auditLog.getCreatedAt());
  }
}