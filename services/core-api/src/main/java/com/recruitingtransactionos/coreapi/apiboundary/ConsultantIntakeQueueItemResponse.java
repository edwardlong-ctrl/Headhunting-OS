package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantIntakeQueueItemResponse(
    String informationPacketId,
    String title,
    String sourceType,
    String intendedEntityType,
    String stage,
    String stageDetail,
    String createdAt,
    String updatedAt) {

  public ConsultantIntakeQueueItemResponse {
    informationPacketId = ApiBoundaryContractRules.requireNonBlank(
        informationPacketId,
        "informationPacketId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    sourceType = ApiBoundaryContractRules.requireNonBlank(sourceType, "sourceType");
    intendedEntityType = ApiBoundaryContractRules.requireNonBlank(intendedEntityType, "intendedEntityType");
    stage = ApiBoundaryContractRules.requireNonBlank(stage, "stage");
    stageDetail = ApiBoundaryContractRules.requireNonBlank(stageDetail, "stageDetail");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
  }
}
