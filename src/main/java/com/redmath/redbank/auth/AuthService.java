package com.redmath.redbank.auth;

import com.redmath.redbank.auth.dto.ChangePasswordRequest;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.LoginResponse;
import com.redmath.redbank.auth.dto.RefreshTokenRequest;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.auth.dto.RegisterResponse;
import com.redmath.redbank.common.exception.DuplicateUserException;
import com.redmath.redbank.common.exception.InvalidCredentialsException;
import com.redmath.redbank.common.exception.InvalidPasswordChangeException;
import com.redmath.redbank.common.exception.InvalidRefreshTokenException;
import com.redmath.redbank.common.exception.UserAccountNotActiveException;
import com.redmath.redbank.security.jwt.JwtService;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtDecoder refreshJwtDecoder;

  public AuthService(
      UserService userService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder
  ) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshJwtDecoder = refreshJwtDecoder;
  }

  public RegisterResponse register(RegisterRequest request) {
    String normalizedEmail = request.email()
        .trim()
        .toLowerCase(Locale.ROOT);

    String normalizedPhoneNumber = request.phoneNumber().trim();

    if (userService.existsByEmail(normalizedEmail)) {
      throw new DuplicateUserException("Email is already registered");
    }

    if (userService.existsByPhoneNumber(normalizedPhoneNumber)) {
      throw new DuplicateUserException("Phone number is already registered");
    }

    Instant now = Instant.now();

    User user = User.builder()
        .email(normalizedEmail)
        .phoneNumber(normalizedPhoneNumber)
        .passwordHash(passwordEncoder.encode(request.password()))
        .name(request.name().trim())
        .address(request.address().trim())
        .status(UserStatus.PENDING_APPROVAL)
        .createdAt(now)
        .updatedAt(now)
        .build();

    User savedUser = userService.save(user);

    return new RegisterResponse(
        savedUser.getId(),
        savedUser.getEmail(),
        savedUser.getStatus()
    );
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    String normalizedEmail = request.email()
        .trim()
        .toLowerCase(Locale.ROOT);

    User user = userService.findByEmailForUpdate(normalizedEmail)
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UserAccountNotActiveException();
    }

    user.incrementRefreshTokenVersion(Instant.now());

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return new LoginResponse(accessToken, refreshToken, "Bearer");
  }

  @Transactional
  public LoginResponse refresh(RefreshTokenRequest request) {
    Jwt jwt = decodeRefreshToken(request.refreshToken());

    long userId = requiredLongClaim(jwt, "userId");
    long tokenVersion = requiredLongClaim(jwt, "refreshTokenVersion");

    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(InvalidRefreshTokenException::new);

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UserAccountNotActiveException();
    }

    if (tokenVersion != user.getRefreshTokenVersion()) {
      throw new InvalidRefreshTokenException();
    }

    user.incrementRefreshTokenVersion(Instant.now());

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return new LoginResponse(accessToken, refreshToken, "Bearer");
  }

  @Transactional
  public void logout(RefreshTokenRequest request) {
    Jwt jwt = decodeRefreshToken(request.refreshToken());

    long userId = requiredLongClaim(jwt, "userId");
    long tokenVersion = requiredLongClaim(jwt, "refreshTokenVersion");

    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(InvalidRefreshTokenException::new);

    if (tokenVersion != user.getRefreshTokenVersion()) {
      throw new InvalidRefreshTokenException();
    }

    user.incrementRefreshTokenVersion(Instant.now());
  }

  @Transactional
  public void changePassword(
      Long userId,
      ChangePasswordRequest request
  ) {
    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(UserAccountNotActiveException::new);

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UserAccountNotActiveException();
    }

    if (!passwordEncoder.matches(
        request.currentPassword(),
        user.getPasswordHash()
    )) {
      throw new InvalidPasswordChangeException(
          "Current password is incorrect"
      );
    }

    if (passwordEncoder.matches(
        request.newPassword(),
        user.getPasswordHash()
    )) {
      throw new InvalidPasswordChangeException(
          "New password must be different from the current password"
      );
    }

    String newPasswordHash =
        passwordEncoder.encode(request.newPassword());

    user.changePasswordHash(newPasswordHash, Instant.now());
  }

  private long requiredLongClaim(Jwt jwt, String claimName) {
    Object claim = jwt.getClaim(claimName);

    if (!(claim instanceof Number number)) {
      throw new InvalidRefreshTokenException();
    }

    return number.longValue();
  }

  private Jwt decodeRefreshToken(String refreshToken) {
    try {
      return refreshJwtDecoder.decode(refreshToken);
    } catch (JwtException exception) {
      throw new InvalidRefreshTokenException();
    }
  }
}