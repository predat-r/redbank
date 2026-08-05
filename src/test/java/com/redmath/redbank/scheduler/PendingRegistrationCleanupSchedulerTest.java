package com.redmath.redbank.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PendingRegistrationCleanupSchedulerTest {

  @Mock
  private UserRepository userRepository;

  private PendingRegistrationCleanupScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new PendingRegistrationCleanupScheduler(userRepository);
  }

  @Test
  @DisplayName("expireStaleRegistrations handles empty pending users list")
  void expireStaleRegistrationsEmpty() {
    when(userRepository.findAllByStatus(eq(UserStatus.PENDING_APPROVAL), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    scheduler.expireStaleRegistrations();

    verify(userRepository).findAllByStatus(eq(UserStatus.PENDING_APPROVAL), any(Pageable.class));
  }

  @Test
  @DisplayName("expireStaleRegistrations rejects stale pending registrations created over 30 days ago")
  void expireStaleRegistrationsStaleUserRejected() {
    Instant thirtyOneDaysAgo = Instant.now().minus(31, ChronoUnit.DAYS);

    User staleUser = User.builder()
        .id(1L)
        .email("stale.user@example.com")
        .name("Stale User")
        .status(UserStatus.PENDING_APPROVAL)
        .createdAt(thirtyOneDaysAgo)
        .build();

    when(userRepository.findAllByStatus(eq(UserStatus.PENDING_APPROVAL), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(staleUser)));

    scheduler.expireStaleRegistrations();

    assertEquals(UserStatus.REJECTED, staleUser.getStatus());
    assertEquals("Automatically rejected: registration expired after 30 days", staleUser.getRejectionReason());
  }

  @Test
  @DisplayName("expireStaleRegistrations leaves fresh pending registrations unchanged")
  void expireStaleRegistrationsFreshUserUnchanged() {
    Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);

    User freshUser = User.builder()
        .id(2L)
        .email("fresh.user@example.com")
        .name("Fresh User")
        .status(UserStatus.PENDING_APPROVAL)
        .createdAt(tenDaysAgo)
        .build();

    when(userRepository.findAllByStatus(eq(UserStatus.PENDING_APPROVAL), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(freshUser)));

    scheduler.expireStaleRegistrations();

    assertEquals(UserStatus.PENDING_APPROVAL, freshUser.getStatus());
  }
}
