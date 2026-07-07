package br.com.finlumia.movement.views;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.movement.models.FileType;
import br.com.finlumia.movement.models.ImportJob;
import br.com.finlumia.movement.models.ImportStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ImportJobView(
        UUID id,
        ImportStatus status,
        @JsonProperty("fileName") String fileName,
        @JsonProperty("fileType") FileType fileType,
        @JsonProperty("totalRows") Integer totalRows,
        @JsonProperty("importedRows") Integer importedRows,
        List<String> errors,
        @JsonProperty("createdAt") Instant createdAt
) {
    public static ImportJobView from(ImportJob job) {
        return new ImportJobView(
                job.id(), job.status(), job.fileName(), job.fileType(),
                job.totalRows(), job.importedRows(), job.errors(), job.createdAt());
    }
}
