package com.bustech.erp.integration.contaazul.client;

import com.bustech.erp.common.exception.IntegrationException;
import com.bustech.erp.integration.contaazul.config.ContaAzulProperties;
import com.bustech.erp.integration.contaazul.dto.ContaAzulTokenResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP client dedicated to Conta Azul's OAuth2 token endpoint.
 *
 * <p>Handles two flows:
 * <ul>
 *   <li>{@link #exchangeCode} — authorization_code grant (first-time connection)</li>
 *   <li>{@link #exchangeRefreshToken} — refresh_token grant (token renewal)</li>
 * </ul>
 *
 * <p>Authentication against the token endpoint uses HTTP Basic (client_id:client_secret).
 * Credentials are <strong>never</strong> written to logs — only the HTTP status and
 * a masked token tail are logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContaAzulTokenClient {

    private final WebClient.Builder webClientBuilder;
    private final ContaAzulProperties props;

    private WebClient webClient;

    @PostConstruct
    void init() {
        webClient = webClientBuilder
                .baseUrl(props.tokenUrl())
                .defaultHeader("Authorization", buildBasicAuth())
                .build();
        log.info("ContaAzulTokenClient inicializado — token endpoint: {}", props.tokenUrl());
    }

    /**
     * Exchanges an authorization code for an access token + refresh token.
     *
     * @param code the authorization code received in the OAuth2 callback
     * @return token response with access_token, refresh_token, expires_in, token_type
     * @throws IntegrationException on any HTTP or connectivity error
     */
    public ContaAzulTokenResponse exchangeCode(String code) {
        log.debug("Trocando authorization_code por token [grant=authorization_code, redirect_uri={}]",
                props.redirectUri());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.redirectUri());

        return post(form, "authorization_code");
    }

    /**
     * Exchanges a refresh token for a new access token.
     *
     * @param refreshToken the stored refresh token
     * @return token response with new access_token, refresh_token, expires_in, token_type
     * @throws IntegrationException on any HTTP or connectivity error
     */
    public ContaAzulTokenResponse exchangeRefreshToken(String refreshToken) {
        log.debug("Renovando token [grant=refresh_token, token=****{}]", tail(refreshToken));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        return post(form, "refresh_token");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ContaAzulTokenResponse post(MultiValueMap<String, String> form, String grantLabel) {
        try {
            ContaAzulTokenResponse response = webClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(ContaAzulTokenResponse.class)
                    .block();

            log.info("Token Conta Azul obtido [grant={}, expires_in={}s, access=****{}]",
                    grantLabel,
                    response != null ? response.expiresIn() : "?",
                    response != null ? tail(response.accessToken()) : "?");

            return response;

        } catch (WebClientResponseException e) {
            String sanitizedBody = sanitize(e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("Conta Azul rejeitou credenciais OAuth2 [grant={}, status=401] — "
                        + "verifique client_id/client_secret", grantLabel);
                throw new IntegrationException(
                        "Credenciais OAuth2 inválidas. Verifique CONTAAZUL_CLIENT_ID e CONTAAZUL_CLIENT_SECRET.");
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("Conta Azul recusou a requisição [grant={}, status=400, body={}]",
                        grantLabel, sanitizedBody);
                throw new IntegrationException(
                        "Requisição inválida ao trocar token [grant=" + grantLabel + "]: " + sanitizedBody);
            }
            log.error("Conta Azul token endpoint retornou erro [grant={}, status={}, body={}]",
                    grantLabel, e.getStatusCode().value(), sanitizedBody);
            throw new IntegrationException(
                    "Erro HTTP " + e.getStatusCode().value() + " ao obter token Conta Azul.");
        } catch (Exception e) {
            log.error("Erro de conectividade ao chamar token endpoint Conta Azul [grant={}]: {}",
                    grantLabel, e.getMessage());
            throw new IntegrationException("Erro de conectividade ao obter token Conta Azul: " + e.getMessage());
        }
    }

    /** Builds the Authorization: Basic header from client credentials (never logged). */
    private String buildBasicAuth() {
        String credentials = props.clientId() + ":" + props.clientSecret();
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns last 6 chars of a token string for safe logging. */
    private static String tail(String token) {
        if (token == null || token.length() <= 6) return "??????";
        return token.substring(token.length() - 6);
    }

    /** Strips any value that looks like a token/secret from error response bodies. */
    private static String sanitize(String body) {
        if (body == null) return "";
        // Replace any value that looks like a JWT or long opaque token
        return body.replaceAll("[A-Za-z0-9_\\-]{40,}", "****");
    }
}
