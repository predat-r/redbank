package com.redmath.redbank.auth;

import com.redmath.redbank.auth.dto.ChangePasswordRequest;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.LoginResponse;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.auth.dto.RegisterResponse;
import com.redmath.redbank.auth.dto.RegistrationResult;
import com.redmath.redbank.auth.dto.RegistrationStatusResponse;
import com.redmath.redbank.security.TrustedOriginService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenCookieService refreshTokenCookieService;
  private final TrustedOriginService trustedOriginService;

  @GetMapping("/csrf")
  public CsrfToken csrfToken(CsrfToken csrfToken) {
    return csrfToken;
  }


  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletResponse servletResponse
  ) {
    RegistrationResult result = authService.register(request);
    refreshTokenCookieService.write(servletResponse, result.refreshToken());
    return result.response();
  }


  @PostMapping("/login")
  public LoginResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletResponse servletResponse
  ) {
    AuthenticationResult result = authService.login(request);
    refreshTokenCookieService.write(servletResponse, result.refreshToken());
    return result.response();
  }


  @PostMapping("/refresh")
  public LoginResponse refresh(
      @CookieValue(name = "${app.security.refresh-cookie.name:__Secure-refresh-token}",
          required = false) String refreshToken,
      @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
      HttpServletResponse servletResponse
  ) {
    trustedOriginService.requireTrusted(origin);
    AuthenticationResult result = authService.refresh(refreshToken);
    refreshTokenCookieService.write(servletResponse, result.refreshToken());
    return result.response();
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @CookieValue(name = "${app.security.refresh-cookie.name:__Secure-refresh-token}",
          required = false) String refreshToken,
      @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
      HttpServletResponse servletResponse
  ) {
    trustedOriginService.requireTrusted(origin);
    try {
      authService.logout(refreshToken);
    } finally {
      refreshTokenCookieService.clear(servletResponse);
    }
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
