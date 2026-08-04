package com.redmath.redbank.scheduler;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenRevocationScheduler {

  private final UserRepository userRepository;

  @Scheduled(cron = "${scheduler.token-revocation.cron:0 0 2 * * *}")
  @Transactional
  public void revokeTokensForInactiveUsers() {
    log.info("Refresh token revocation job started");

    List<User> deactivatedUsers = userRepository.findAllByStatus(UserStatus.DEACTIVATED,
        org.springframework.data.domain.Pageable.unpaged()).getContent();

    List<User> rejectedUsers = userRepository.findAllByStatus(UserStatus.REJECTED,
        org.springframework.data.domain.Pageable.unpaged()).getContent();

    Instant now = Instant.now();
    int[] count = {0};

    deactivatedUsers.forEach(user -> {
      user.incrementRefreshTokenVersion(now);
      count[0]++;
    });

    rejectedUsers.forEach(user -> {
      user.incrementRefreshTokenVersion(now);
      count[0]++;
    });

    log.info("Refresh token revocation job completed: {} inactive user tokens revoked", count[0]);
  }
}
