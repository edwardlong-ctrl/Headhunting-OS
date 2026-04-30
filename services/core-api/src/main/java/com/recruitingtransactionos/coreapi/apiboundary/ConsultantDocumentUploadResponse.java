package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantDocumentUploadResponse(
    String sourceItemId,
    String informationPacketId,
    String contentHash,
    String scanStatus) implements ApiSafeResponseBody {
}
