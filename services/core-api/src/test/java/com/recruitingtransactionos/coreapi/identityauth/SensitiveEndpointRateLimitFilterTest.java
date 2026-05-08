package com.recruitingtransactionos.coreapi.identityauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SensitiveEndpointRateLimitFilterTest {

  @Test
  void throttlesRepeatedLoginAttemptsWithoutInvokingDownstreamChain() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-05-08T00:00:00Z"));
    SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(
        new SensitiveEndpointRateLimitFilter.RateLimitPolicy(true, 2, 60, 2, 60),
        clock,
        new ObjectMapper());
    CountingChain chain = new CountingChain();

    assertThat(doFilter(filter, loginRequest("203.0.113.10"), chain).getStatus()).isEqualTo(200);
    assertThat(doFilter(filter, loginRequest("203.0.113.10"), chain).getStatus()).isEqualTo(200);
    MockHttpServletResponse throttled = doFilter(filter, loginRequest("203.0.113.10"), chain);

    assertThat(throttled.getStatus()).isEqualTo(429);
    assertThat(throttled.getHeader("Retry-After")).isEqualTo("60");
    assertThat(throttled.getContentAsString())
        .contains("rate_limited")
        .doesNotContain("203.0.113.10");
    assertThat(chain.count).isEqualTo(2);

    clock.advanceSeconds(61);
    assertThat(doFilter(filter, loginRequest("203.0.113.10"), chain).getStatus()).isEqualTo(200);
    assertThat(chain.count).isEqualTo(3);
  }

  @Test
  void throttlesRefreshAttemptsWithAuthPolicy() throws Exception {
    SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(
        new SensitiveEndpointRateLimitFilter.RateLimitPolicy(true, 1, 60, 5, 60),
        Clock.fixed(Instant.parse("2026-05-08T00:00:00Z"), ZoneOffset.UTC),
        new ObjectMapper());
    CountingChain chain = new CountingChain();

    assertThat(doFilter(filter, request("POST", "/api/auth/refresh", "203.0.113.11"), chain)
        .getStatus()).isEqualTo(200);
    MockHttpServletResponse throttled =
        doFilter(filter, request("POST", "/api/auth/refresh", "203.0.113.11"), chain);

    assertThat(throttled.getStatus()).isEqualTo(429);
    assertThat(chain.count).isEqualTo(1);
  }

  @Test
  void throttlesEachConsultantDocumentEndpointWithDocumentPolicy() throws Exception {
    for (Endpoint endpoint : new Endpoint[] {
        new Endpoint("POST", "/api/consultant/documents/upload"),
        new Endpoint("GET", "/api/consultant/documents/00000000-0000-0000-0000-000000200033/download"),
        new Endpoint("POST", "/api/consultant/documents/00000000-0000-0000-0000-000000200033/parse"),
        new Endpoint("GET", "/api/consultant/documents/00000000-0000-0000-0000-000000200033/parsed"),
        new Endpoint("GET", "/api/consultant/documents/00000000-0000-0000-0000-000000200033/evidence")
    }) {
      SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(
          new SensitiveEndpointRateLimitFilter.RateLimitPolicy(true, 5, 60, 1, 60),
          Clock.fixed(Instant.parse("2026-05-08T00:00:00Z"), ZoneOffset.UTC),
          new ObjectMapper());
      CountingChain chain = new CountingChain();

      assertThat(doFilter(filter, request(endpoint.method(), endpoint.path(), "203.0.113.12"), chain)
          .getStatus()).as(endpoint.path()).isEqualTo(200);
      assertThat(doFilter(filter, request(endpoint.method(), endpoint.path(), "203.0.113.12"), chain)
          .getStatus()).as(endpoint.path()).isEqualTo(429);
      assertThat(chain.count).as(endpoint.path()).isEqualTo(1);
    }
  }

  @Test
  void doesNotThrottleNonSensitiveApiRoutes() throws Exception {
    SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(
        new SensitiveEndpointRateLimitFilter.RateLimitPolicy(true, 1, 60, 1, 60),
        Clock.fixed(Instant.parse("2026-05-08T00:00:00Z"), ZoneOffset.UTC),
        new ObjectMapper());
    CountingChain chain = new CountingChain();

    assertThat(doFilter(filter, request("GET", "/api/consultant/dashboard", "203.0.113.20"), chain)
        .getStatus()).isEqualTo(200);
    assertThat(doFilter(filter, request("GET", "/api/consultant/dashboard", "203.0.113.20"), chain)
        .getStatus()).isEqualTo(200);

    assertThat(chain.count).isEqualTo(2);
  }

  private static MockHttpServletResponse doFilter(
      SensitiveEndpointRateLimitFilter filter,
      MockHttpServletRequest request,
      FilterChain chain) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    return response;
  }

  private static MockHttpServletRequest loginRequest(String remoteAddress) {
    return request("POST", "/api/auth/login", remoteAddress);
  }

  private static MockHttpServletRequest request(String method, String path, String remoteAddress) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(remoteAddress);
    return request;
  }

  private static final class CountingChain implements FilterChain {
    private int count;

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
        throws IOException, ServletException {
      count++;
      ((MockHttpServletResponse) response).setStatus(200);
    }
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    private void advanceSeconds(long seconds) {
      now = now.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private record Endpoint(String method, String path) {}
}
