package com.redmath.redbank.audit;

@FunctionalInterface
public interface AuditActorResolver {

  String getActorEmail(Long actorUserId);
}
