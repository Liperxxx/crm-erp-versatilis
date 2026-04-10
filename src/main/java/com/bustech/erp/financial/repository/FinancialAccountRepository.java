package com.bustech.erp.financial.repository;

import com.bustech.erp.financial.entity.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {

    List<FinancialAccount> findByCompanyId(Long companyId);

    Optional<FinancialAccount> findByIdAndCompanyId(Long id, Long companyId);

    Optional<FinancialAccount> findByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndName(Long companyId, String name);
}
