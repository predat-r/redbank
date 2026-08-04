package com.redmath.redbank.scheduler;

import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingRegistrationCleanupScheduler {

  private static final int EXPIRY_DAYS = 30;

  private final UserRepository userRepository;

  @Scheduled(cron = "${scheduler.registration-cleanup.cron:0 0 3 * * *}")
  @Transactional
  public void expireStaleRegistrations() {
    log.info("Pending registration cleanup job started");

    Instant cutoff = Instant.now().minus(EXPIRY_DAYS, ChronoUnit.DAYS);

    int[] count = {0};
    userRepository.findAllByStatus(UserStatus.PENDING_APPROVAL,
            org.springframework.data.domain.Pageable.unpaged())
        .forEach(user -> {
          if (user.getCreatedAt().isBefore(cutoff)) {
            user.rejectRegistration("Automatically rejected: registration expired after 30 days",
                Instant.now());
            count[0]++;
          }
        });

    log.info("Pending registration cleanup job completed: {} registrations expired", count[0]);
  }
}
