package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeQueueItemResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeQueueResponse;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantIntakeQueueQueryService {

  private static final int MAX_LIMIT = 25;
  private static final int DEFAULT_LIMIT = 12;

  private final InformationPacketPersistencePort informationPacketPersistencePort;
  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final ObjectProvider<IntakeReviewQueryService> intakeReviewQueryService;

  public ConsultantIntakeQueueQueryService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      ObjectProvider<IntakeReviewQueryService> intakeReviewQueryService) {
    this.informationPacketPersistencePort = Objects.requireNonNull(
        informationPacketPersistencePort, "informationPacketPersistencePort must not be null");
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort, "intakeExtractionRunPort must not be null");
    this.intakeReviewQueryService = Objects.requireNonNull(
        intakeReviewQueryService, "intakeReviewQueryService must not be null");
  }

  public ConsultantIntakeQueueResponse listQueue(UUID organizationId, Integer requestedLimit) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    int limit = requestedLimit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    List<ConsultantIntakeQueueItemResponse> items = informationPacketPersistencePort
        .listRecentByOrganization(organizationId, limit).stream()
        .map(packet -> toQueueItem(organizationId, packet))
        .toList();
    return new ConsultantIntakeQueueResponse(items);
  }

  private ConsultantIntakeQueueItemResponse toQueueItem(UUID organizationId, InformationPacket packet) {
    List<SourceItem> sourceItems = informationPacketPersistencePort.listSourceItems(
        organizationId,
        packet.informationPacketId());
    SourceItem latestSourceItem = sourceItems.stream()
        .max(Comparator.comparing(SourceItem::createdAt))
        .orElse(null);
    QueueStage queueStage = resolveStage(organizationId, packet);
    return new ConsultantIntakeQueueItemResponse(
        packet.informationPacketId().value().toString(),
        titleFor(packet, latestSourceItem),
        latestSourceItem == null
            ? packet.packetType().wireValue()
            : latestSourceItem.sourceType().wireValue(),
        packet.intendedEntityType().wireValue(),
        queueStage.stage(),
        queueStage.detail(),
        packet.createdAt().toString(),
        queueStage.updatedAt().toString());
  }

  private QueueStage resolveStage(UUID organizationId, InformationPacket packet) {
    List<IntakeExtractionRun> runs = intakeExtractionRunPort
        .listByInformationPacket(organizationId, packet.informationPacketId()).stream()
        .filter(run -> run.mode() == IntakeExtractionMode.GOVERNED_AI_V1)
        .sorted(Comparator.comparing(IntakeExtractionRun::createdAt).reversed())
        .toList();
    if (runs.isEmpty()) {
      return new QueueStage("uploaded", packet.processingStatus().wireValue(), packet.updatedAt());
    }

    IntakeExtractionRun latestRun = runs.get(0);
    Instant latestActivityAt = laterOf(packet.updatedAt(), latestRun.completedAt().orElse(latestRun.createdAt()));
    if (latestRun.status() == IntakeExtractionStatus.FAILED) {
      return new QueueStage(
          "extract_failed",
          latestRun.failureReason().orElse("Extraction failed."),
          latestActivityAt);
    }
    if (latestRun.status() == IntakeExtractionStatus.CREATED) {
      return new QueueStage("extracting", latestRun.status().wireValue(), latestActivityAt);
    }
    if (latestRun.outputEnvelope().isEmpty()) {
      return new QueueStage("extracted", latestRun.status().wireValue(), latestActivityAt);
    }

    try {
      IntakeReviewQueryService reviewQueryService = intakeReviewQueryService.getIfAvailable();
      if (reviewQueryService == null) {
        return new QueueStage("extracted", "Extraction complete.", latestActivityAt);
      }
      IntakeReviewQueryService.IntakeReviewView reviewView = reviewQueryService.reviewView(
          organizationId,
          packet.informationPacketId());
      boolean anyReviewed = reviewView.facts().stream().anyMatch(fact -> fact.latestReview() != null);
      boolean allApproved = !reviewView.facts().isEmpty() && reviewView.facts().stream().allMatch(
          fact -> fact.latestReview() != null && fact.latestReview().decision() == ReviewDecision.APPROVED);
      if (allApproved) {
        return new QueueStage("ready_for_publish", "All current facts are approved.", latestActivityAt);
      }
      if (anyReviewed) {
        return new QueueStage("in_review", "Review decisions are in progress.", latestActivityAt);
      }
    } catch (IllegalArgumentException ignored) {
      // Keep the queue resilient even when one packet lacks a readable review surface.
    }
    return new QueueStage("extracted", "Extraction complete.", latestActivityAt);
  }

  private static Instant laterOf(Instant first, Instant second) {
    return first.isAfter(second) ? first : second;
  }

  private static String titleFor(InformationPacket packet, SourceItem latestSourceItem) {
    if (latestSourceItem != null) {
      if (latestSourceItem.title() != null) {
        return latestSourceItem.title();
      }
      if (latestSourceItem.originalFilename() != null) {
        return latestSourceItem.originalFilename();
      }
    }
    return packet.intendedEntityType().wireValue() + " packet "
        + packet.informationPacketId().value().toString().substring(0, 8);
  }

  private record QueueStage(String stage, String detail, Instant updatedAt) {
  }
}
