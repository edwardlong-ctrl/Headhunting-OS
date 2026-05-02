package com.recruitingtransactionos.coreapi.apiboundary.consultant;

public record ConsultantIntakePublishRequest(
    String candidateId,
    String companyId,
    String jobId,
    String jobCompanyId,
    String reason) {}
