package br.com.finlumia.docs.support.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record PresignUploadRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String mimeType,

        @Positive
        long fileSizeBytes,

        UUID responseId
) {
}
