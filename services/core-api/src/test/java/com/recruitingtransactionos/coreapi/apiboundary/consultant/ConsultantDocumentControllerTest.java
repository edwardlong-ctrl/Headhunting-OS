package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceHit;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceRetrievalResult;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentResourceNotFoundException;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentRetrievalResult;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.identityauth.SecurityConfig;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConsultantDocumentController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test-issuer",
    "rto.document-storage.root-dir=${java.io.tmpdir}/rto-documents-controller-test"
})
class ConsultantDocumentControllerTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000200031");
  private static final UUID USER_ACCOUNT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000200032");
  private static final UUID SOURCE_ITEM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000200033");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DocumentUploadService documentUploadService;

  @MockBean
  private DocumentParsingService documentParsingService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void consultantUploadUsesAuthenticatedActorId() throws Exception {
    when(documentUploadService.upload(any(), any())).thenReturn(new DocumentUploadResult(
        new SourceItemId(SOURCE_ITEM_ID),
        UUID.fromString("00000000-0000-0000-0000-000000200034"),
        "sha256:test",
        ORGANIZATION_ID + "/" + SOURCE_ITEM_ID + "/sha256_test/cv.pdf",
        "clean"));

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "cv.pdf",
        "application/pdf",
        "cv".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/consultant/documents/upload")
            .file(file)
            .param("sourceType", "CV")
            .param("origin", "CONSULTANT_UPLOAD")
            .param("title", "Candidate CV")
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.sourceItemId").value(SOURCE_ITEM_ID.toString()))
        .andExpect(jsonPath("$.data.informationPacketId").value("00000000-0000-0000-0000-000000200034"))
        .andExpect(jsonPath("$.data.scanStatus").value("clean"));

    verify(documentUploadService).upload(any(), any());
    verify(documentUploadService).upload(
        org.mockito.ArgumentMatchers.argThat(command ->
            USER_ACCOUNT_ID.equals(command.uploadedByActorId())
                && ORGANIZATION_ID.equals(command.organizationId())),
        any());
  }

  @Test
  void nonConsultantUploadIsDeniedBeforeServiceCall() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "cv.pdf",
        "application/pdf",
        "cv".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/consultant/documents/upload")
            .file(file)
            .param("sourceType", "CV")
            .param("origin", "CONSULTANT_UPLOAD")
            .with(authentication(auth(PortalRole.CLIENT))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("client_unsafe_field_denied"));

    verifyNoInteractions(documentUploadService);
  }

  @Test
  void downloadSanitizesUnsafeFilenameInContentDisposition() throws Exception {
    when(documentUploadService.retrieveDocument(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenReturn(new DocumentRetrievalResult(
            new ByteArrayInputStream("safe".getBytes(StandardCharsets.UTF_8)),
            "application/pdf",
            "../../evil\r\nx.pdf"));

    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/download", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"evil_x.pdf\""))
        .andExpect(content().bytes("safe".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void nonConsultantDownloadIsDeniedBeforeServiceCall() throws Exception {
    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/download", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CLIENT))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("client_unsafe_field_denied"));

    verifyNoInteractions(documentUploadService);
  }

  @Test
  void consultantCanParseDocumentAndReceiveSummary() throws Exception {
    when(documentParsingService.parseDocument(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenReturn(parsedDocument(DocumentProcessingStatus.SUCCEEDED, Optional.empty()));
    when(documentParsingService.countChunksForDocument(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenReturn(3);

    mockMvc.perform(post("/api/consultant/documents/{sourceItemId}/parse", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.processingStatus").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.chunkCount").value(3));
  }

  @Test
  void parseSummarySanitizesUnsafeFailureReason() throws Exception {
    when(documentParsingService.findLatestParsedDocumentByDocumentId(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenReturn(Optional.of(parsedDocument(
            DocumentProcessingStatus.FAILED,
            Optional.of("com.recruitingtransactionos.coreapi.Parser exploded"))));
    when(documentParsingService.countChunksForDocument(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenReturn(0);

    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/parsed", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.processingStatus").value("FAILED"))
        .andExpect(jsonPath("$.data.failureReason").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void consultantCanFetchEvidenceHits() throws Exception {
    ParsedDocument parsedDocument = parsedDocument(DocumentProcessingStatus.SUCCEEDED, Optional.empty());
    when(documentParsingService.retrieveDocumentEvidence(
        ORGANIZATION_ID,
        SOURCE_ITEM_ID,
        "nebula",
        5)).thenReturn(new DocumentEvidenceRetrievalResult(
            parsedDocument,
            List.of(new DocumentEvidenceHit(
                UUID.fromString("00000000-0000-0000-0000-000000200036"),
                0,
                1,
                0,
                42,
                2.0d,
                "Nebula evidence excerpt"))));

    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/evidence", SOURCE_ITEM_ID)
            .param("query", "nebula")
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.processingStatus").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.totalHits").value(1))
        .andExpect(jsonPath("$.data.hits[0].locator").value("page 1 offsets 0-42"));
  }

  @Test
  void evidenceResponseSanitizesUnsafeQueryEcho() throws Exception {
    ParsedDocument parsedDocument = parsedDocument(DocumentProcessingStatus.SUCCEEDED, Optional.empty());
    when(documentParsingService.retrieveDocumentEvidence(
        ORGANIZATION_ID,
        SOURCE_ITEM_ID,
        "qa@example.com",
        5)).thenReturn(new DocumentEvidenceRetrievalResult(
            parsedDocument,
            List.of(new DocumentEvidenceHit(
                UUID.fromString("00000000-0000-0000-0000-000000200036"),
                0,
                1,
                0,
                42,
                2.0d,
                "Nebula evidence excerpt"))));

    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/evidence", SOURCE_ITEM_ID)
            .param("query", "qa@example.com")
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.query").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.data.hits[0].locator").value("page 1 offsets 0-42"));
  }

  @Test
  void parseMissingDocumentReturnsNotFound() throws Exception {
    when(documentParsingService.parseDocument(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenThrow(new DocumentResourceNotFoundException("source item not found in organization"));

    mockMvc.perform(post("/api/consultant/documents/{sourceItemId}/parse", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("not_found"))
        .andExpect(jsonPath("$.error.safeReason").value("document_unavailable"));
  }

  @Test
  void evidenceMissingParsedDocumentReturnsNotFound() throws Exception {
    when(documentParsingService.retrieveDocumentEvidence(ORGANIZATION_ID, SOURCE_ITEM_ID, "nebula", 5))
        .thenThrow(new DocumentResourceNotFoundException("parsed document not found"));

    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/evidence", SOURCE_ITEM_ID)
            .param("query", "nebula")
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("not_found"))
        .andExpect(jsonPath("$.error.safeReason").value("document_unavailable"));
  }

  @Test
  void parsedSummaryMissingDocumentReturnsNotFound() throws Exception {
    when(documentParsingService.findLatestParsedDocumentByDocumentId(ORGANIZATION_ID, SOURCE_ITEM_ID))
        .thenReturn(Optional.empty());

    mockMvc.perform(get("/api/consultant/documents/{sourceItemId}/parsed", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("not_found"))
        .andExpect(jsonPath("$.error.safeReason").value("document_unavailable"));
  }

  @Test
  void nonConsultantParseIsDeniedBeforeServiceCall() throws Exception {
    mockMvc.perform(post("/api/consultant/documents/{sourceItemId}/parse", SOURCE_ITEM_ID)
            .with(authentication(auth(PortalRole.CLIENT))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("client_unsafe_field_denied"));

    verifyNoInteractions(documentParsingService);
  }

  private static ParsedDocument parsedDocument(
      DocumentProcessingStatus status,
      Optional<String> failureReason) {
    Instant now = Instant.parse("2026-05-01T00:00:00Z");
    return new ParsedDocument(
        UUID.fromString("00000000-0000-0000-0000-000000200035"),
        ORGANIZATION_ID,
        new SourceItemId(SOURCE_ITEM_ID),
        status,
        "pdf-parser",
        "v1",
        "application/pdf",
        "sha256:test",
        "en",
        false,
        now,
        status == DocumentProcessingStatus.PENDING_EXTERNAL_PROCESSING
            ? Optional.empty()
            : Optional.of(now),
        failureReason);
  }

  private static Authentication auth(PortalRole portalRole) {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        USER_ACCOUNT_ID,
        ORGANIZATION_ID,
        portalRole,
        "Consultant User",
        UUID.fromString("00000000-0000-0000-0000-000000200035")));
  }
}
