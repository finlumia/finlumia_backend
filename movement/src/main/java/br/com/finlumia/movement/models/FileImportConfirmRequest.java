package br.com.finlumia.movement.models;

public record FileImportConfirmRequest(
        InstitutionId institution,
        Boolean skipDuplicates
) {
    public FileImportConfirmRequest {
        if (skipDuplicates == null) skipDuplicates = true;
    }
}
