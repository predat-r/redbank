package com.redmath.redbank.security.jwt;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.role.UserRoleRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final UserRoleRepository userRoleRepository;
  private final Duration accessTokenTtl;
  private final Duration refreshTokenTtl;

  public JwtService(
      JwtEncoder jwtEncoder,
      UserRoleRepository userRoleRepository,
      @Value("${spring.security.jwt.access-token-ttl}") Duration accessTokenTtl,
      @Value("${spring.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl
  ) {
    this.jwtEncoder = jwtEncoder;
    this.userRoleRepository = userRoleRepository;
    this.accessTokenTtl = accessTokenTtl;
    this.refreshTokenTtl = refreshTokenTtl;
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();

    List<String> roles = userRoleRepository.findAllByUser_Id(user.getId()).stream()
        .map(userRole -> userRole.getRole().getName().name()).sorted().toList();

    JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now)
        .expiresAt(now.plus(accessTokenTtl)).subject(user.getEmail())
        .claim("userId", user.getId()).claim("roles", roles)
        .id(UUID.randomUUID().toString())
        .claim("tokenType", "access").build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  public String generateRefreshToken(User user) {
    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuedAt(now)
        .expiresAt(now.plus(refreshTokenTtl))
        .subject(user.getEmail())
        .id(UUID.randomUUID().toString())
        .claim("userId", user.getId())
        .claim("tokenType", "refresh")
        .claim("refreshTokenVersion", user.getRefreshTokenVersion())
        .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims))
        .getTokenValue();
  }
}