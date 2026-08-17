package com.redmath.redbank.locationrisk.login;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

  Optional<LoginEvent> findTopByUserIdAndSuccessfulTrueOrderByOccurredAtDesc(Long userId);

  Optional<LoginEvent> findTopByUserIdAndSuccessfulTrueAndIdNotOrderByOccurredAtDesc(
      Long userId,
      Long excludedLoginEventId
  );

  boolean existsByUserIdAndIpAddressAndIdNotAndSuccessfulTrue(
      Long userId,
      String ipAddress,
      Long excludedLoginEventId
  );


  List<LoginEvent> findTop20ByUserIdAndIdNotOrderByOccurredAtDesc(
      Long userId,
      Long excludedLoginEventId
  );

  List<LoginEvent> findTop20ByUserIdAndIpAddressAndIdNotOrderByOccurredAtDesc(
      Long userId,
      String ipAddress,
      Long excludedLoginEventId
  );

  List<LoginEvent> findTop20ByUserIdAndIpAddressAndSuccessfulFalseOrderByOccurredAtDesc(
      Long userId,
      String ipAddress
  );


}
