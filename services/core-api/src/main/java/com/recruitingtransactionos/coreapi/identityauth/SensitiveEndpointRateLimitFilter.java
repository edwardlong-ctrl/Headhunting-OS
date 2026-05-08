package com.recruitingtransactionos.coreapi.identityauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public final class SensitiveEndpointRateLimitFilter extends OncePerRequestFilter {

  private final RateLimitPolicy policy;
  private final Clock clock;
  private final ObjectMapper objectMapper;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public SensitiveEndpointRateLimitFilter(
      RateLimitPolicy policy,
      Clock clock,
      ObjectMapper objectMapper) {
    this.policy = Objects.requireNonNull(policy, "policy must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !policy.enabled() || category(request) == EndpointCategory.OTHER;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    EndpointCategory category = category(request);
    long now = clock.instant().getEpochSecond();
    Limit limit = limit(category);
    String key = category.name() + ":" + clientKey(request);
    Window window = windows.compute(key, (ignored, existing) -> {
      if (existing == null || now >= existing.resetAtEpochSecond()) {
        return new Window(1, now + limit.windowSeconds());
      }
      return new Window(existing.count() + 1, existing.resetAtEpochSecond());
    });
    pruneExpired(now);

    if (window.count() > limit.maxAttempts()) {
      long retryAfter = Math.max(1L, window.resetAtEpochSecond() - now);
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(retryAfter));
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getWriter(), ApiResponseEnvelope.failure(
          new ApiErrorResponse(
              "rate_limited",
              "too_many_requests",
              "Too many requests. Try again later.")));
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Limit limit(EndpointCategory category) {
    return switch (category) {
      case AUTH -> new Limit(policy.authMaxAttempts(), policy.authWindowSeconds());
      case DOCUMENT -> new Limit(policy.documentMaxAttempts(), policy.documentWindowSeconds());
      case OTHER -> throw new IllegalArgumentException("No limit for non-sensitive endpoint.");
    };
  }

  private static EndpointCategory category(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null) {
      return EndpointCategory.OTHER;
    }
    String method = request.getMethod() == null ? "" : request.getMethod();
    if (uri.equals("/api/auth/login") || uri.equals("/api/auth/refresh")) {
      return EndpointCategory.AUTH;
    }
    if (uri.startsWith("/api/consultant/documents/")
        || (uri.equals("/api/consultant/documents/upload") && method.equalsIgnoreCase("POST"))) {
      return EndpointCategory.DOCUMENT;
    }
    return EndpointCategory.OTHER;
  }

  private static String clientKey(HttpServletRequest request) {
    String remoteAddress = request.getRemoteAddr();
    if (remoteAddress == null || remoteAddress.isBlank()) {
      return "unknown";
    }
    return remoteAddress.strip();
  }

  private void pruneExpired(long now) {
    if (windows.size() < 2048) {
      return;
    }
    windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtEpochSecond());
  }

  public record RateLimitPolicy(
      boolean enabled,
      int authMaxAttempts,
      long authWindowSeconds,
      int documentMaxAttempts,
      long documentWindowSeconds) {

    public RateLimitPolicy {
      if (authMaxAttempts <= 0 || documentMaxAttempts <= 0) {
        throw new IllegalArgumentException("Rate limit attempts must be positive.");
      }
      if (authWindowSeconds <= 0 || documentWindowSeconds <= 0) {
        throw new IllegalArgumentException("Rate limit windows must be positive.");
      }
    }
  }

  private record Limit(int maxAttempts, long windowSeconds) {}

  private record Window(int count, long resetAtEpochSecond) {}

  private enum EndpointCategory {
    AUTH,
    DOCUMENT,
    OTHER
  }
}
