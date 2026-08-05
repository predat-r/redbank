package com.redmath.redbank.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.redmath.redbank.user.role.RoleName;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class AuthUtilities {

  private AuthUtilities() {
  }

  public static RequestPostProcessor withAdmin(long userId) {
    return withUser(userId, RoleName.ADMIN);
  }

  public static RequestPostProcessor withAccountHolder(long userId) {
    return withUser(userId, RoleName.ACCOUNT_HOLDER);
  }

  public static RequestPostProcessor withPendingUser(long userId) {
    return withUser(userId, RoleName.PENDING_USER);
  }

  public static RequestPostProcessor withUser(
      long userId,
      RoleName... roles
  ) {
    if (userId <= 0) {
      throw new IllegalArgumentException("Test user id must be positive");
    }

    if (roles == null || roles.length == 0) {
      throw new IllegalArgumentException("At least one role is required");
    }

    List<String> roleNames = Arrays.stream(roles)
        .map(RoleName::name)
        .toList();

    List<SimpleGrantedAuthority> authorities = roleNames.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();

    return jwt()
        .jwt(token -> token
            .subject("test-user-" + userId)
            .claim("userId", userId)
            .claim("roles", roleNames)
            .claim("tokenType", "access"))
        .authorities(authorities.toArray(GrantedAuthority[]::new));
  }
}