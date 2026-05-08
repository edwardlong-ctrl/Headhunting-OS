package com.recruitingtransactionos.coreapi.apiboundary.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.AuthenticationFailureException;
import com.recruitingtransactionos.coreapi.identityauth.AuthenticationService;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthenticationController.class)
@TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test-issuer"
})
class AuthenticationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AuthenticationService authenticationService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void loginSuccessReturnsOnlyAllowlistedAuthSessionFields() throws Exception {
    AuthenticationService.AuthenticatedSession session = new AuthenticationService.AuthenticatedSession(
        UUID.fromString("00000000-0000-0000-0000-000000190001"),
        UUID.fromString("00000000-0000-0000-0000-000000190002"),
        "Consultant User",
        PortalRole.CONSULTANT,
        "access.token.value",
        "refresh-token-value",
        Instant.parse("2026-05-01T10:00:00Z"),
        Instant.parse("2026-05-08T10:00:00Z"));
    when(authenticationService.login(any(), eq("consultant@example.com"), eq("secret123"),
        eq(PortalRole.CONSULTANT))).thenReturn(session);

    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "organizationId", "00000000-0000-0000-0000-000000190001",
                "email", "consultant@example.com",
                "password", "secret123",
                "portalRole", "consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.organizationId").value("00000000-0000-0000-0000-000000190001"))
        .andExpect(jsonPath("$.data.userAccountId").value("00000000-0000-0000-0000-000000190002"))
        .andExpect(jsonPath("$.data.portalRole").value("consultant"))
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
        .andReturn();

    Set<String> fieldNames = Set.copyOf(objectMapper.readTree(result.getResponse().getContentAsString())
        .get("data")
        .properties()
        .stream()
        .map(Map.Entry::getKey)
        .toList());
    assertThat(fieldNames).isEqualTo(ApiBoundaryContractRules.authSessionResponseFieldNames());
    assertSanitized(result.getResponse().getContentAsString());
  }

  @Test
  void invalidCredentialsAreSanitizedAndReturn401() throws Exception {
    when(authenticationService.login(any(), any(), any(), any()))
        .thenThrow(AuthenticationFailureException.invalidCredentials());

    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "organizationId", "00000000-0000-0000-0000-000000190011",
                "email", "bad@example.com",
                "password", "wrong-pass",
                "portalRole", "consultant"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("invalid_credentials"))
        .andReturn();

    assertSanitized(result.getResponse().getContentAsString());
  }

  @Test
  void invalidBearerTokenFailsClosedBeforeController() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/refresh")
            .header("Authorization", "Bearer not-a-valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "refresh-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("invalid_token"))
        .andReturn();

    assertSanitized(result.getResponse().getContentAsString());
  }

  @Test
  void invalidPortalRoleReturns400WithoutInternalLeakage() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "organizationId", "00000000-0000-0000-0000-000000190021",
                "email", "owner@example.com",
                "password", "secret123",
                "portalRole", "ai_assistant"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.errorCode").value("validation_failed"))
        .andReturn();

    assertSanitized(result.getResponse().getContentAsString());
  }

  @Test
  void weakLoginPayloadIsRejectedBeforeAuthenticationService() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "organizationId", "00000000-0000-0000-0000-000000190031",
                "email", "not-an-email",
                "password", "short",
                "portalRole", "consultant"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.errorCode").value("validation_failed"))
        .andReturn();

    verifyNoInteractions(authenticationService);
    assertSanitized(result.getResponse().getContentAsString());
  }

  private static void assertSanitized(String body) {
    assertThat(body)
        .doesNotContain("Exception", "stacktrace", "com.recruitingtransactionos", "java.");
  }
}
