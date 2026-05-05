package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantInterviewFeedbackReviewResponse;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackReviewService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackSuggestionNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test"
})
@WebMvcTest(ConsultantInterviewFeedbackReviewController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ConsultantInterviewFeedbackReviewControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-00000035e001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-00000035e002");
  private static final UUID SUGGESTION_ID =
      UUID.fromString("00000000-0000-0000-0000-00000035e003");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InterviewFeedbackReviewService reviewService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void getSuggestion_returnsBadRequestForInvalidUuid() throws Exception {
    mockMvc.perform(get("/api/consultant/interview-feedback-suggestions/not-a-uuid")
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.errorCode").value("validation_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("invalid_request"));
  }

  @Test
  void reviewSuggestion_returnsBadRequestForUnsupportedDecision() throws Exception {
    when(reviewService.review(
        eq(ORG_ID),
        eq(USER_ID),
        eq(new InterviewFeedbackSuggestionId(SUGGESTION_ID)),
        eq("escalate"),
        eq("Need another pass")))
        .thenThrow(new IllegalArgumentException("unsupported_review_decision"));

    mockMvc.perform(post("/api/consultant/interview-feedback-suggestions/{suggestionId}/review", SUGGESTION_ID)
            .with(authentication(auth(PortalRole.CONSULTANT)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"decision":"escalate","note":"Need another pass"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.errorCode").value("validation_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("invalid_request"));
  }

  @Test
  void reviewSuggestion_returnsNotFoundWhenSuggestionIsUnavailable() throws Exception {
    when(reviewService.review(
        eq(ORG_ID),
        eq(USER_ID),
        eq(new InterviewFeedbackSuggestionId(SUGGESTION_ID)),
        eq("approve"),
        eq("Looks good")))
        .thenThrow(new InterviewFeedbackSuggestionNotFoundException());

    mockMvc.perform(post("/api/consultant/interview-feedback-suggestions/{suggestionId}/review", SUGGESTION_ID)
            .with(authentication(auth(PortalRole.CONSULTANT)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"decision":"approve","note":"Looks good"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("not_found"))
        .andExpect(jsonPath("$.error.safeReason").value("interview_feedback_suggestion_unavailable"));
  }

  @Test
  void reviewSuggestion_returnsSuccessForConsultant() throws Exception {
    when(reviewService.review(
        eq(ORG_ID),
        eq(USER_ID),
        eq(new InterviewFeedbackSuggestionId(SUGGESTION_ID)),
        eq("approve"),
        eq("Looks good")))
        .thenReturn(new ConsultantInterviewFeedbackReviewResponse(
            SUGGESTION_ID.toString(),
            "approved",
            "2026-05-05T02:00:00Z",
            "00000000-0000-0000-0000-00000035e004",
            "hire",
            null,
            true));

    mockMvc.perform(post("/api/consultant/interview-feedback-suggestions/{suggestionId}/review", SUGGESTION_ID)
            .with(authentication(auth(PortalRole.CONSULTANT)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"decision":"approve","note":"Looks good"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("approved"))
        .andExpect(jsonPath("$.data.interactionUpdated").value(true));
  }

  private static Authentication auth(PortalRole portalRole) {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        USER_ID,
        ORG_ID,
        portalRole,
        "Task35 Review Tester",
        UUID.fromString("00000000-0000-0000-0000-00000035e00f")));
  }
}
