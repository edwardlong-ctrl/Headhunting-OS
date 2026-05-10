package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ClientShortlistFeedbackExportAdapter implements ReportingExportAdapter {

  private final ClientSafeCandidateProjectionService projectionService;
  private final ClientShortlistFeedbackSource source;

  public ClientShortlistFeedbackExportAdapter(
      ClientSafeCandidateProjectionService projectionService,
      ClientShortlistFeedbackSource source) {
    this.projectionService = Objects.requireNonNull(
        projectionService,
        "projectionService must not be null");
    this.source = Objects.requireNonNull(source, "source must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    ArrayList<ReportingExportSection> sections = new ArrayList<>();
    for (InternalCandidateProjectionSnapshot snapshot : source.snapshotsFor(request)) {
      ClientSafeCandidateCard card = projectionService.project(access(), snapshot);
      sections.add(new ReportingExportSection("client-safe-candidate", List.of(
          field("anonymousCandidateRef", card.anonymousCandidateRef().value()),
          field("projectionVersion", card.projectionVersion()),
          field("redactionLevel", card.redactionLevel().wireValue()),
          field("generalizedHeadline", card.generalizedHeadline()),
          field("safeSummary", card.safeSummary()),
          field("safeSkillSummary", card.safeSkillSummary()))));
    }
    sections.addAll(source.feedbackSectionsFor(request));
    return new ReportingExportPayload(
        "json",
        "client_safe_shortlist_feedback_export",
        "client_same_organization_shortlist_review",
        false,
        sections,
        List.of("ClientSafeCandidateProjectionService", "Client shortlist feedback source"),
        List.of());
  }

  private static ReportingExportField field(String name, String value) {
    return new ReportingExportField(name, value, FieldVisibilityPolicy.CLIENT_SAFE);
  }

  private static AccessRequest access() {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  public interface ClientShortlistFeedbackSource {

    List<InternalCandidateProjectionSnapshot> snapshotsFor(ReportingExportRequest request);

    List<ReportingExportSection> feedbackSectionsFor(ReportingExportRequest request);
  }
}
