package com.redmath.redbank.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public class DenyListJwtAuthenticationConverter implements
    Converter<Jwt, AbstractAuthenticationToken> {

  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final TokenDenylistService tokenDenylistService;

  public DenyListJwtAuthenticationConverter(JwtAuthenticationConverter jwtAuthenticationConverter,
      TokenDenylistService tokenDenylistService) {
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.tokenDenylistService = tokenDenylistService;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt source) {
    String jti = source.getId();
    if (tokenDenylistService.isDenylisted(jti)) {
      throw new BadCredentialsException("Access token has been revoked");
    }
    return jwtAuthenticationConverter.convert(source);
  }

}
