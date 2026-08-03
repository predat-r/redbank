package com.redmath.redbank.auth;

import com.redmath.redbank.auth.AuthService;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.LoginResponse;
import com.redmath.redbank.auth.dto.RefreshTokenRequest;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.auth.dto.RegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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
}