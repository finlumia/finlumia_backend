package br.com.finlumia.movement.views;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.ImportJob;
import br.com.finlumia.movement.models.ImportStatus;
import br.com.finlumia.movement.models.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OcrPreviewView(
        @JsonProperty("jobId") UUID jobId,
        ImportStatus status,
        OcrExtractedView extracted
) {
    public record OcrExtractedView(
            String description,
            BigDecimal amount,
            LocalDate date,
            CategoryId category,
            PaymentMethod method,
            double confidence
    ) {}

    public static OcrPreviewView from(ImportJob job) {
        OcrExtractedView extracted = null;
        if (job.ocrDescription() != null) {
            extracted = new OcrExtractedView(
                    job.ocrDescription(),
                    job.ocrAmount(),
                    job.ocrDate(),
                    job.ocrCategory(),
                    job.ocrMethod(),
                    job.ocrConfidence() != null ? job.ocrConfidence() : 0.0);
        }
        return new OcrPreviewView(job.id(), job.status(), extracted);
    }
}
