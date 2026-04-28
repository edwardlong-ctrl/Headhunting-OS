package com.recruitingtransactionos.coreapi.candidateprofile.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CandidateProfileAccessService {

  private static final Set<FieldClassification> RAW_FIELD_CONTEXTS = Set.of(
      FieldClassification.INTERNAL,
      FieldClassification.PII,
      FieldClassification.RAW_SOURCE,
      FieldClassification.CONSULTANT_PRIVATE,
      FieldClassification.AUDIT,
      FieldClassification.SYSTEM_GOVERNANCE);

  private static final Set<AccessAction> SENSITIVE_CANDIDATE_ACTIONS = Set.of(
      AccessAction.UPDATE,
      AccessAction.APPROVE,
      AccessAction.DISCLOSE,
      AccessAction.UNLOCK,
      AccessAction.EXPORT);

  private final CandidateProfileService candidateProfileService;
  private final PermissionEnforcer permissionEnforcer;

  public CandidateProfileAccessService(
      CandidateProfileService candidateProfileService,
      PermissionEnforcer permissionEnforcer) {
    this.candidateProfileService =
        Objects.requireNonNull(candidateProfileService, "candidateProfileService must not be null");
    this.permissionEnforcer =
        Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public AccessDecision requireRawCandidateReadAllowed(AccessRequest accessRequest) {
    requireResourceAction(accessRequest, ResourceType.CANDIDATE, AccessAction.READ);
    requireRawFieldContext(accessRequest, "raw_candidate_read_requires_raw_field_context");
    return permissionEnforcer.requireAllowed(accessRequest);
  }

  public Optional<CandidateProfile> findRawCandidateProfileByIdAndOrganizationId(
      AccessRequest accessRequest,
      UUID organizationId,
      CandidateProfileId candidateProfileId) {
    requireRawCandidateProfileReadAllowed(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    return candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        organizationId,
        candidateProfileId);
  }

  public Optional<CandidateProfile> findRawCandidateProfileByCandidateIdAndOrganizationId(
      AccessRequest accessRequest,
      UUID organizationId,
      CandidateId candidateId) {
    requireRawCandidateProfileReadAllowed(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        organizationId,
        candidateId);
  }

  public List<CandidateProfileField> listRawCandidateProfileFields(
      AccessRequest accessRequest,
      UUID organizationId,
      CandidateProfileId candidateProfileId) {
    requireRawCandidateProfileReadAllowed(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    return candidateProfileService.listCandidateProfileFields(organizationId, candidateProfileId);
  }

  public CandidateProfileField updateRawCandidateProfileField(
      AccessRequest accessRequest,
      UpsertCandidateProfileFieldRequest request) {
    requireResourceAction(accessRequest, ResourceType.CANDIDATE_PROFILE, AccessAction.UPDATE);
    permissionEnforcer.requireAllowed(accessRequest);
    return candidateProfileService.upsertCandidateProfileField(request);
  }

  public AccessDecision requireSensitiveCandidateActionAllowed(AccessRequest accessRequest) {
    requireSensitiveActionContext(accessRequest);
    return permissionEnforcer.requireAllowed(accessRequest);
  }

  private AccessDecision requireRawCandidateProfileReadAllowed(AccessRequest accessRequest) {
    requireResourceAction(accessRequest, ResourceType.CANDIDATE_PROFILE, AccessAction.READ);
    requireRawFieldContext(
        accessRequest,
        "raw_candidate_profile_requires_raw_field_context");
    return permissionEnforcer.requireAllowed(accessRequest);
  }

  private void requireSensitiveActionContext(AccessRequest accessRequest) {
    requireAccessRequest(accessRequest);
    if (!isCandidateResource(accessRequest.resourceType())
        || !SENSITIVE_CANDIDATE_ACTIONS.contains(accessRequest.action())) {
      throw denied(
          "sensitive_candidate_action_context_required",
          "Sensitive candidate action guard requires a candidate resource and sensitive action.");
    }
  }

  private void requireResourceAction(
      AccessRequest accessRequest,
      ResourceType resourceType,
      AccessAction action) {
    requireAccessRequest(accessRequest);
    if (accessRequest.resourceType() != resourceType || accessRequest.action() != action) {
      throw denied(
          "access_context_resource_action_mismatch",
          "Access request resource and action do not match this service boundary.");
    }
  }

  private void requireRawFieldContext(AccessRequest accessRequest, String reasonCode) {
    requireAccessRequest(accessRequest);
    if (!RAW_FIELD_CONTEXTS.contains(accessRequest.fieldClassification())) {
      throw denied(
          reasonCode,
          "Raw candidate/profile service boundaries cannot use safe projection field context.");
    }
  }

  private void requireAccessRequest(AccessRequest accessRequest) {
    if (accessRequest == null) {
      permissionEnforcer.requireAllowed(null);
    }
  }

  private static boolean isCandidateResource(ResourceType resourceType) {
    return resourceType == ResourceType.CANDIDATE
        || resourceType == ResourceType.CANDIDATE_PROFILE
        || resourceType == ResourceType.CONSENT_RECORD
        || resourceType == ResourceType.DISCLOSURE_RECORD;
  }

  private static AccessDeniedException denied(String reasonCode, String safeExplanation) {
    return new AccessDeniedException(new AccessDecision(false, reasonCode, safeExplanation));
  }
}
