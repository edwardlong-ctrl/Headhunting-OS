package com.recruitingtransactionos.coreapi.apiboundary;

public sealed interface ApiSafeResponseBody
    permits ApiErrorResponse,
        ApiAccessDeniedResponse,
        ApiValidationErrorResponse,
        AuthSessionResponse,
        AuthLogoutResponse,
        ClientSafeCandidateCardResponse,
        ConsultantCompanySummaryResponse,
        ConsultantCompanyDetailResponse,
        ConsultantJobSummaryResponse,
        ConsultantJobDetailResponse,
        ConsultantShortlistSummaryResponse,
        ConsultantShortlistDetailResponse,
        ConsultantDocumentUploadResponse,
        PagedResult {}
