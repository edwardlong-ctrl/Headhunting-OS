package com.recruitingtransactionos.coreapi.consentdisclosure;

public record ConsentDisclosurePrerequisites(
    boolean jobActivated,
    boolean feeAgreementActive,
    boolean priorContactCleared,
    boolean priorApplicationCleared) {}
