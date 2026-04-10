package com.bustech.erp.financial.repository;

import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.common.enums.TransactionType;
import com.bustech.erp.financial.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    Page<FinancialTransaction> findByCompanyId(Long companyId, Pageable pageable);

    Page<FinancialTransaction> findByCompanyIdAndType(Long companyId, TransactionType type, Pageable pageable);

    Page<FinancialTransaction> findByCompanyIdAndStatus(Long companyId, TransactionStatus status, Pageable pageable);

    Optional<FinancialTransaction> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
          AND t.type = :type
          AND t.status = 'PAID'
          AND t.paymentDate BETWEEN :from AND :to
        """)
    BigDecimal sumByCompanyIdAndTypeAndPeriod(
        @Param("companyId") Long companyId,
        @Param("type") TransactionType type,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.company.id = :companyId
          AND t.status = 'PENDING'
          AND t.dueDate < :today
        """)
    BigDecimal sumOverdueByCompanyId(
        @Param("companyId") Long companyId,
        @Param("today") LocalDate today
    );
}
