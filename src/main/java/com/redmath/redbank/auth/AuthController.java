package com.redmath.redbank.auth;

import com.redmath.redbank.auth.dto.ChangePasswordRequest;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.LoginResponse;
import com.redmath.redbank.auth.dto.RefreshTokenRequest;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.auth.dto.RegisterResponse;
import com.redmath.redbank.auth.dto.RegistrationStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;


  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(
      @Valid @RequestBody RegisterRequest request
  ) {
    return authService.register(request);
  }


  @PostMapping("/login")
  public LoginResponse login(
      @Valid @RequestBody LoginRequest request
  ) {
    return authService.login(request);
  }


  @PostMapping("/refresh")
  public LoginResponse refresh(
      @Valid @RequestBody RefreshTokenRequest request
  ) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @Valid @RequestBody RefreshTokenRequest request
  ) {
    authService.logout(request);
  }

  @PutMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody ChangePasswordRequest request
  ) {
    authService.changePassword(extractUserId(jwt), request);
  }


  @GetMapping("/registration-status")
  public RegistrationStatusResponse getRegistrationStatus(@AuthenticationPrincipal Jwt jwt) {
    return authService.getRegistrationStatus(extractUserId(jwt));
  }

  private long extractUserId(Jwt jwt) {
    Object claim = jwt.getClaim("userId");

    if (!(claim instanceof Number userId)) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }

    return userId.longValue();
  }

}
