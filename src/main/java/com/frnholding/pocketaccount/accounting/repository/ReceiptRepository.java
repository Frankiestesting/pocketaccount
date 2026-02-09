package com.frnholding.pocketaccount.accounting.repository;

import com.frnholding.pocketaccount.accounting.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    @Query("SELECT r FROM Receipt r WHERE " +
           "(:from IS NULL OR r.purchaseDate >= :from) " +
           "AND (:to IS NULL OR r.purchaseDate <= :to) " +
           "ORDER BY r.createdAt DESC")
    List<Receipt> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

        @Query("SELECT r FROM Receipt r WHERE " +
            "(:from IS NULL OR r.purchaseDate >= :from) " +
            "AND (:to IS NULL OR r.purchaseDate <= :to) " +
            "ORDER BY r.createdAt DESC")
        List<Receipt> findByDateRange(@Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          Pageable pageable);
}
