package com.bustech.erp.company.service;

import com.bustech.erp.common.exception.BusinessException;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.company.dto.CompanyRequest;
import com.bustech.erp.company.dto.CompanyResponse;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    CompanyRepository companyRepository;

    @InjectMocks
    CompanyService companyService;

    // ─── createCompany ───────────────────────────────────────────────────────

    @Test
    void createCompany_validRequest_returnsCompanyResponse() {
        var request = new CompanyRequest("Acme Corp", "acme-corp");
        var saved   = company(1L, "Acme Corp", "acme-corp");

        when(companyRepository.existsByName("Acme Corp")).thenReturn(false);
        when(companyRepository.existsBySlug("acme-corp")).thenReturn(false);
        when(companyRepository.save(any())).thenReturn(saved);

        var result = companyService.createCompany(request);

        assertThat(result.name()).isEqualTo("Acme Corp");
        assertThat(result.slug()).isEqualTo("acme-corp");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createCompany_duplicateName_throwsBusinessException() {
        var request = new CompanyRequest("Bustech", "bustech");
        when(companyRepository.existsByName("Bustech")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("nome");
    }

    @Test
    void createCompany_duplicateSlug_throwsBusinessException() {
        var request = new CompanyRequest("Bustech Tecnologia", "bustech");
        when(companyRepository.existsByName("Bustech Tecnologia")).thenReturn(false);
        when(companyRepository.existsBySlug("bustech")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("slug");
    }

    @Test
    void createCompany_slugIsLowercased_beforePersistence() {
        var request = new CompanyRequest("Acme Corp", "ACME-CORP");
        var saved   = company(1L, "Acme Corp", "acme-corp");

        when(companyRepository.existsByName("Acme Corp")).thenReturn(false);
        when(companyRepository.existsBySlug("acme-corp")).thenReturn(false);
        when(companyRepository.save(any())).thenReturn(saved);

        var result = companyService.createCompany(request);

        assertThat(result.slug()).isEqualTo("acme-corp");
    }

    // ─── getCompanyById ──────────────────────────────────────────────────────

    @Test
    void getCompanyById_existingId_returnsResponse() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company(1L, "Bustech", "bustech")));

        var result = companyService.getCompanyById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Bustech");
    }

    @Test
    void getCompanyById_unknownId_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── listCompanies ───────────────────────────────────────────────────────

    @Test
    void listCompanies_returnsAllCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(
            company(1L, "Bustech",   "bustech"),
            company(2L, "Versatilis","versatilis")
        ));

        var result = companyService.listCompanies();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CompanyResponse::name)
            .containsExactlyInAnyOrder("Bustech", "Versatilis");
    }

    @Test
    void listCompanies_empty_returnsEmptyList() {
        when(companyRepository.findAll()).thenReturn(List.of());

        assertThat(companyService.listCompanies()).isEmpty();
    }

    // ─── deactivateCompany ───────────────────────────────────────────────────

    @Test
    void deactivateCompany_setsActiveFalseAndPersists() {
        var c = company(1L, "Bustech", "bustech");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(c));
        when(companyRepository.save(any())).thenReturn(c);

        companyService.deactivateCompany(1L);

        assertThat(c.isActive()).isFalse();
        verify(companyRepository).save(c);
    }

    @Test
    void deactivateCompany_unknownId_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.deactivateCompany(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── fixture ─────────────────────────────────────────────────────────────

    static Company company(Long id, String name, String slug) {
        return Company.builder()
            .id(id).name(name).slug(slug).active(true)
            .createdAt(Instant.EPOCH).updatedAt(Instant.EPOCH)
            .build();
    }
}