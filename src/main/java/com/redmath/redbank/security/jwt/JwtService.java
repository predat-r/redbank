package com.redmath.redbank.security.jwt;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.role.UserRoleRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final UserRoleRepository userRoleRepository;

  public String generateToken(User user) {
    Instant now = Instant.now();

    List<String> roles = userRoleRepository.findAllByUser_Id(user.getId()).stream()
        .map(userRole -> userRole.getRole().getName().name()).sorted().toList();

    JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now)
        .expiresAt(now.plus(1, ChronoUnit.HOURS)).subject(user.getEmail())
        .claim("userId", user.getId()).claim("roles", roles).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }
}