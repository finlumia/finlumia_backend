package br.com.finlumia.identify.models;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String name,
        String locale,
        UserTheme theme) {
}
