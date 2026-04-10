package com.bustech.erp.financial.repository;

import com.bustech.erp.financial.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {

    List<CostCenter> findByCompanyId(Long companyId);

    List<CostCenter> findByCompanyIdAndActive(Long companyId, boolean active);

    Optional<CostCenter> findByIdAndCompanyId(Long id, Long companyId);

    Optional<CostCenter> findByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    /**
     * Marks as inactive all synced cost centers (externalId IS NOT NULL) for the given
     * company whose externalId is no longer present in the latest Conta Azul response.
     * Manually-created cost centers (externalId IS NULL) are never touched.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE CostCenter cc SET cc.active = false
        WHERE cc.company.id = :companyId
          AND cc.externalId IS NOT NULL
          AND cc.externalId NOT IN :externalIds
        """)
    int deactivateNotIn(
        @Param("companyId") Long companyId,
        @Param("externalIds") Collection<String> externalIds
    );
}
