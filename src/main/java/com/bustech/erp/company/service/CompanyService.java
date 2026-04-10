package com.bustech.erp.company.service;

import com.bustech.erp.common.exception.BusinessException;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.company.dto.CompanyRequest;
import com.bustech.erp.company.dto.CompanyResponse;
import com.bustech.erp.company.dto.CompanyUpdateRequest;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyResponse> listCompanies() {
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CompanyResponse getCompanyById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        String slug = request.slug().toLowerCase();
        if (companyRepository.existsByName(request.name())) {
            throw new BusinessException("Ja existe uma empresa cadastrada com o nome informado.");
        }
        if (companyRepository.existsBySlug(slug)) {
            throw new BusinessException("Ja existe uma empresa cadastrada com o slug informado.");
        }
        Company company = Company.builder()
                .name(request.name())
                .slug(slug)
                .build();
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyUpdateRequest request) {
        Company company = findById(id);
        String slug = request.slug().toLowerCase();
        if (companyRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException("Ja existe uma empresa cadastrada com o nome informado.");
        }
        if (companyRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessException("Ja existe uma empresa cadastrada com o slug informado.");
        }
        company.setName(request.name());
        company.setSlug(slug);
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public void deactivateCompany(Long id) {
        Company company = findById(id);
        company.setActive(false);
        companyRepository.save(company);
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada com id: " + id));
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.isActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
