package com.redmath.redbank.audit.dto;

import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditTargetType;
import java.time.Instant;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    String actorEmail,
    AuditAction action,
    AuditTargetType targetType,
    String targetIdentifier,
    String details,
    Instant createdAt
) {

}