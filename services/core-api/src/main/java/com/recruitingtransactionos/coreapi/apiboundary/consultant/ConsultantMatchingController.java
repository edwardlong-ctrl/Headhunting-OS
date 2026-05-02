package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.matching.AuthenticityRiskLevel;
import com.recruitingtransactionos.coreapi.matching.MatchDimension;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationRequest;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationResult;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationService;
import com.recruitingtransactionos.coreapi.matching.MatchScore;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import com.recruitingtransactionos.coreapi.matching.MatchJobRef;
import com.recruitingtransactionos.coreapi.matching.MatchSubjectRef;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverageInput;
import com.recruitingtransactionos.coreapi.matching.EvidenceAssertionStrength;
import com.recruitingtransactionos.coreapi.matching.MatchReportId;
import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/jobs/{jobId}/matching")
public final class ConsultantMatchingController {

  private final MatchReportGenerationService matchReportGenerationService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantMatchingController() {
    this(
        new MatchReportGenerationService(),
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  private ConsultantMatchingController(
      MatchReportGenerationService matchReportGenerationService,
      PermissionEnforcer permissionEnforcer) {
    this.matchReportGenerationService = matchReportGenerationService;
    this.permissionEnforcer = permissionEnforcer;
  }

  @PostMapping("/generate")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> generateMatchReport(
      @PathVariable String jobId,
      @RequestBody ConsultantMatchGenerationRequest request,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    
    requireAccess(principal.portalRole(), AccessAction.CREATE);

    try {
      MatchReportGenerationRequest matchRequest = new MatchReportGenerationRequest(
          new MatchReportId("match_report_" + UUID.randomUUID().toString().replace("-", "")),
          MatchJobRef.of("job_ref_" + jobId.replace("-", "")),
          MatchSubjectRef.of(resolveCandidateSubjectRef(request)),
          MatchScore.of(request.requestedOverallScore()),
          parseDimensionScores(request.requestedDimensionScores()),
          new EvidenceCoverageInput(
              parseDimensionScores(request.requestedDimensionScores()).keySet(),
              List.of()),
          IndustryPackMaturity.valueOf(request.industryPackMaturity()),
          request.keywordOnlyEvidence(),
          request.projectEvidencePresent(),
          EvidenceAssertionStrength.valueOf(request.candidateIntentSignalStrength()),
          request.ontologyStale(),
          request.industryPackVersionStale(),
          AuthenticityRiskLevel.valueOf(request.authenticityRisk()),
          ReidentificationRiskSignal.valueOf(request.reidentificationRiskSignal()),
          request.ontologyVersion(),
          request.industryPackVersion(),
          Instant.now());

      MatchReportGenerationResult result = matchReportGenerationService.generate(matchRequest);
      
      ConsultantMatchReportResponse response = new ConsultantMatchReportResponse(
          result.matchReport().matchReportId().value(),
          result.matchReport().scoreCapDecision().cappedScore().value(),
          result.matchReport().scoreCapDecision().capApplied(),
          result.matchReport().scoreCapDecision().reasonCode().name(),
          result.matchReport().scoreConfidence().name()
      );

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponseEnvelope.success(response));
    } catch (IllegalArgumentException e) {
      return error(
          HttpStatus.BAD_REQUEST,
          ApiValidationErrorResponse.of("invalid_request", List.of(e.getMessage())).toErrorResponse());
    }
  }

  private Map<MatchDimension, MatchScore> parseDimensionScores(Map<String, Integer> scores) {
    if (scores == null) return Map.of();
    return scores.entrySet().stream()
        .collect(Collectors.toMap(
            e -> MatchDimension.valueOf(e.getKey()),
            e -> MatchScore.of(e.getValue())));
  }

  private void requireAccess(PortalRole portalRole, AccessAction action) {
    permissionEnforcer.requireAllowed(new AccessRequest(
        portalRole,
        ResourceType.JOB,
        action,
        FieldClassification.CONSULTANT_PRIVATE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));
  }

  private static String resolveCandidateSubjectRef(ConsultantMatchGenerationRequest request) {
    if (request.candidateCardRef() != null && !request.candidateCardRef().isBlank()) {
      return request.candidateCardRef().strip();
    }
    if (request.anonymousCandidateCardId() != null && !request.anonymousCandidateCardId().isBlank()) {
      return "match_subject_card_"
          + request.anonymousCandidateCardId().strip().replace("card_", "").replace("-", "").toLowerCase();
    }
    if (request.candidateId() != null && !request.candidateId().isBlank()) {
      return "match_subject_candidate_" + request.candidateId().strip().replace("-", "").toLowerCase();
    }
    throw new IllegalArgumentException("candidate selection is required");
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status, com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }
}

record ConsultantMatchGenerationRequest(
    String candidateId,
    String anonymousCandidateCardId,
    String candidateCardRef,
    int requestedOverallScore,
    Map<String, Integer> requestedDimensionScores,
    String industryPackMaturity,
    boolean keywordOnlyEvidence,
    boolean projectEvidencePresent,
    String candidateIntentSignalStrength,
    boolean ontologyStale,
    boolean industryPackVersionStale,
    String authenticityRisk,
    String reidentificationRiskSignal,
    String ontologyVersion,
    String industryPackVersion
) {}
