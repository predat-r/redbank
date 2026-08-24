package com.redmath.redbank.user;

import com.redmath.redbank.audit.AuditActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuditActorResolver implements AuditActorResolver {

  private final UserRepository userRepository;

  @Override
  public String getActorEmail(Long actorUserId) {
    if (actorUserId == null) {
      return "unknown@redbank.com";
    }
    return userRepository.findById(actorUserId)
        .map(User::getEmail)
        .orElse("unknown@redbank.com");
  }
}
