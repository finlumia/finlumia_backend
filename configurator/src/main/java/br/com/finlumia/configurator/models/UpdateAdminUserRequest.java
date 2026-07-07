package br.com.finlumia.configurator.models;

public record UpdateAdminUserRequest(
        String name,
        RoleEnum role,
        UserAdminStatus status,
        Boolean mfa) {
}
