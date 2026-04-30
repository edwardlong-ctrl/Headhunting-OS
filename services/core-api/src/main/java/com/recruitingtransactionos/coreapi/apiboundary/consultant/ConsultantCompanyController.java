package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanyDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanySummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/companies")
public final class ConsultantCompanyController {


  private final ConsultantApiQueryService queryService;
  private final ConsultantApiCommandService commandService;

  public ConsultantCompanyController(
      ConsultantApiQueryService queryService,
      ConsultantApiCommandService commandService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    this.commandService = Objects.requireNonNull(commandService, "commandService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listCompanies(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    AccessRequest accessRequest = buildAccessRequest(ResourceType.COMPANY, AccessAction.READ);
    PagedQuery pagedQuery = PagedQuery.builder(orgId).limit(limit).offset(offset).build();

    PagedResult<ConsultantCompanySummaryResponse> result =
        queryService.listCompanies(accessRequest, pagedQuery, status);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @GetMapping("/{companyId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getCompany(
      @PathVariable String companyId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    CompanyId cid = parseCompanyId(companyId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.COMPANY, AccessAction.READ);

    return queryService.getCompanyDetail(accessRequest, orgId, cid)
        .map(response -> ResponseEntity.ok(
            ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ConsultantCompanyController::notFound);
  }

  @PostMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createCompany(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody CompanyCreateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    AccessRequest accessRequest = buildAccessRequest(ResourceType.COMPANY, AccessAction.CREATE);

    ConsultantCompanyDetailResponse result =
        commandService.createCompany(accessRequest, orgId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponseEnvelope.success(result));
  }

  @PutMapping("/{companyId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> updateCompany(
      @PathVariable String companyId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody CompanyUpdateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    CompanyId cid = parseCompanyId(companyId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.COMPANY, AccessAction.UPDATE);

    ConsultantCompanyDetailResponse result =
        commandService.updateCompany(accessRequest, orgId, cid, request);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @PostMapping("/{companyId}/contacts")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createCompanyContact(
      @PathVariable String companyId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody CompanyContactCreateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    CompanyId cid = parseCompanyId(companyId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.COMPANY, AccessAction.CREATE);

    ConsultantCompanyDetailResponse result =
        commandService.createCompanyContact(accessRequest, orgId, cid, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponseEnvelope.success(result));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      Exception exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request: " + exception.getMessage()))
            .toErrorResponse());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> unreadablePayload(
      HttpMessageNotReadableException exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request body."))
            .toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(
      RuntimeException exception) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        new ApiErrorResponse(
            "internal_error",
            "request_failed",
            "Request failed."));
  }

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(
          new AccessDecision(false,
              "consultant_role_required",
              "Consultant role is required for this endpoint."));
    }
  }


  private static CompanyId parseCompanyId(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      throw new IllegalArgumentException("companyId must not be blank");
    }
    try {
      return new CompanyId(UUID.fromString(companyId));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid company ID format.");
    }
  }

  private static AccessRequest buildAccessRequest(ResourceType resourceType, AccessAction action) {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        resourceType,
        action,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(
        HttpStatus.NOT_FOUND,
        new ApiErrorResponse(
            "not_found",
            "company_unavailable",
            "Company is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
