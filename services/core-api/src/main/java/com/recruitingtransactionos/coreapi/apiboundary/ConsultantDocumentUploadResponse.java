package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantDocumentUploadResponse(
    String sourceItemId,
    String informationPacketId,
    String scanStatus) implements ApiSafeResponseBody {
}
