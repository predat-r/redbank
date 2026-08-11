package com.redmath.redbank.security;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@ExtendWith(MockitoExtension.class)
class DenyListJwtAuthenticationConverterTest {

  @Mock
  private JwtAuthenticationConverter delegate;

  @Mock
  private TokenDenylistService denylistService;

  @Test
  void rejectsDenylistedJti() {
    Jwt jwt = jwt("blocked-jti");
    when(denylistService.isDenylisted("blocked-jti")).thenReturn(true);

    DenyListJwtAuthenticationConverter converter =
        new DenyListJwtAuthenticationConverter(delegate, denylistService);

    assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
  }

  @Test
  void delegatesAllowedJwt() {
    Jwt jwt = jwt("allowed-jti");
    UsernamePasswordAuthenticationToken expected =
        UsernamePasswordAuthenticationToken.authenticated("user", null, java.util.List.of());
    when(denylistService.isDenylisted("allowed-jti")).thenReturn(false);
    when(delegate.convert(jwt)).thenReturn(expected);

    DenyListJwtAuthenticationConverter converter =
        new DenyListJwtAuthenticationConverter(delegate, denylistService);

    assertSame(expected, converter.convert(jwt));
    verify(delegate).convert(jwt);
  }

  private Jwt jwt(String jti) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user@example.com")
        .jti(jti)
        .build();
  }
}
