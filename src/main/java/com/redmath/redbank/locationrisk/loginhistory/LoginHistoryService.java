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
    return loginEventRepository.findTopByUserIdAndSuccessfulTrueOrderByOccurredAtDesc(userId);
  }

  public Optional<LoginEvent> getLatestSuccessfulLoginExcluding(Long userId,
      Long excludedLoginEventId) {
    return loginEventRepository.findTopByUserIdAndSuccessfulTrueAndIdNotOrderByOccurredAtDesc(
        userId, excludedLoginEventId);
  }

  public boolean hasUsedIpBeforeExcluding(Long userId, String ipAddress,
      Long excludedLoginEventId) {
    return loginEventRepository.existsByUserIdAndIpAddressAndIdNotAndSuccessfulTrue(userId,
        ipAddress, excludedLoginEventId);
  }
  

  public List<LoginEvent> getLatestLoginAttemptsExcluding(Long userId, Long excludedLoginEventId) {
    return loginEventRepository.findTop20ByUserIdAndIdNotOrderByOccurredAtDesc(userId,
        excludedLoginEventId);
  }

  public List<LoginEvent> getAttemptsByIpAddressExcluding(Long userId, String ipAddress,
      Long excludedLoginEventId) {
    return loginEventRepository.findTop20ByUserIdAndIpAddressAndIdNotOrderByOccurredAtDesc(userId,
        ipAddress, excludedLoginEventId);
  }
}
