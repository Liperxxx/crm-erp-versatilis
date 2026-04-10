package com.bustech.erp.integration.contaazul.client;

import com.bustech.erp.common.exception.IntegrationException;
import com.bustech.erp.integration.contaazul.config.ContaAzulProperties;
import com.bustech.erp.integration.contaazul.dto.ContaAzulAccountDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulCategoryDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulCostCenterDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulEventDetailDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulEventDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContaAzulDataClient {

    private final WebClient.Builder webClientBuilder;
    private final ContaAzulProperties props;

    public List<ContaAzulCategoryDto> fetchCategories(String accessToken) {
        return fetchPaginated(accessToken, props.apiPaths().categories(),
            new ParameterizedTypeReference<ContaAzulPageResponse<ContaAzulCategoryDto>>() {});
    }

    public List<ContaAzulCostCenterDto> fetchCostCenters(String accessToken) {
        return fetchPaginated(accessToken, props.apiPaths().costCenters(),
            new ParameterizedTypeReference<ContaAzulPageResponse<ContaAzulCostCenterDto>>() {});
    }

    public List<ContaAzulAccountDto> fetchAccounts(String accessToken) {
        return fetchPaginated(accessToken, props.apiPaths().accounts(),
            new ParameterizedTypeReference<ContaAzulPageResponse<ContaAzulAccountDto>>() {});
    }

    /**
     * Fetches both receivables and payables, tagging each with RECEITA/DESPESA.
     */
    public List<ContaAzulEventDto> fetchEvents(String accessToken) {
        List<ContaAzulEventDto> all = new ArrayList<>();

        List<ContaAzulEventDto> receivables = fetchPaginated(accessToken,
            props.apiPaths().receivables(),
            new ParameterizedTypeReference<ContaAzulPageResponse<ContaAzulEventDto>>() {});
        for (ContaAzulEventDto dto : receivables) {
            all.add(dto.withType("RECEITA"));
        }

        List<ContaAzulEventDto> payables = fetchPaginated(accessToken,
            props.apiPaths().payables(),
            new ParameterizedTypeReference<ContaAzulPageResponse<ContaAzulEventDto>>() {});
        for (ContaAzulEventDto dto : payables) {
            all.add(dto.withType("DESPESA"));
        }

        log.info("[CA] Eventos: {} recebíveis + {} pagáveis = {} total",
            receivables.size(), payables.size(), all.size());
        return all;
    }

    public Optional<ContaAzulEventDetailDto> fetchEventDetail(String accessToken, String eventId) {
        String path = props.apiPaths().parcelDetail().replace("{id}", eventId);
        try {
            ContaAzulEventDetailDto detail = webClientBuilder.build()
                .get()
                .uri(props.apiBaseUrl() + path)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(ContaAzulEventDetailDto.class)
                .block();
            return Optional.ofNullable(detail);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("[CA] Parcela {} não encontrada (404)", eventId);
            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.error("[CA] Erro ao buscar detalhe da parcela {}: status={}, body={}",
                eventId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IntegrationException(
                "Erro na API Conta Azul ao buscar detalhe da parcela [" + eventId + "]: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[CA] Erro inesperado ao buscar detalhe da parcela {}: {}", eventId, e.getMessage());
            throw new IntegrationException(
                "Erro inesperado ao buscar detalhe da parcela [" + eventId + "]");
        }
    }

    private <T> List<T> fetchPaginated(
            String accessToken,
            String path,
            ParameterizedTypeReference<ContaAzulPageResponse<T>> typeRef) {

        try {
            ContaAzulPageResponse<T> page = webClientBuilder.build()
                .get()
                .uri(props.apiBaseUrl() + path)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(typeRef)
                .block();

            if (page == null) return Collections.emptyList();

            List<T> items = page.content();
            log.info("[CA] {} → {} itens (total={})", path, items.size(), page.itensTotais());
            return items;

        } catch (WebClientResponseException.NotFound e) {
            log.warn("[CA] Path {} not found — returning empty list", path);
            return Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.error("[CA] API error at {}: status={}, body={}",
                path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IntegrationException(
                "Erro na API Conta Azul [" + path + "]: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[CA] Erro inesperado ao buscar {}: {}", path, e.getMessage());
            throw new IntegrationException(
                "Erro inesperado ao buscar dados Conta Azul [" + path + "]");
        }
    }
}
