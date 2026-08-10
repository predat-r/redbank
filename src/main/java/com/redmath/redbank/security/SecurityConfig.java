package com.redmath.redbank.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/v3/api-docs/**",
      "/api/auth/csrf",
      "/api/auth/register",
      "/api/auth/login",
      "/api/auth/refresh",
      "/api/auth/logout"
  };

  private static final String[] REGISTRATION_STATUS_ENDPOINTS = {
      "/api/auth/registration-status"
  };

  private static final String[] ADMIN_ENDPOINTS = {
      "/api/admin/**"
  };

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter =
        new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return converter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      SecurityErrorResponseHandler securityErrorResponseHandler
  ) throws Exception {
    configureBasicSecurity(http);
    configureAuthorization(http);
    configureExceptionHandling(http, securityErrorResponseHandler);
    configureJwtAuthentication(http, jwtAuthenticationConverter, securityErrorResponseHandler);

    return http.build();
  }

  private void configureBasicSecurity(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository())
            .ignoringRequestMatchers(
                "/api/auth/register",
                "/api/auth/login"))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults());
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    return repository;
  }

  private void configureAuthorization(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(PUBLIC_ENDPOINTS)
        .permitAll()
        .requestMatchers(REGISTRATION_STATUS_ENDPOINTS)
        .hasAnyRole("PENDING_USER", "ACCOUNT_HOLDER")
        .requestMatchers(ADMIN_ENDPOINTS)
        .hasRole("ADMIN")
        .anyRequest()
        .hasAnyRole("ADMIN", "ACCOUNT_HOLDER"));
  }

  private void configureExceptionHandling(
      HttpSecurity http,
      SecurityErrorResponseHandler errorHandler
  ) throws Exception {
    http.exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint(errorHandler)
        .accessDeniedHandler(errorHandler));
  }

  private void configureJwtAuthentication(
      HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      SecurityErrorResponseHandler errorHandler
  ) throws Exception {
    http.oauth2ResourceServer(oauth2 -> oauth2
        .authenticationEntryPoint(errorHandler)
        .accessDeniedHandler(errorHandler)
        .jwt(jwt -> jwt
            .jwtAuthenticationConverter(jwtAuthenticationConverter)));
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      TrustedOriginService trustedOriginService
  ) {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(trustedOriginService.allowedOrigins());
    configuration.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of(
        "Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Idempotency-Key"));
    configuration.setExposedHeaders(List.of(
        "X-Idempotent-Replayed", "X-Idempotency-Key"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
