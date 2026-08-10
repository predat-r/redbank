package com.redmath.redbank.locationrisk.login;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

  Optional<LoginEvent> findTopByUserIdAndSuccessfulTrueOrderByOccurredAtDesc(Long userId);

  boolean existsByUserIdAndIpAddressAndSuccessfulTrue(Long userId, String ipAddress);

  List<LoginEvent> findTop20ByUserIdOrderByOccurredAtDesc(Long userId);
}
