package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestCorrelationFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_REQUEST_ID_KEY = "requestId";
  public static final String MDC_ORGANIZATION_ID_KEY = "organizationId";
  public static final String MDC_ACTOR_ROLE_KEY = "actorRole";
  public static final String MDC_ERROR_CODE_KEY = "errorCode";

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestCorrelationFilter.class);
  private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{8,64}");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI() == null || !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestId = safeRequestId(request.getHeader(REQUEST_ID_HEADER));
    long started = System.nanoTime();
    MDC.put(MDC_REQUEST_ID_KEY, requestId);
    MDC.put(MDC_ERROR_CODE_KEY, "none");
    response.setHeader(REQUEST_ID_HEADER, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      RtoAuthenticatedPrincipal principal = currentPrincipal();
      String organizationId = principal == null ? "unknown" : principal.organizationId().toString();
      String actorRole = principal == null ? "anonymous" : principal.portalRole().wireValue();
      MDC.put(MDC_ORGANIZATION_ID_KEY, organizationId);
      MDC.put(MDC_ACTOR_ROLE_KEY, actorRole);
      long durationMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
      LOGGER.info(
          "event=api_request requestId={} organizationId={} actorRole={} method={} route={} status={} durationMs={} errorCode={}",
          requestId,
          organizationId,
          actorRole,
          request.getMethod(),
          safeRoute(request.getRequestURI()),
          response.getStatus(),
          durationMs,
          MDC.get(MDC_ERROR_CODE_KEY));
      MDC.remove(MDC_REQUEST_ID_KEY);
      MDC.remove(MDC_ORGANIZATION_ID_KEY);
      MDC.remove(MDC_ACTOR_ROLE_KEY);
      MDC.remove(MDC_ERROR_CODE_KEY);
    }
  }

  static String safeRequestId(String candidate) {
    if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
      return candidate;
    }
    return "req-" + UUID.randomUUID();
  }

  private static String safeRoute(String route) {
    if (route == null || route.isBlank()) {
      return "unknown";
    }
    return route.replaceAll("[^A-Za-z0-9/_{}:.-]", "_");
  }

  private static RtoAuthenticatedPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof RtoAuthenticatedPrincipal principal)) {
      return null;
    }
    return principal;
  }
}
