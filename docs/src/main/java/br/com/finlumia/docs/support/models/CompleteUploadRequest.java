package br.com.finlumia.docs.support.models;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CompleteUploadRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String mimeType,

        UUID responseId
) {
}
