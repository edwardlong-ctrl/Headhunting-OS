package com.recruitingtransactionos.coreapi.candidateprofile;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Objects;

public record CandidateProfileFieldSourceReference(
    CandidateProfileFieldSourceType sourceType,
    String sourceId,
    String sourceTrust,
    Instant createdAt) {

  public CandidateProfileFieldSourceReference {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    sourceId = CandidateProfileGuards.requireNonBlank(sourceId, "sourceId");
    sourceTrust = CandidateProfileGuards.optionalNonBlank(sourceTrust, "sourceTrust");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static CandidateProfileFieldSourceReference claimLedgerItem(
      ClaimId claimId,
      Instant createdAt) {
    Objects.requireNonNull(claimId, "claimId must not be null");
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
        claimId.value().toString(),
        "claim_ledger_item",
        createdAt);
  }

  public static CandidateProfileFieldSourceReference reviewEvent(
      ReviewEventId reviewEventId,
      Instant createdAt) {
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.REVIEW_EVENT,
        reviewEventId.value().toString(),
        "review_event",
        createdAt);
  }

  public static CandidateProfileFieldSourceReference sourceItem(
      SourceItemId sourceItemId,
      Instant createdAt) {
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.SOURCE_ITEM,
        sourceItemId.value().toString(),
        "source_item",
        createdAt);
  }

  public static CandidateProfileFieldSourceReference informationPacket(
      InformationPacketId informationPacketId,
      Instant createdAt) {
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.INFORMATION_PACKET,
        informationPacketId.value().toString(),
        "information_packet",
        createdAt);
  }

  public static CandidateProfileFieldSourceReference intakeExtractionRun(
      IntakeExtractionRunId extractionRunId,
      Instant createdAt) {
    Objects.requireNonNull(extractionRunId, "extractionRunId must not be null");
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.INTAKE_EXTRACTION_RUN,
        extractionRunId.value().toString(),
        "intake_extraction_run",
        createdAt);
  }

  public static CandidateProfileFieldSourceReference workflowEvent(
      WorkflowEventId workflowEventId,
      Instant createdAt) {
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.WORKFLOW_EVENT,
        workflowEventId.value().toString(),
        "workflow_event",
        createdAt);
  }

  public static CandidateProfileFieldSourceReference sourceSpan(
      String sourceSpanRef,
      String sourceTrust,
      Instant createdAt) {
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.SOURCE_SPAN,
        sourceSpanRef,
        sourceTrust,
        createdAt);
  }

  public static CandidateProfileFieldSourceReference externalEvidence(
      String evidenceRef,
      String sourceTrust,
      Instant createdAt) {
    return new CandidateProfileFieldSourceReference(
        CandidateProfileFieldSourceType.EXTERNAL_EVIDENCE,
        evidenceRef,
        sourceTrust,
        createdAt);
  }
}
