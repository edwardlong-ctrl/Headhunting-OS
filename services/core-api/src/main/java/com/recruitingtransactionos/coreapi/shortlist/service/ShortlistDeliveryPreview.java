package com.recruitingtransactionos.coreapi.shortlist.service;

import java.util.Objects;

public record ShortlistDeliveryPreview(
    String clientSafeSummary,
    String pdfSummary,
    String emailSummary,
    String wechatSummary) {

  public ShortlistDeliveryPreview {
    clientSafeSummary = requireNonBlank(clientSafeSummary, "clientSafeSummary");
    pdfSummary = requireNonBlank(pdfSummary, "pdfSummary");
    emailSummary = requireNonBlank(emailSummary, "emailSummary");
    wechatSummary = requireNonBlank(wechatSummary, "wechatSummary");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
