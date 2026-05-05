package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/commissions")
public final class ConsultantCommissionController {

  private final ConsultantCommissionQueryService queryService;
  private final ConsultantCommissionCommandService commandService;

  public ConsultantCommissionController(
      ConsultantCommissionQueryService queryService,
      ConsultantCommissionCommandService commandService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    this.commandService = Objects.requireNonNull(commandService, "commandService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listCommissions(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(queryService.listCommissions(
        readAccessRequest(), PagedQuery.builder(principal.organizationId()).limit(limit).offset(offset).build())));
  }

  @PostMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createCommission(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ConsultantCommissionCreateRequest request) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseEnvelope.success(
        commandService.createCommission(writeAccessRequest(), principal.organizationId(), principal.userAccountId(), request)));
  }

  @PostMapping("/{commissionId}/mark-paid")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> markPaid(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @PathVariable String commissionId,
      @RequestBody ConsultantVersionedCommandRequest request) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        commandService.markPaid(writeAccessRequest(), principal.organizationId(), principal.userAccountId(), commissionId, request.version())));
  }

  @PostMapping("/{commissionId}/withhold")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> withhold(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @PathVariable String commissionId,
      @RequestBody ConsultantCommissionWithholdRequest request) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        commandService.withhold(writeAccessRequest(), principal.organizationId(), principal.userAccountId(), commissionId, request)));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse("commission_request_failed", "request_failed", exception.getMessage())));
  }

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(new AccessDecision(false, "consultant_role_required", "Consultant role is required for this endpoint."));
    }
  }

  private static AccessRequest readAccessRequest() {
    return new AccessRequest(PortalRole.CONSULTANT, ResourceType.COMMISSION, AccessAction.READ, FieldClassification.INTERNAL, Set.of(RelationshipScope.SAME_ORGANIZATION), false);
  }

  private static AccessRequest writeAccessRequest() {
    return new AccessRequest(PortalRole.CONSULTANT, ResourceType.COMMISSION, AccessAction.UPDATE, FieldClassification.INTERNAL, Set.of(RelationshipScope.SAME_ORGANIZATION), false);
  }
}
