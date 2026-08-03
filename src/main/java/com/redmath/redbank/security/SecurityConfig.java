package com.redmath.redbank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

    authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return authenticationConverter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      SecurityErrorResponseHandler securityErrorResponseHandler) throws Exception {
    http.csrf(csrf -> csrf.disable()).sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                    "/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(securityErrorResponseHandler)
                .accessDeniedHandler(securityErrorResponseHandler)).oauth2ResourceServer(
            oauth2 -> oauth2.authenticationEntryPoint(securityErrorResponseHandler)
                .accessDeniedHandler(securityErrorResponseHandler)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    return http.build();
  }
}
