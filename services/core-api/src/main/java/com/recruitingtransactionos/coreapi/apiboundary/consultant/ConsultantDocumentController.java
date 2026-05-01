package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantDocumentEvidenceResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantDocumentUploadResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantParsedDocumentResponse;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceRetrievalResult;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentResourceNotFoundException;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentRetrievalResult;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadException;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/consultant/documents")
public final class ConsultantDocumentController {


  private final DocumentUploadService documentUploadService;
  private final DocumentParsingService documentParsingService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantDocumentController(
      DocumentUploadService documentUploadService,
      DocumentParsingService documentParsingService) {
    this(
        documentUploadService,
        documentParsingService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  private ConsultantDocumentController(
      DocumentUploadService documentUploadService,
      DocumentParsingService documentParsingService,
      PermissionEnforcer permissionEnforcer) {
    this.documentUploadService = Objects.requireNonNull(
        documentUploadService, "documentUploadService must not be null");
    this.documentParsingService = Objects.requireNonNull(
        documentParsingService, "documentParsingService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(
        permissionEnforcer, "permissionEnforcer must not be null");
  }

  @PostMapping("/upload")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("sourceType") String sourceType,
      @RequestParam("origin") String origin,
      @RequestParam(value = "title", required = false) String title,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {

    requireRawSourceAccess(principal.portalRole(), AccessAction.CREATE);
    UUID orgId = principal.organizationId();

    try {
      DocumentUploadCommand command = DocumentUploadCommand.fromWireValues(
          orgId,
          sourceType,
          origin,
          title,
          ActorRole.CONSULTANT,
          principal.userAccountId(),
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
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {

    requireRawSourceAccess(principal.portalRole(), AccessAction.READ);
    UUID orgId = principal.organizationId();
    UUID sid = parseDocumentId(sourceItemId);

    DocumentRetrievalResult result;
    try {
      result = documentUploadService.retrieveDocument(orgId, sid);
    } catch (DocumentUploadException e) {
      return notFound();
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(result.mimeType()));
    headers.setContentDisposition(ContentDisposition.attachment()
        .filename(DocumentStoreKey.safeDownloadFilename(result.filename()))
        .build());

    return ResponseEntity.ok()
        .headers(headers)
        .body(new InputStreamResource(result.content()));
  }

  @PostMapping("/{sourceItemId}/parse")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> parseDocument(
      @PathVariable String sourceItemId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireRawSourceAccess(principal.portalRole(), AccessAction.UPDATE);
    UUID orgId = principal.organizationId();
    UUID sid = parseDocumentId(sourceItemId);
    ParsedDocument parsedDocument = documentParsingService.parseDocument(orgId, sid);
    return ResponseEntity.ok(ApiResponseEnvelope.success(toParsedDocumentResponse(sid, parsedDocument, orgId)));
  }

  @GetMapping("/{sourceItemId}/parsed")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> parsedDocument(
      @PathVariable String sourceItemId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireRawSourceAccess(principal.portalRole(), AccessAction.READ);
    UUID orgId = principal.organizationId();
    UUID sid = parseDocumentId(sourceItemId);
    ParsedDocument parsedDocument = documentParsingService.findLatestParsedDocumentByDocumentId(orgId, sid)
        .orElseThrow(() -> new DocumentResourceNotFoundException("parsed document unavailable"));
    return ResponseEntity.ok(ApiResponseEnvelope.success(toParsedDocumentResponse(sid, parsedDocument, orgId)));
  }

  @GetMapping("/{sourceItemId}/evidence")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> evidence(
      @PathVariable String sourceItemId,
      @RequestParam(value = "query", required = false) String query,
      @RequestParam(value = "limit", required = false, defaultValue = "5") int limit,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireRawSourceAccess(principal.portalRole(), AccessAction.READ);
    UUID orgId = principal.organizationId();
    UUID sid = parseDocumentId(sourceItemId);
    DocumentEvidenceRetrievalResult result =
        documentParsingService.retrieveDocumentEvidence(orgId, sid, query, limit);
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantDocumentEvidenceResponse(
        sid.toString(),
        result.parsedDocument().parsedDocumentId().toString(),
        result.parsedDocument().processingStatus().wireValue(),
        query == null ? null : query.strip(),
        result.hits().size(),
        result.hits().stream().map(hit -> new ConsultantDocumentEvidenceResponse.Hit(
            hit.parsedDocumentChunkId().toString(),
            hit.chunkIndex(),
            hit.pageNumber(),
            hit.startOffset(),
            hit.endOffset(),
            hit.score(),
            hit.excerpt())).toList())));
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

  @ExceptionHandler(DocumentResourceNotFoundException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> documentResourceNotFound(
      DocumentResourceNotFoundException exception) {
    return error(
        HttpStatus.NOT_FOUND,
        new ApiErrorResponse(
            "not_found",
            "document_unavailable",
            "Document is unavailable."));
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

  private void requireRawSourceAccess(PortalRole portalRole, AccessAction action) {
    permissionEnforcer.requireAllowed(new AccessRequest(
        portalRole,
        ResourceType.SOURCE_ITEM,
        action,
        FieldClassification.RAW_SOURCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));
  }

  private ConsultantParsedDocumentResponse toParsedDocumentResponse(
      UUID sourceItemId,
      ParsedDocument parsedDocument,
      UUID organizationId) {
    return new ConsultantParsedDocumentResponse(
        sourceItemId.toString(),
        parsedDocument.parsedDocumentId().toString(),
        parsedDocument.processingStatus().wireValue(),
        parsedDocument.parserName(),
        parsedDocument.parserVersion(),
        parsedDocument.mediaType(),
        parsedDocument.ocrRequired(),
        documentParsingService.countChunksForDocument(organizationId, sourceItemId),
        parsedDocument.createdAt().toString(),
        parsedDocument.completedAt().map(Instant::toString).orElse(null),
        parsedDocument.failureReason().orElse(null));
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
