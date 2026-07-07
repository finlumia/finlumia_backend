package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ImportJob(
        UUID id,
        UUID userKey,
        ImportStatus status,
        String fileName,
        FileType fileType,
        Integer totalRows,
        Integer importedRows,
        List<String> errors,
        String ocrDescription,
        BigDecimal ocrAmount,
        LocalDate ocrDate,
        CategoryId ocrCategory,
        PaymentMethod ocrMethod,
        Double ocrConfidence,
        Instant createdAt,
        byte[] fileContent
) {}
