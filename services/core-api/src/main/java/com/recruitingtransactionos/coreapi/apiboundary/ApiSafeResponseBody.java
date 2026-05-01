package com.recruitingtransactionos.coreapi.apiboundary;

public sealed interface ApiSafeResponseBody
    permits ApiErrorResponse,
        ApiAccessDeniedResponse,
        ApiValidationErrorResponse,
        AuthSessionResponse,
        AuthLogoutResponse,
        ClientSafeCandidateCardResponse,
        ConsultantCompanySummaryResponse,
        ConsultantMatchReportResponse,
    ConsultantCompanyDetailResponse,
        ConsultantJobSummaryResponse,
        ConsultantJobDetailResponse,
        ConsultantShortlistSummaryResponse,
        ConsultantShortlistDetailResponse,
        ConsultantDocumentUploadResponse,
        ConsultantParsedDocumentResponse,
        ConsultantDocumentEvidenceResponse,
        PagedResult {}
