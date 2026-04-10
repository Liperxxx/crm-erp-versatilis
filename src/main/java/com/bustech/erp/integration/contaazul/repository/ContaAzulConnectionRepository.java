package com.bustech.erp.integration.contaazul.repository;

import com.bustech.erp.common.enums.ContaAzulConnectionStatus;
import com.bustech.erp.integration.contaazul.entity.ContaAzulConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContaAzulConnectionRepository extends JpaRepository<ContaAzulConnection, Long> {

    @Query("SELECT c FROM ContaAzulConnection c JOIN FETCH c.company WHERE c.company.id = :companyId")
    Optional<ContaAzulConnection> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c FROM ContaAzulConnection c JOIN FETCH c.company WHERE c.status = :status")
    List<ContaAzulConnection> findAllByStatusWithCompany(@Param("status") ContaAzulConnectionStatus status);

    boolean existsByCompanyId(Long companyId);

    boolean existsByCompanyIdAndStatus(Long companyId, ContaAzulConnectionStatus status);
}
