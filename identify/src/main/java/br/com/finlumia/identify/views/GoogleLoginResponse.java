package br.com.finlumia.identify.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleLoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        UserProfileResponse user,
        @JsonProperty("is_new_user") boolean isNewUser) {
}
