package com.recruitingtransactionos.coreapi.identityauth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtFilter,
      ObjectMapper objectMapper) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              objectMapper.writeValue(
                  response.getWriter(),
                  ApiResponseEnvelope.failure(new ApiErrorResponse(
                      "authentication_failed",
                      "authentication_required",
                      "Authentication is required.")));
            }))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/health").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtService jwtService(
      @Value("${rto.auth.jwt.secret:}") String secret,
      @Value("${rto.auth.jwt.issuer:recruiting-transaction-os}") String issuer,
      @Value("${rto.auth.jwt.access-token-ttl-seconds:1800}") long accessTokenTtlSeconds,
      @Value("${rto.auth.jwt.refresh-token-ttl-seconds:604800}") long refreshTokenTtlSeconds) {
    return new JwtService(secret, issuer, accessTokenTtlSeconds, refreshTokenTtlSeconds);
  }

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtService jwtService,
      IdentityAuthenticationPort port,
      Clock clock,
      ObjectMapper objectMapper) {
    return new JwtAuthenticationFilter(jwtService, port, clock, objectMapper);
  }

  @Bean
  Clock systemClock() {
    return Clock.systemUTC();
  }
}
