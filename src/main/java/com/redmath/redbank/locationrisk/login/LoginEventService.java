package com.redmath.redbank.locationrisk.login;

import com.redmath.redbank.locationrisk.login.dto.LoginEventDto;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginEventService {


  private final LoginEventRepository loginEventRepository;

  public LoginEvent createLoginEvent(LoginEventDto dto) {
    LoginEvent event = new LoginEvent();

    event.setUserId(dto.userId());
    event.setIpAddress(dto.ipAddress());
    event.setUserAgent(dto.userAgent());
    event.setDeviceIdentifier(dto.deviceIdentifier());
    event.setSuccessful(dto.successful());
    event.setFailureReason(dto.failureReason());
    event.setCity(dto.city());
    event.setCountry(dto.country());
    event.setAccessTokenJti(dto.accessTokenJti());
    event.setOccurredAt(Instant.now());

    return loginEventRepository.save(event);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public LoginEvent recordFailedLogin(Long userId, LoginContext context, String failureReason) {
    LoginEventDto dto = new LoginEventDto(userId, context.ipAddress(), context.userAgent(),
        context.deviceIdentifier(), false, failureReason, null, null, null);

    return createLoginEvent(dto);
  }

  public LoginEvent recordSuccessfulLogin(Long userId, LoginContext context,
      String accessTokenJti) {
    LoginEventDto dto = new LoginEventDto(userId, context.ipAddress(), context.userAgent(),
        context.deviceIdentifier(), true, null, null, null, accessTokenJti);

    return createLoginEvent(dto);
  }
}
