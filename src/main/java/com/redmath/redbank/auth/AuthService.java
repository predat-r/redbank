package com.redmath.redbank.auth;

import com.redmath.redbank.auth.dto.ChangePasswordRequest;
import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.LoginResponse;
import com.redmath.redbank.auth.dto.RefreshTokenRequest;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.auth.dto.RegisterResponse;
import com.redmath.redbank.auth.dto.RegistrationStatusResponse;
import com.redmath.redbank.common.exception.DuplicateUserException;
import com.redmath.redbank.common.exception.InvalidCredentialsException;
import com.redmath.redbank.common.exception.InvalidPasswordChangeException;
import com.redmath.redbank.common.exception.InvalidRefreshTokenException;
import com.redmath.redbank.common.exception.UserAccountNotActiveException;
import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.security.jwt.JwtService;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import com.redmath.redbank.user.UserStatus;
import com.redmath.redbank.user.role.Role;
import com.redmath.redbank.user.role.RoleName;
import com.redmath.redbank.user.role.RoleRepository;
import com.redmath.redbank.user.role.UserRole;
import com.redmath.redbank.user.role.UserRoleId;
import com.redmath.redbank.user.role.UserRoleRepository;
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
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private static final String BEARER_PREFIX = "Bearer";

  public AuthService(UserService userService, PasswordEncoder passwordEncoder,
      JwtService jwtService, @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
      UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshJwtDecoder = refreshJwtDecoder;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
  }

  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

    String normalizedPhoneNumber = request.phoneNumber().trim();

    if (userService.existsByEmail(normalizedEmail)) {
      throw new DuplicateUserException("Email is already registered");
    }

    if (userService.existsByPhoneNumber(normalizedPhoneNumber)) {
      throw new DuplicateUserException("Phone number is already registered");
    }

    Instant now = Instant.now();

    User user = User.builder().email(normalizedEmail).phoneNumber(normalizedPhoneNumber)
        .passwordHash(passwordEncoder.encode(request.password())).name(request.name().trim())
        .address(request.address().trim()).status(UserStatus.PENDING_APPROVAL).createdAt(now)
        .updatedAt(now).build();

    User savedUser = userService.save(user);
    Role role = roleRepository.findByName(RoleName.PENDING_USER)
        .orElseThrow(() -> new IllegalStateException("PENDING_USER role is not configured"));
    UserRoleId userRoleId = new UserRoleId(savedUser.getId(), role.getId());
    UserRole userRole = UserRole.builder().id(userRoleId).user(savedUser).role(role).assignedAt(now)
        .build();
    userRoleRepository.save(userRole);
    savedUser.incrementRefreshTokenVersion(now);
    String accessToken = jwtService.generateAccessToken(savedUser);
    String refreshToken = jwtService.generateRefreshToken(savedUser);
    LoginResponse tokens = new LoginResponse(accessToken, refreshToken, BEARER_PREFIX);
    return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getStatus(),
        tokens);
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

    User user = userService.findByEmailForUpdate(normalizedEmail)
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING_APPROVAL) {
      throw new UserAccountNotActiveException();
    }

    user.incrementRefreshTokenVersion(Instant.now());

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return new LoginResponse(accessToken, refreshToken, BEARER_PREFIX);
  }

  @Transactional
  public LoginResponse refresh(RefreshTokenRequest request) {
    Jwt jwt = decodeRefreshToken(request.refreshToken());

    long userId = requiredLongClaim(jwt, "userId");
    long tokenVersion = requiredLongClaim(jwt, "refreshTokenVersion");

    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(InvalidRefreshTokenException::new);

    if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING_APPROVAL) {
      throw new UserAccountNotActiveException();
    }

    if (tokenVersion != user.getRefreshTokenVersion()) {
      throw new InvalidRefreshTokenException();
    }

    user.incrementRefreshTokenVersion(Instant.now());

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return new LoginResponse(accessToken, refreshToken, BEARER_PREFIX);
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
  public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(UserAccountNotActiveException::new);

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new UserAccountNotActiveException();
    }

    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidPasswordChangeException("Current password is incorrect");
    }

    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new InvalidPasswordChangeException(
          "New password must be different from the current password");
    }

    String newPasswordHash = passwordEncoder.encode(request.newPassword());

    user.changePasswordHash(newPasswordHash, Instant.now());
  }

  @Transactional(readOnly = true)
  public RegistrationStatusResponse getRegistrationStatus(Long userId) {
    User user = userService.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    return new RegistrationStatusResponse(
        user.getId(),
        user.getStatus(),
        user.getRejectionReason()
    );

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
    } catch (JwtException _) {
      throw new InvalidRefreshTokenException();
    }
  }


}