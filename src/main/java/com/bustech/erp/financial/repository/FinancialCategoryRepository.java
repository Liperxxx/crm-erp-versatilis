package com.bustech.erp.financial.repository;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.financial.entity.FinancialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, Long> {

    List<FinancialCategory> findByCompanyId(Long companyId);

    List<FinancialCategory> findByCompanyIdAndActive(Long companyId, boolean active);

    List<FinancialCategory> findByCompanyIdAndType(Long companyId, FinancialDirection type);

    List<FinancialCategory> findByCompanyIdAndTypeAndActive(Long companyId, FinancialDirection type, boolean active);

    Optional<FinancialCategory> findByIdAndCompanyId(Long id, Long companyId);

    Optional<FinancialCategory> findByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndNameAndType(Long companyId, String name, FinancialDirection type);

    /**
     * Marks as inactive all synced categories (externalId IS NOT NULL) for the given
     * company whose externalId is no longer present in the latest Conta Azul response.
     * Manually-created categories (externalId IS NULL) are never touched.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE FinancialCategory c SET c.active = false
        WHERE c.company.id = :companyId
          AND c.externalId IS NOT NULL
          AND c.externalId NOT IN :externalIds
        """)
    int deactivateNotIn(
        @Param("companyId") Long companyId,
        @Param("externalIds") Collection<String> externalIds
    );
}
