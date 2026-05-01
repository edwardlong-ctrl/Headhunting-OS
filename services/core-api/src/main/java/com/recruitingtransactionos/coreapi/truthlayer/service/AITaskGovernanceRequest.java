package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Objects;
import java.util.UUID;

public record AITaskGovernanceRequest(
    String writeBackTarget,
    String humanReviewStatus,
    ActorRef requestedBy,
    ActorRef reviewActor,
    boolean clientSafeBoundaryApplied,
    boolean bulkApproval) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static AITaskGovernanceRequest from(AITaskRunAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    String target = command.writeBackTarget() == null
        ? AITaskWriteBackTarget.NO_WRITE_BACK.wireValue()
        : command.writeBackTarget().value();
    String reviewStatus = command.humanReviewStatus() == null
        ? AITaskHumanReviewStatus.NOT_REQUIRED.wireValue()
        : command.humanReviewStatus();
    JsonNode metadata = parseMetadata(command.metadataJson());
    return new AITaskGovernanceRequest(
        target,
        reviewStatus,
        command.requestedBy(),
        reviewActor(metadata),
        metadata.path("client_safe_boundary_applied").asBoolean(
            metadata.path("clientSafeBoundaryApplied").asBoolean(false)),
        metadata.path("bulk_approval").asBoolean(
            metadata.path("bulkApproval").asBoolean(false)));
  }

  private static JsonNode parseMetadata(String metadataJson) {
    try {
      return metadataJson == null || metadataJson.isBlank()
          ? OBJECT_MAPPER.createObjectNode()
          : OBJECT_MAPPER.readTree(metadataJson);
    } catch (Exception exception) {
      return OBJECT_MAPPER.createObjectNode();
    }
  }

  private static ActorRef reviewActor(JsonNode metadata) {
    JsonNode actorNode = metadata.path("review_actor");
    if (actorNode.isMissingNode() || actorNode.isNull()) {
      actorNode = metadata.path("reviewActor");
    }
    String userId = text(actorNode, "user_id");
    if (userId == null) {
      userId = text(actorNode, "userId");
    }
    String role = text(actorNode, "role");
    if (userId == null || role == null) {
      return null;
    }
    try {
      return new ActorRef(UUID.fromString(userId), ActorRole.valueOf(role.trim().toUpperCase()));
    } catch (Exception exception) {
      return null;
    }
  }

  private static String text(JsonNode node, String fieldName) {
    JsonNode value = node.path(fieldName);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String text = value.asText(null);
    return text == null || text.isBlank() ? null : text;
  }
}
