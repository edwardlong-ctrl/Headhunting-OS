package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCleanFactResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakePublishResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeReviewResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeRunResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantSourceHighlightResponse;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedAiIntakeOrchestrator;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewDecisionService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewQueryService;
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
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/intake")
public final class ConsultantIntakeController {

  private final GovernedAiIntakeOrchestrator governedAiIntakeOrchestrator;
  private final IntakeReviewQueryService intakeReviewQueryService;
  private final IntakeReviewDecisionService intakeReviewDecisionService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantIntakeController(
      GovernedAiIntakeOrchestrator governedAiIntakeOrchestrator,
      IntakeReviewQueryService intakeReviewQueryService,
      IntakeReviewDecisionService intakeReviewDecisionService) {
    this.governedAiIntakeOrchestrator = Objects.requireNonNull(
        governedAiIntakeOrchestrator, "governedAiIntakeOrchestrator must not be null");
    this.intakeReviewQueryService = Objects.requireNonNull(
        intakeReviewQueryService, "intakeReviewQueryService must not be null");
    this.intakeReviewDecisionService = Objects.requireNonNull(
        intakeReviewDecisionService, "intakeReviewDecisionService must not be null");
    this.permissionEnforcer = new PermissionEnforcer(new PermissionEvaluator());
  }

  @PostMapping("/packets/{informationPacketId}/extract")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> extract(
      @PathVariable String informationPacketId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireIntakeAccess(principal.portalRole(), AccessAction.UPDATE);
    IntakeExtractionRun run = governedAiIntakeOrchestrator.extract(
        principal.organizationId(),
        new InformationPacketId(UUID.fromString(informationPacketId)),
        principal.userAccountId(),
        ActorRole.CONSULTANT);
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantIntakeRunResponse(
        run.extractionRunId().value().toString(),
        run.informationPacketId().value().toString(),
        run.outputEnvelope().map(envelope -> envelope.intendedEntityType().wireValue()).orElse("UNKNOWN"),
        run.status().wireValue(),
        run.outputEnvelope().map(envelope -> envelope.outputSchemaVersion()).orElse(null),
        run.outputEnvelope().map(envelope -> envelope.cleanFactCandidates().size()).orElse(0),
        run.outputEnvelope().map(envelope -> envelope.aiTaskRunIds().stream().map(UUID::toString).toList()).orElse(List.of()))));
  }

  @GetMapping("/packets/{informationPacketId}/review")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> review(
      @PathVariable String informationPacketId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireIntakeAccess(principal.portalRole(), AccessAction.READ);
    IntakeReviewQueryService.IntakeReviewView view = intakeReviewQueryService.reviewView(
        principal.organizationId(),
        new InformationPacketId(UUID.fromString(informationPacketId)));
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantIntakeReviewResponse(
        view.run().extractionRunId().value().toString(),
        view.run().informationPacketId().value().toString(),
        view.run().outputEnvelope().orElseThrow().intendedEntityType().wireValue(),
        view.facts().size(),
        view.facts().stream().map(fact -> new ConsultantCleanFactResponse(
            fact.claimId() == null ? null : fact.claimId().value().toString(),
            fact.candidate().claimFieldName(),
            fact.candidate().targetEntityType(),
            fact.candidate().targetFieldPath(),
            fact.candidate().proposedValue(),
            fact.candidate().suggestedVerificationStatus().wireValue(),
            fact.candidate().suggestedRiskTier().wireValue(),
            fact.candidate().entityResolutionStatus(),
            fact.latestReview() == null ? null : fact.latestReview().decision().wireValue(),
            fact.latestReview() == null ? null : fact.latestReview().reviewEventId().value().toString(),
            fact.candidate().conflictsWithCanonical(),
            fact.candidate().rationale(),
            new ConsultantSourceHighlightResponse(
                fact.candidate().sourceItemId().value().toString(),
                fact.candidate().parsedDocumentId().toString(),
                fact.candidate().parsedDocumentChunkId().toString(),
                fact.candidate().pageNumber(),
                fact.candidate().startOffset(),
                fact.candidate().endOffset(),
                fact.candidate().safeSnippet(),
                locator(fact.candidate().pageNumber(), fact.candidate().startOffset(), fact.candidate().endOffset())))).toList())));
  }

  @PostMapping("/claims/{claimLedgerItemId}/decisions")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> decide(
      @PathVariable String claimLedgerItemId,
      @RequestBody ConsultantIntakeDecisionRequest request,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireIntakeAccess(principal.portalRole(), AccessAction.UPDATE);
    String decisionStatus = intakeReviewDecisionService.decide(
        principal.organizationId(),
        new ClaimId(UUID.fromString(claimLedgerItemId)),
        principal.userAccountId(),
        ActorRole.CONSULTANT,
        parseReviewDecision(request.decision()),
        RiskTier.fromWireValue(request.riskTier()),
        request.bulkFlag() != null && request.bulkFlag(),
        request.reason()).status().name();
    ConsultantIntakePublishResponse response = new ConsultantIntakePublishResponse(
        claimLedgerItemId,
        0,
        List.of(decisionStatus),
        List.of());
    return ResponseEntity.ok(ApiResponseEnvelope.success(response));
  }

  @PostMapping("/packets/{informationPacketId}/publish")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> publish(
      @PathVariable String informationPacketId,
      @RequestBody ConsultantIntakePublishRequest request,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireIntakeAccess(principal.portalRole(), AccessAction.UPDATE);
    IntakeReviewDecisionService.PublishResult result = intakeReviewDecisionService.publish(
        principal.organizationId(),
        new InformationPacketId(UUID.fromString(informationPacketId)),
        principal.userAccountId(),
        new IntakeReviewDecisionService.PublishRequest(
            parseUuid(request.candidateId()),
            parseUuid(request.companyId()),
            parseUuid(request.jobId()),
            parseUuid(request.jobCompanyId()),
            request.reason()));
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantIntakePublishResponse(
        informationPacketId,
        result.canonicalWrites().size(),
        result.canonicalWrites().stream().map(write -> write.status().name()).toList(),
        result.directWrites())));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request: " + exception.getMessage())).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(
      RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "internal_error",
            "request_failed",
            "Request failed.")));
  }

  private void requireIntakeAccess(PortalRole portalRole, AccessAction action) {
    permissionEnforcer.requireAllowed(new AccessRequest(
        portalRole,
        ResourceType.INFORMATION_PACKET,
        action,
        FieldClassification.SYSTEM_GOVERNANCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));
  }

  private static UUID parseUuid(String value) {
    return value == null || value.isBlank() ? null : UUID.fromString(value);
  }

  private static ReviewDecision parseReviewDecision(String wireValue) {
    if (wireValue == null || wireValue.isBlank()) {
      throw new IllegalArgumentException("decision must not be blank");
    }
    for (ReviewDecision value : ReviewDecision.values()) {
      if (value.wireValue().equals(wireValue.strip())) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown review decision: " + wireValue);
  }

  private static String locator(Integer pageNumber, int startOffset, int endOffset) {
    if (pageNumber != null) {
      return "page " + pageNumber + " offsets " + startOffset + "-" + endOffset;
    }
    return "offsets " + startOffset + "-" + endOffset;
  }
}
