package com.redmath.redbank.auth;

import com.redmath.redbank.auth.dto.LoginRequest;
import com.redmath.redbank.auth.dto.LoginResponse;
import com.redmath.redbank.auth.dto.RegisterRequest;
import com.redmath.redbank.auth.dto.RegisterResponse;
import com.redmath.redbank.security.jwt.JwtService;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;


  public RegisterResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();

    String normalizedPhoneNumber = request.phoneNumber().trim();

    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new IllegalArgumentException("Email is already registered");
    }

    if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
      throw new IllegalArgumentException("Phone number is already registered");
    }

    Instant now = Instant.now();

    User user = User.builder().email(normalizedEmail).phoneNumber(normalizedPhoneNumber)
        .passwordHash(passwordEncoder.encode(request.password())).name(request.name().trim())
        .address(request.address().trim()).status(UserStatus.PENDING_APPROVAL).createdAt(now)
        .updatedAt(now).build();

    User savedUser = userRepository.save(user);

    return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getStatus());
  }


  public LoginResponse login(LoginRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();

    User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new IllegalStateException("User account is not active");
    }

    String accessToken = jwtService.generateToken(user);

    return new LoginResponse(accessToken, "Bearer");
  }

}