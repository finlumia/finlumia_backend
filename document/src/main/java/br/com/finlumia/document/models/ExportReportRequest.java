package br.com.finlumia.document.models;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record ExportReportRequest(
        @NotNull ExportFormat format,
        Period period,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<ReportSection> sections,
        String title,
        @JsonProperty("includeCharts") Boolean includeCharts
) {
}
