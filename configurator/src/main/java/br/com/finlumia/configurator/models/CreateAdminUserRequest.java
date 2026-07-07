package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email String email,
        @NotNull RoleEnum role,
        UserAdminStatus status,
        Boolean mfa,
        Boolean sendInvite) {
}
