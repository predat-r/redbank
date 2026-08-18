package com.redmath.redbank.security;

import com.redmath.redbank.security.denylist.DenyListJwtAuthenticationConverter;
import com.redmath.redbank.security.denylist.TokenDenylistService;
import com.redmath.redbank.security.ratelimit.RateLimitingFilter;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {"/swagger-ui/**", "/swagger-ui.html",
      "/v3/api-docs/**", "/api/auth/csrf", "/api/auth/register", "/api/auth/login",
      "/api/auth/refresh", "/api/auth/logout", "/actuator/health"};

  private static final String[] REGISTRATION_STATUS_ENDPOINTS = {"/api/auth/registration-status"};

  private static final String[] ADMIN_ENDPOINTS = {"/api/admin/**"};

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      DenyListJwtAuthenticationConverter denyListJwtAuthenticationConverter,
      SecurityErrorResponseHandler securityErrorResponseHandler,
      ObjectProvider<RateLimitingFilter> rateLimitingFilterProvider) throws Exception {
    configureCsrfSessionAndCors(http);
    configureAuthorization(http);
    configureExceptionHandling(http, securityErrorResponseHandler);
    configureJwtResourceServer(http, denyListJwtAuthenticationConverter,
        securityErrorResponseHandler);
    addRateLimitingFilterIfAvailable(http, rateLimitingFilterProvider);

    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
    return new CsrfTokenRequestAttributeHandler();
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookieCustomizer(cookie -> cookie.sameSite("None").secure(true));
    repository.setCookiePath("/");
    return repository;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return converter;
  }

  @Bean
  DenyListJwtAuthenticationConverter denyListJwtAuthenticationConverter(
      TokenDenylistService tokenDenylistService) {
    return new DenyListJwtAuthenticationConverter(jwtAuthenticationConverter(),
        tokenDenylistService);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(TrustedOriginService trustedOriginService) {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(trustedOriginService.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Idempotency-Key",
            "ngrok-skip-browser-warning"));
    configuration.setExposedHeaders(List.of("X-Idempotent-Replayed", "X-Idempotency-Key"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private void configureCsrfSessionAndCors(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository())
            .csrfTokenRequestHandler(csrfTokenRequestHandler())
            .ignoringRequestMatchers("/api/auth/register", "/api/auth/login")).sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(Customizer.withDefaults());
  }

  private void configureAuthorization(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
        .requestMatchers(REGISTRATION_STATUS_ENDPOINTS).hasAnyRole("PENDING_USER", "ACCOUNT_HOLDER")
        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN").anyRequest()
        .hasAnyRole("ADMIN", "ACCOUNT_HOLDER"));
  }

  private void configureExceptionHandling(HttpSecurity http,
      SecurityErrorResponseHandler errorHandler) throws Exception {
    http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errorHandler)
        .accessDeniedHandler(errorHandler));
  }

  private void configureJwtResourceServer(HttpSecurity http,
      DenyListJwtAuthenticationConverter denyListJwtAuthenticationConverter,
      SecurityErrorResponseHandler errorHandler) throws Exception {
    http.oauth2ResourceServer(
        oauth2 -> oauth2.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(denyListJwtAuthenticationConverter)));
  }

  private void addRateLimitingFilterIfAvailable(HttpSecurity http,
      ObjectProvider<RateLimitingFilter> rateLimitingFilterProvider) {
    RateLimitingFilter filter = rateLimitingFilterProvider.getIfAvailable();

    if (filter != null) {
      http.addFilterBefore(filter, AuthorizationFilter.class);
    }
  }
}

