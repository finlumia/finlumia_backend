package br.com.finlumia.configurator.models;

public record UpdatePermissionRequest(
        Boolean canRead,
        Boolean canWrite,
        Boolean canDelete,
        Boolean canAdmin) {
}
