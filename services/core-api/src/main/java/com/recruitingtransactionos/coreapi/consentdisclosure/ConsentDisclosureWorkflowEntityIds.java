package com.recruitingtransactionos.coreapi.consentdisclosure;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ConsentDisclosureWorkflowEntityIds {

  private ConsentDisclosureWorkflowEntityIds() {}

  public static UUID consentEntityId(UUID organizationId, String consentRecordRef) {
    return deterministicUuid("consent|" + organizationId + "|" + consentRecordRef);
  }

  public static UUID disclosureEntityId(UUID organizationId, String disclosureRecordRef) {
    return deterministicUuid("disclosure|" + organizationId + "|" + disclosureRecordRef);
  }

  public static UUID unlockRequestEntityId(UUID organizationId, String unlockRequestRef) {
    return deterministicUuid("unlock_request|" + organizationId + "|" + unlockRequestRef);
  }

  public static UUID candidateEntityId(UUID organizationId, String candidateRef) {
    return deterministicUuid("candidate|" + organizationId + "|" + candidateRef);
  }

  private static UUID deterministicUuid(String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }
}
