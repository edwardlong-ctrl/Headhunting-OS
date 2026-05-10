package com.recruitingtransactionos.coreapi.reportingexport;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReportingExportAdapterRegistry {

  private final Map<ReportingExportType, ReportingExportAdapter> adapters;

  public ReportingExportAdapterRegistry(Map<ReportingExportType, ReportingExportAdapter> adapters) {
    EnumMap<ReportingExportType, ReportingExportAdapter> copied =
        new EnumMap<>(ReportingExportType.class);
    Objects.requireNonNull(adapters, "adapters must not be null").forEach((type, adapter) -> {
      copied.put(
          Objects.requireNonNull(type, "adapters must not contain null type keys"),
          Objects.requireNonNull(adapter, "adapters must not contain null adapter values"));
    });
    this.adapters = Map.copyOf(copied);
  }

  public static ReportingExportAdapterRegistry single(ReportingExportAdapter adapter) {
    EnumMap<ReportingExportType, ReportingExportAdapter> all =
        new EnumMap<>(ReportingExportType.class);
    for (ReportingExportType type : ReportingExportType.values()) {
      all.put(type, adapter);
    }
    return new ReportingExportAdapterRegistry(all);
  }

  public Optional<ReportingExportAdapter> adapterFor(ReportingExportType exportType) {
    return Optional.ofNullable(adapters.get(
        Objects.requireNonNull(exportType, "exportType must not be null")));
  }
}
