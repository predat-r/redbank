package com.redmath.redbank.audit;

public interface AuditActorResolver {
  String getActorEmail(Long actorUserId);
}
