package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantDocumentUploadResponse;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentRetrievalResult;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadException;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/consultant/documents")
public final class ConsultantDocumentController {

  private static final String ACTOR_ROLE_HEADER = "X-RTO-Actor-Role";
  private static final String ORGANIZATION_ID_HEADER = "X-RTO-Organization-Id";

  private final DocumentUploadService documentUploadService;

  public ConsultantDocumentController(DocumentUploadService documentUploadService) {
    this.documentUploadService = Objects.requireNonNull(documentUploadService,
        "documentUploadService must not be null");
  }

  @PostMapping("/upload")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("sourceType") String sourceType,
      @RequestParam("origin") String origin,
      @RequestParam(value = "title", required = false) String title,
      @RequestHeader(name = ACTOR_ROLE_HEADER, required = false) String actorRole,
      @RequestHeader(name = ORGANIZATION_ID_HEADER, required = false) String organizationId) {

    requireConsultantRole(actorRole);
    UUID orgId = parseOrganizationId(organizationId);

    try {
      DocumentUploadCommand command = DocumentUploadCommand.fromWireValues(
          orgId,
          sourceType,
          origin,
          title,
          ActorRole.CONSULTANT,
          null,
          file.getOriginalFilename(),
          file.getContentType(),
          file.getSize());

      InputStream content;
      try {
        content = file.getInputStream();
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to read uploaded file", e);
      }
      DocumentUploadResult result = documentUploadService.upload(command, content);

      ConsultantDocumentUploadResponse response = new ConsultantDocumentUploadResponse(
          result.sourceItemId().value().toString(),
          result.informationPacketId() != null
              ? result.informationPacketId().toString() : null,
          result.contentHash(),
          result.scanStatus());

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponseEnvelope.success(response));
    } catch (IllegalArgumentException e) {
      return validationFailed(e);
    }
  }

  @GetMapping("/{sourceItemId}/download")
  public ResponseEntity<?> download(
      @PathVariable String sourceItemId,
      @RequestHeader(name = ACTOR_ROLE_HEADER, required = false) String actorRole,
      @RequestHeader(name = ORGANIZATION_ID_HEADER, required = false) String organizationId) {

    requireConsultantRole(actorRole);
    UUID orgId = parseOrganizationId(organizationId);
    UUID sid = parseDocumentId(sourceItemId);

    DocumentRetrievalResult result;
    try {
      result = documentUploadService.retrieveDocument(orgId, sid);
    } catch (DocumentUploadException e) {
      return notFound();
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(result.mimeType()));
    headers.set(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + result.filename() + "\"");

    return ResponseEntity.ok()
        .headers(headers)
        .body(new InputStreamResource(result.content()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler({IllegalArgumentException.class, DocumentUploadException.class})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      Exception exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request: " + exception.getMessage()))
            .toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(
      RuntimeException exception) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        new ApiErrorResponse(
            "internal_error",
            "request_failed",
            "Request failed."));
  }

  private static void requireConsultantRole(String actorRole) {
    if (actorRole == null || !PortalRole.CONSULTANT.wireValue().equals(actorRole.strip())) {
      throw new AccessDeniedException(
          new AccessDecision(false,
              "consultant_role_required",
              "Consultant role is required for this endpoint."));
    }
  }

  private static UUID parseOrganizationId(String organizationId) {
    if (organizationId == null || organizationId.isBlank()) {
      throw new IllegalArgumentException("X-RTO-Organization-Id header is required.");
    }
    try {
      return UUID.fromString(organizationId.strip());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid organization ID format.");
    }
  }

  private static UUID parseDocumentId(String sourceItemId) {
    if (sourceItemId == null || sourceItemId.isBlank()) {
      throw new IllegalArgumentException("sourceItemId must not be blank");
    }
    try {
      return UUID.fromString(sourceItemId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid source item ID format.");
    }
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(
        HttpStatus.NOT_FOUND,
        new ApiErrorResponse(
            "not_found",
            "document_unavailable",
            "Document is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
