package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientDisclosedCandidateResponse;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/disclosed-candidates")
public final class ClientDisclosedCandidateController {

  private final ShortlistService shortlistService;
  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final ClientUnlockRequestPort clientUnlockRequestPort;
  private final DisclosureRecordPort disclosureRecordPort;

  public ClientDisclosedCandidateController(
      ShortlistService shortlistService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      ClientUnlockRequestPort clientUnlockRequestPort,
      DisclosureRecordPort disclosureRecordPort) {
    this.shortlistService = shortlistService;
    this.candidateService = candidateService;
    this.candidateProfileService = candidateProfileService;
    this.clientUnlockRequestPort = clientUnlockRequestPort;
    this.disclosureRecordPort = disclosureRecordPort;
  }

  @GetMapping("/{shortlistId}/{cardId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> disclosedCandidate(
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    ShortlistId parsedShortlistId = new ShortlistId(UUID.fromString(shortlistId));
    ShortlistCandidateCardId parsedCardId = new ShortlistCandidateCardId(UUID.fromString(cardId));
    ClientUnlockRequest latestRequest = clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
            principal.organizationId(),
            parsedShortlistId,
            parsedCardId)
        .orElseThrow(() -> new IllegalArgumentException("unlock_request_not_found"));
    if (latestRequest.clientActorId().compareTo(principal.userAccountId()) != 0
        || latestRequest.status() != ClientUnlockRequestStatus.APPROVED
        || latestRequest.approvedDisclosureRecordRef() == null) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "identity_disclosure_required",
          "Identity-disclosed candidate data is not available."));
    }
    ShortlistCandidateCard card = shortlistService.findCardByIdAndOrganizationId(principal.organizationId(), parsedCardId)
        .filter(existingCard -> existingCard.shortlistId().equals(parsedShortlistId))
        .orElseThrow(() -> new IllegalArgumentException("shortlist_card_not_found"));
    DisclosureRecord disclosedRecord = disclosureRecordPort.findByRefAndOrganizationId(
            principal.organizationId(),
            latestRequest.approvedDisclosureRecordRef())
        .filter(record -> record.status() == DisclosureStatus.IDENTITY_DISCLOSED)
        .filter(record -> record.disclosureLevel() == DisclosureLevel.L4_IDENTITY_DISCLOSED)
        .filter(record -> principal.userAccountId().toString().equals(record.clientRef()))
        .filter(record -> card.candidateId().value().toString().equals(record.candidateRef()))
        .filter(record -> card.candidateProfileId().toString().equals(record.candidateProfileRef()))
        .filter(record -> latestRequest.jobId().toString().equals(record.jobRef()))
        .orElseThrow(() -> new AccessDeniedException(new AccessDecision(
            false,
            "identity_disclosure_required",
            "Identity-disclosed candidate data is not available.")));
    Candidate candidate = candidateService.findCandidateByIdAndOrganizationId(principal.organizationId(), card.candidateId())
        .orElseThrow(() -> new IllegalArgumentException("candidate_not_found"));
    CandidateProfile profile = candidateProfileService.findCandidateProfileByIdAndOrganizationId(
            principal.organizationId(),
            new CandidateProfileId(card.candidateProfileId()))
        .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found"));
    ClientDisclosedCandidateResponse response = new ClientDisclosedCandidateResponse(
        shortlistId,
        cardId,
        disclosedRecord.disclosureRecordRef(),
        candidate.candidateId().value().toString(),
        profile.candidateProfileId().value().toString(),
        candidate.status().wireValue(),
        Integer.toString(profile.profileVersion().value()),
        profile.fields().stream()
            .filter(field -> Set.of(
                "identity.full_name",
                "contact.email",
                "contact.phone",
                "profile.headline",
                "profile.summary",
                "skills.primary_skills",
                "location.current_city").contains(field.fieldPath().value()))
            .map(field -> new ClientDisclosedCandidateResponse.DisclosedField(
                field.fieldPath().value(),
                field.value().jsonValue()))
            .toList());
    return ResponseEntity.ok(ApiResponseEnvelope.success(response));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(
            ApiValidationErrorResponse.of("invalid_request", List.of(exception.getMessage())).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(
            new ApiErrorResponse("internal_error", "request_failed", "Request failed.")));
  }

  private static void requireClientRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CLIENT) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "client_role_required",
          "Client role is required for this endpoint."));
    }
  }
}
