package com.recruitingtransactionos.coreapi.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

  @Test
  void generatesRequestIdForApiRequestWhenHeaderIsMissing() throws Exception {
    RequestCorrelationFilter filter = new RequestCorrelationFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/observability/workflow-events");
    MockHttpServletResponse response = new MockHttpServletResponse();
    CapturingChain chain = new CapturingChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
        .matches("[a-zA-Z0-9._:-]{16,64}");
    assertThat(chain.requestIdDuringChain).isEqualTo(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
    assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY)).isNull();
  }

  @Test
  void preservesSafeRequestIdHeader() throws Exception {
    RequestCorrelationFilter filter = new RequestCorrelationFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/observability/workflow-events");
    request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "task40-request-0001");
    MockHttpServletResponse response = new MockHttpServletResponse();
    CapturingChain chain = new CapturingChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isEqualTo("task40-request-0001");
    assertThat(chain.requestIdDuringChain).isEqualTo("task40-request-0001");
  }

  @Test
  void replacesUnsafeRequestIdHeader() throws Exception {
    RequestCorrelationFilter filter = new RequestCorrelationFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/observability/workflow-events");
    request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe token\nAuthorization: Bearer secret");
    MockHttpServletResponse response = new MockHttpServletResponse();
    CapturingChain chain = new CapturingChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
        .isNotEqualTo("unsafe token\nAuthorization: Bearer secret")
        .matches("[a-zA-Z0-9._:-]{16,64}");
    assertThat(chain.requestIdDuringChain).isEqualTo(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
  }

  @Test
  void doesNotAddRequestIdToNonApiRequest() throws Exception {
    RequestCorrelationFilter filter = new RequestCorrelationFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    CapturingChain chain = new CapturingChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isNull();
    assertThat(chain.requestIdDuringChain).isNull();
  }

  @Test
  void requestLogIncludesSafeCorrelationActorAndRouteFieldsWithoutQuerySecrets() throws Exception {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000400301");
    SecurityContextHolder.getContext().setAuthentication(new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        UUID.fromString("00000000-0000-0000-0000-000000400302"),
        organizationId,
        PortalRole.ADMIN,
        "Task 40 Admin",
        UUID.fromString("00000000-0000-0000-0000-000000400303"))));
    try {
      RequestCorrelationFilter filter = new RequestCorrelationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest(
          "GET",
          "/api/admin/observability/workflow-events");
      request.setQueryString("access_token=Bearer sk-secret");
      request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "task40-request-0002");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilter(request, response, new CapturingChain());

      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.getFirst().getFormattedMessage())
          .contains("requestId=task40-request-0002")
          .contains("organizationId=" + organizationId)
          .contains("actorRole=admin")
          .contains("route=/api/admin/observability/workflow-events")
          .contains("status=200")
          .contains("errorCode=none")
          .doesNotContain("Bearer")
          .doesNotContain("sk-secret")
          .doesNotContain("access_token");
    } finally {
      logger.detachAppender(appender);
      SecurityContextHolder.clearContext();
      MDC.clear();
    }
  }

  @Test
  void requestLogMasksIdentifiersAndPiiInUrlPath() throws Exception {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    UUID sourceItemId = UUID.fromString("00000000-0000-0000-0000-000000410301");
    try {
      RequestCorrelationFilter filter = new RequestCorrelationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest(
          "GET",
          "/api/consultant/documents/" + sourceItemId + "/download/jane.candidate@example.com");
      request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "task41-request-0001");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilter(request, response, new CapturingChain());

      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.getFirst().getFormattedMessage())
          .contains("route=/api/consultant/documents/{uuid}/download/{email}")
          .doesNotContain(sourceItemId.toString())
          .doesNotContain("jane.candidate@example.com");
    } finally {
      logger.detachAppender(appender);
      SecurityContextHolder.clearContext();
      MDC.clear();
    }
  }

  private static final class CapturingChain implements FilterChain {
    private String requestIdDuringChain;

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
        throws IOException, ServletException {
      requestIdDuringChain = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY);
    }
  }
}
