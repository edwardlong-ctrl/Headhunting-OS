package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCandidateDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCandidateSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantCandidateResponseMapper;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantCandidateQueryService {

  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantCandidateQueryService(
      CandidateService candidateService,
      CandidateProfileService candidateProfileService) {
    this(candidateService, candidateProfileService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantCandidateQueryService(
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      PermissionEnforcer permissionEnforcer) {
    this.candidateService = Objects.requireNonNull(candidateService, "candidateService must not be null");
    this.candidateProfileService = Objects.requireNonNull(
        candidateProfileService,
        "candidateProfileService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public PagedResult<ConsultantCandidateSummaryResponse> listCandidates(
      AccessRequest accessRequest, PagedQuery pagedQuery, String statusFilter) {
    requireCandidateRead(accessRequest);
    UUID organizationId = pagedQuery.organizationId();
    List<Candidate> all = statusFilter == null
        ? candidateService.findAllCandidatesByOrganizationId(organizationId)
        : candidateService.findCandidatesByOrganizationIdAndStatus(
            organizationId, CandidateStatus.fromWireValue(statusFilter));
    List<ConsultantCandidateSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(ConsultantCandidateResponseMapper::toSummary)
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  public Optional<ConsultantCandidateDetailResponse> getCandidateDetail(
      AccessRequest accessRequest, UUID organizationId, CandidateId candidateId) {
    requireCandidateRead(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return candidateService.findCandidateByIdAndOrganizationId(organizationId, candidateId)
        .map(candidate -> ConsultantCandidateResponseMapper.toDetail(
            candidate,
            candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
                organizationId,
                candidate.candidateId()).orElse(null)));
  }

  private void requireCandidateRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "candidate_read_context_required",
          "Consultant candidate API requires a candidate read context."));
    }
  }
}
