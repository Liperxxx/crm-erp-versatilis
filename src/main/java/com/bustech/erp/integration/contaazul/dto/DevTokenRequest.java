package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the dev-only manual token injection endpoint.
 * Only used in the development profile to bypass the OAuth2 redirect flow
 * when a public callback URL is not available locally.
 */
public record DevTokenRequest(
    @NotBlank
    @JsonProperty("access_token")
    String accessToken,

    @NotBlank
    @JsonProperty("refresh_token")
    String refreshToken,

    @Min(60)
    @JsonProperty("expires_in")
    long expiresIn
) {}
