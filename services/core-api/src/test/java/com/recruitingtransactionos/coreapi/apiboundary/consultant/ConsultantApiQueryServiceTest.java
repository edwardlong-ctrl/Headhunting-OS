package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderService;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderState;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistDeliveryPreview;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistPreSendCheck;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultantApiQueryServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
  private static final UUID JOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000112");
  private static final UUID SHORTLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000113");
  private static final Instant NOW = Instant.parse("2026-05-03T08:00:00Z");

  @Mock private CompanyService companyService;
  @Mock private JobService jobService;
  @Mock private ShortlistService shortlistService;
  @Mock private ShortlistBuilderService shortlistBuilderService;
  @Mock private IndustryPackService industryPackService;

  @Test
  void getShortlistDetailUsesBuilderStateAndReturnsOpaqueMatchReportId() {
    Shortlist shortlist = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new com.recruitingtransactionos.coreapi.job.JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.READY_FOR_REVIEW)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(2)
        .build();
    ShortlistCandidateCard card = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            UUID.fromString("00000000-0000-0000-0000-000000000114")))
        .organizationId(ORG_ID)
        .shortlistId(shortlist.shortlistId())
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000115"))
        .candidateId(new com.recruitingtransactionos.coreapi.candidateprofile.CandidateId(
            UUID.fromString("00000000-0000-0000-0000-000000000116")))
        .candidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000117"))
        .sortOrder(0)
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .matchReportId(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
        .clientNotes("note")
        .metadata("{\"anonymousCandidateRef\":\"anon_candidate_1\",\"projectionVersion\":\"task29-shortlist-v1\","
            + "\"redactionLevel\":\"l2_client_safe\",\"generalizedHeadline\":\"Consultant-reviewed candidate\","
            + "\"generalizedRoleFamily\":\"Confidential role family\",\"generalizedSeniorityBand\":\"Consultant-reviewed shortlist level\","
            + "\"generalizedLocationRegion\":\"Location shared after identity unlock\",\"safeSummary\":\"safe summary\","
            + "\"safeSkillSummary\":\"safe skill summary\",\"safeEvidenceSummaries\":[\"evidence\"],"
            + "\"safeMatchNarratives\":[\"narrative\"],\"overallScore\":4,\"confidence\":\"high\","
            + "\"reidentificationRiskSignal\":\"low\",\"dimensionScores\":[]}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    ShortlistBuilderState state = new ShortlistBuilderState(
        shortlist,
        List.of(card),
        List.of(
            new ShortlistPreSendCheck("status_ready_for_review", "ready", true),
            new ShortlistPreSendCheck("has_included_cards", "cards", true),
            new ShortlistPreSendCheck("anonymous_cards_generated", "anonymous", true),
            new ShortlistPreSendCheck("delivery_preview_ready", "preview", true),
            new ShortlistPreSendCheck("reidentification_risk_within_gate", "risk", true),
            new ShortlistPreSendCheck("client_safe_consent_confirmed", "consent", true)),
        new ShortlistDeliveryPreview("summary", "pdf", "email", "wechat"));

    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistBuilderService.getBuilderState(ORG_ID, shortlist.shortlistId()))
        .thenReturn(state);

    ConsultantApiQueryService service = new ConsultantApiQueryService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantShortlistDetailResponse response = service.getShortlistDetail(
            shortlistReadAccessRequest(),
            ORG_ID,
            shortlist.shortlistId())
        .orElseThrow();

    assertThat(response.preSendChecks()).hasSize(6);
    assertThat(response.cards()).singleElement().satisfies(dto ->
        assertThat(dto.matchReportId()).isEqualTo("match_report_123456781234123412341234567890ab"));
  }

  @Test
  void listShortlistsCountsOnlyActiveCardsExcludingRemovedOnes() {
    Shortlist shortlist = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new com.recruitingtransactionos.coreapi.job.JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.READY_FOR_REVIEW)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(2)
        .build();
    ShortlistCandidateCard includedCard = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            UUID.fromString("00000000-0000-0000-0000-000000000114")))
        .organizationId(ORG_ID)
        .shortlistId(shortlist.shortlistId())
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000115"))
        .candidateId(new com.recruitingtransactionos.coreapi.candidateprofile.CandidateId(
            UUID.fromString("00000000-0000-0000-0000-000000000116")))
        .candidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000117"))
        .sortOrder(0)
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .matchReportId(null)
        .clientNotes(null)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    ShortlistCandidateCard removedCard = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            UUID.fromString("00000000-0000-0000-0000-000000000118")))
        .organizationId(ORG_ID)
        .shortlistId(shortlist.shortlistId())
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000119"))
        .candidateId(new com.recruitingtransactionos.coreapi.candidateprofile.CandidateId(
            UUID.fromString("00000000-0000-0000-0000-000000000120")))
        .candidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000121"))
        .sortOrder(1)
        .status(ShortlistCandidateCardStatus.REMOVED)
        .matchReportId(null)
        .clientNotes(null)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();

    when(shortlistService.findAllShortlistsByOrganizationId(ORG_ID))
        .thenReturn(List.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(includedCard, removedCard));

    ConsultantApiQueryService service = new ConsultantApiQueryService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        new PermissionEnforcer(new PermissionEvaluator()));

    PagedResult<com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistSummaryResponse> response =
        service.listShortlists(
            shortlistReadAccessRequest(),
            PagedQuery.builder(ORG_ID).limit(20).offset(0).build(),
            null);

    assertThat(response.items()).singleElement().satisfies(item ->
        assertThat(item.candidateCount()).isEqualTo(1));
  }

  private static AccessRequest shortlistReadAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.SHORTLIST,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
