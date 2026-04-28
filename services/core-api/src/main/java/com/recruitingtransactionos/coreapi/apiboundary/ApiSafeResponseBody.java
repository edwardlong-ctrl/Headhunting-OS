package com.recruitingtransactionos.coreapi.apiboundary;

public sealed interface ApiSafeResponseBody
    permits ApiErrorResponse,
        ApiAccessDeniedResponse,
        ApiValidationErrorResponse,
        ClientSafeCandidateCardResponse {}
