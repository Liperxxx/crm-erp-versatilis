package com.bustech.erp.integration.contaazul.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed configuration for the Conta Azul OAuth2 integration.
 * Bound from the {@code app.contaazul.*} namespace in application.yml.
 *
 * <h3>Required environment variables (for real API calls)</h3>
 * <pre>
 *   CONTAAZUL_CLIENT_ID      OAuth2 client id — from developer.contaazul.com
 *   CONTAAZUL_CLIENT_SECRET  OAuth2 client secret
 *   CONTAAZUL_REDIRECT_URI   URI registered in the Conta Azul app settings
 * </pre>
 *
 * <h3>Optional</h3>
 * <pre>
 *   CONTAAZUL_FRONTEND_URL   Where to redirect after OAuth2 callback
 *                            (default: http://localhost:3000/dashboard.html)
 * </pre>
 *
 * All other properties have safe defaults and rarely need overriding.
 */
@ConfigurationProperties(prefix = "app.contaazul")
public record ContaAzulProperties(

    /** OAuth2 client id. Set via {@code CONTAAZUL_CLIENT_ID}. */
    String clientId,

    /** OAuth2 client secret. Set via {@code CONTAAZUL_CLIENT_SECRET}. */
    String clientSecret,

    /**
     * OAuth2 redirect URI — must exactly match the URI registered in the
     * Conta Azul developer portal.
     * <ul>
     *   <li>Dev: {@code http://localhost:8081/api/integrations/conta-azul/callback}
     *       (set in application-dev.yml via {@code CONTAAZUL_REDIRECT_URI})</li>
     *   <li>Prod: set {@code CONTAAZUL_REDIRECT_URI} to the public HTTPS URL</li>
     * </ul>
     */
    String redirectUri,

    /**
     * Full URL of the Conta Azul authorization endpoint.
     * Users are redirected here to grant access.
     * Default: {@code https://api.contaazul.com/auth/oauth/v2/authorize}
     */
    @DefaultValue("https://api.contaazul.com/auth/oauth/v2/authorize")
    String authUrl,

    /**
     * Full URL of the Conta Azul token endpoint.
     * Used to exchange an authorization code or refresh token for an access token.
     * Default: {@code https://api.contaazul.com/auth/oauth/v2/token}
     */
    @DefaultValue("https://api.contaazul.com/auth/oauth/v2/token")
    String tokenUrl,

    /**
     * Base URL for all Conta Azul data API calls.
     * Resource paths from {@link ApiPaths} are appended to this URL.
     * Default: {@code https://api.contaazul.com}
     */
    @DefaultValue("https://api.contaazul.com")
    String apiBaseUrl,

    /**
     * Frontend URL to redirect to after the OAuth2 callback completes.
     * A {@code ?contaazul=connected} or {@code ?contaazul=error} query param is appended.
     * Set via {@code CONTAAZUL_FRONTEND_URL}.
     * Default: {@code http://localhost:3000/dashboard.html}
     */
    @DefaultValue("http://localhost:3000/dashboard.html")
    String frontendReturnUrl,

    /**
     * Relative paths for each Conta Azul resource type, appended to {@link #apiBaseUrl}.
     * Override in application.yml once the actual Conta Azul API paths are confirmed.
     */
    ApiPaths apiPaths

) {

    /**
     * Relative API paths for each resource type.
     * All fields have defaults — only override if the Conta Azul API differs.
     */
    public record ApiPaths(
        @DefaultValue("/v1/categories")           String categories,
        @DefaultValue("/v1/costCenters")          String costCenters,
        @DefaultValue("/v1/accounts")             String accounts,
        @DefaultValue("/v1/financial-events")     String events,
        /**
         * Detail endpoint for a single financial event (parcela).
         * The literal {@code {id}} placeholder is replaced at runtime with the event's
         * external ID.
         * Default: {@code /v1/financial-events/{id}}
         */
        @DefaultValue("/v1/financial-events/{id}") String parcelDetail
    ) {}
}
