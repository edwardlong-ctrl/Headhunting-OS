package com.recruitingtransactionos.coreapi.identityauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final IdentityAuthenticationPort port;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      IdentityAuthenticationPort port,
      Clock clock,
      ObjectMapper objectMapper) {
    this.jwtService = Objects.requireNonNull(jwtService, "jwtService must not be null");
    this.port = Objects.requireNonNull(port, "port must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }
    if (!authorization.startsWith("Bearer ")) {
      writeUnauthorized(response, "invalid_token", "Authentication failed.");
      return;
    }
    if (!jwtService.isConfigured()) {
      writeUnauthorized(response, "auth_not_configured", "Authentication failed.");
      return;
    }

    String token = authorization.substring("Bearer ".length()).strip();
    if (token.isBlank()) {
      writeUnauthorized(response, "invalid_token", "Authentication failed.");
      return;
    }

    try {
      JwtService.ParsedAccessToken parsed = jwtService.parseAccessToken(token);
      if (!isPrincipalActive(parsed.principal(), Instant.now(clock))) {
        SecurityContextHolder.clearContext();
        writeUnauthorized(response, "invalid_token", "Authentication failed.");
        return;
      }
      SecurityContextHolder.getContext().setAuthentication(
          new RtoAuthenticationToken(parsed.principal()));
      filterChain.doFilter(request, response);
    } catch (JwtException | IllegalArgumentException | IllegalStateException exception) {
      SecurityContextHolder.clearContext();
      writeUnauthorized(response, "invalid_token", "Authentication failed.");
    }
  }

  private boolean isPrincipalActive(RtoAuthenticatedPrincipal principal, Instant now) {
    IdentityAuthSession session = port.findActiveSessionBySessionId(principal.sessionId(), now)
        .orElse(null);
    if (session == null) {
      return false;
    }
    if (!session.organizationId().equals(principal.organizationId())
        || session.portalRole() != principal.portalRole()
        || !session.userAccountId().equals(principal.userAccountId())) {
      return false;
    }
    IdentityUserAccount account = port.findByOrganizationIdAndUserAccountId(
            principal.organizationId(), principal.userAccountId())
        .orElse(null);
    if (account == null || !account.isActiveForLogin()) {
      return false;
    }
    return port.hasActiveRoleAssignment(
        principal.organizationId(),
        principal.userAccountId(),
        principal.portalRole());
  }

  private void writeUnauthorized(HttpServletResponse response, String safeReason, String safeMessage)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponseEnvelope.failure(new ApiErrorResponse(
            "authentication_failed",
            safeReason,
            safeMessage)));
  }
}
