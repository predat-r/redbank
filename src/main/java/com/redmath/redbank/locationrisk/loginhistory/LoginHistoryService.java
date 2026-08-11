package com.redmath.redbank.locationrisk.loginhistory;

import com.redmath.redbank.locationrisk.login.LoginEvent;
import com.redmath.redbank.locationrisk.login.LoginEventRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

  private final LoginEventRepository loginEventRepository;

  public Optional<LoginEvent> getLatestSuccessfulLogin(Long userId) {
    return loginEventRepository
        .findTopByUserIdAndSuccessfulTrueOrderByOccurredAtDesc(userId);
  }

  public boolean hasUsedIpBefore(Long userId, String ipAddress) {
    return loginEventRepository
        .existsByUserIdAndIpAddressAndSuccessfulTrue(userId, ipAddress);
  }

  public List<LoginEvent> getAttemptsByIpAddress(
      Long userId,
      String ipAddress
  ) {
    return loginEventRepository
        .findTop20ByUserIdAndIpAddress(userId, ipAddress);
  }

  public List<LoginEvent> getLatestLoginAttempts(Long userId) {
    return loginEventRepository
        .findTop20ByUserIdOrderByOccurredAtDesc(userId);
  }
}