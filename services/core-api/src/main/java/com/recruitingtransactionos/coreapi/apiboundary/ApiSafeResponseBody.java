package com.recruitingtransactionos.coreapi.apiboundary;

public sealed interface ApiSafeResponseBody
    permits ApiErrorResponse,
        ApiAccessDeniedResponse,
        ApiValidationErrorResponse,
        ClientSafeCandidateCardResponse,
        ConsultantCompanySummaryResponse,
        ConsultantCompanyDetailResponse,
        ConsultantJobSummaryResponse,
        ConsultantJobDetailResponse,
        ConsultantShortlistSummaryResponse,
        ConsultantShortlistDetailResponse,
        PagedResult {}
