package com.recruitingtransactionos.coreapi.identityauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private static final Instant NOW = Instant.now().plus(Duration.ofMinutes(5));
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000190401");
  private static final UUID USER_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000190402");
  private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000190403");

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void activeSessionBuildsAuthenticatedPrincipal() throws Exception {
    JwtAuthenticationFilter filter = filterWith(activeSessionPort(true, true, principal()));
    MockHttpServletResponse response = perform(filter, validBearerToken(principal()));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isInstanceOf(RtoAuthenticationToken.class);
  }

  @Test
  void revokedOrMissingSessionFailsClosedWith401() throws Exception {
    IdentityAuthenticationPort port = mock(IdentityAuthenticationPort.class);
    when(port.findActiveSessionBySessionId(SESSION_ID, NOW)).thenReturn(Optional.empty());
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        jwtService(), port, Clock.fixed(NOW, ZoneOffset.UTC), new ObjectMapper());

    MockHttpServletResponse response = perform(filter, validBearerToken(principal()));

    assertUnauthorized(response);
  }

  @Test
  void inactiveAccountFailsClosedWith401() throws Exception {
    IdentityAuthenticationPort port = activeSessionPort(false, true, principal());
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        jwtService(), port, Clock.fixed(NOW, ZoneOffset.UTC), new ObjectMapper());

    MockHttpServletResponse response = perform(filter, validBearerToken(principal()));

    assertUnauthorized(response);
  }

  @Test
  void revokedRoleAssignmentFailsClosedWith401() throws Exception {
    IdentityAuthenticationPort port = activeSessionPort(true, false, principal());
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        jwtService(), port, Clock.fixed(NOW, ZoneOffset.UTC), new ObjectMapper());

    MockHttpServletResponse response = perform(filter, validBearerToken(principal()));

    assertUnauthorized(response);
  }

  @Test
  void mismatchedSessionOrganizationFailsClosedWith401() throws Exception {
    RtoAuthenticatedPrincipal principal = principal();
    IdentityAuthenticationPort port = mock(IdentityAuthenticationPort.class);
    when(port.findActiveSessionBySessionId(SESSION_ID, NOW)).thenReturn(Optional.of(
        new IdentityAuthSession(
            SESSION_ID,
            UUID.fromString("00000000-0000-0000-0000-000000190499"),
            USER_ACCOUNT_ID,
            PortalRole.CONSULTANT,
            "refresh-hash",
            NOW.plusSeconds(600),
            null,
            NOW,
            NOW,
            1)));
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        jwtService(), port, Clock.fixed(NOW, ZoneOffset.UTC), new ObjectMapper());

    MockHttpServletResponse response = perform(filter, validBearerToken(principal));

    assertUnauthorized(response);
  }

  @Test
  void wrongIssuerFailsClosedWith401() throws Exception {
    JwtAuthenticationFilter filter = filterWith(activeSessionPort(true, true, principal()));
    JwtService wrongIssuer = new JwtService(
        "0123456789abcdef0123456789abcdef",
        "wrong-issuer",
        1800,
        604800);

    MockHttpServletResponse response = perform(
        filter,
        "Bearer " + wrongIssuer.issueAccessToken(principal(), NOW));

    assertUnauthorized(response);
  }

  private static IdentityAuthenticationPort activeSessionPort(
      boolean activeAccount,
      boolean activeRole,
      RtoAuthenticatedPrincipal principal) {
    IdentityAuthenticationPort port = mock(IdentityAuthenticationPort.class);
    when(port.findActiveSessionBySessionId(SESSION_ID, NOW)).thenReturn(Optional.of(
        new IdentityAuthSession(
            SESSION_ID,
            principal.organizationId(),
            principal.userAccountId(),
            principal.portalRole(),
            "refresh-hash",
            NOW.plusSeconds(600),
            null,
            NOW,
            NOW,
            1)));
    when(port.findByOrganizationIdAndUserAccountId(principal.organizationId(), principal.userAccountId()))
        .thenReturn(Optional.of(new IdentityUserAccount(
            principal.userAccountId(),
            principal.organizationId(),
            "consultant@example.com",
            principal.displayName(),
            activeAccount ? "active" : "suspended",
            "$2a$10$abcdefghijklmnopqrstuv",
            NOW)));
    when(port.hasActiveRoleAssignment(
        principal.organizationId(),
        principal.userAccountId(),
        principal.portalRole())).thenReturn(activeRole);
    return port;
  }

  private static MockHttpServletResponse perform(JwtAuthenticationFilter filter, String bearerToken)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/consultant/companies");
    request.addHeader(HttpHeaders.AUTHORIZATION, bearerToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();
    filter.doFilter(request, response, filterChain);
    return response;
  }

  private static JwtAuthenticationFilter filterWith(IdentityAuthenticationPort port) {
    return new JwtAuthenticationFilter(
        jwtService(),
        port,
        Clock.fixed(NOW, ZoneOffset.UTC),
        new ObjectMapper());
  }

  private static String validBearerToken(RtoAuthenticatedPrincipal principal) {
    return "Bearer " + jwtService().issueAccessToken(principal, NOW);
  }

  private static RtoAuthenticatedPrincipal principal() {
    return new RtoAuthenticatedPrincipal(
        USER_ACCOUNT_ID,
        ORGANIZATION_ID,
        PortalRole.CONSULTANT,
        "Consultant User",
        SESSION_ID);
  }

  private static JwtService jwtService() {
    return new JwtService(
        "0123456789abcdef0123456789abcdef",
        "test-issuer",
        1800,
        604800);
  }

  private static void assertUnauthorized(MockHttpServletResponse response) throws Exception {
    assertThat(response.getStatus()).isEqualTo(401);
    JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
    assertThat(body.path("error").path("errorCode").asText()).isEqualTo("authentication_failed");
    assertThat(body.path("error").path("safeReason").asText()).isEqualTo("invalid_token");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
