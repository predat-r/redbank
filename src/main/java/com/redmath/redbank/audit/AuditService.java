package com.redmath.redbank.audit;

import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

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
}