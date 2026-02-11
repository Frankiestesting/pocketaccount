package com.frnholding.pocketaccount.accounting.repository;

import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findByAccountId(UUID accountId);
       List<BankTransaction> findByAccountId(UUID accountId, Pageable pageable);
    boolean existsByAccountIdAndSourceLineHash(UUID accountId, String sourceLineHash);
    
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.account.id = :accountId " +
           "AND (:from IS NULL OR bt.bookingDate >= :from) " +
           "AND (:to IS NULL OR bt.bookingDate <= :to) " +
           "ORDER BY bt.bookingDate DESC")
    List<BankTransaction> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
                                                      @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to);

       @Query("SELECT bt FROM BankTransaction bt WHERE bt.account.id = :accountId " +
              "AND (:from IS NULL OR bt.bookingDate >= :from) " +
              "AND (:to IS NULL OR bt.bookingDate <= :to) " +
              "ORDER BY bt.bookingDate DESC")
       List<BankTransaction> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
                                                         @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to,
                                                         Pageable pageable);

           Optional<BankTransaction> findFirstByAccountIdAndBookingDateAndAmountAndCurrencyAndDescription(
                  UUID accountId,
                  LocalDate bookingDate,
                  BigDecimal amount,
                  String currency,
                  String description
           );
}
