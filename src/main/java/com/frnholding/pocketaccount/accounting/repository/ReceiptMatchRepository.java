package com.frnholding.pocketaccount.accounting.repository;

import com.frnholding.pocketaccount.accounting.domain.ReceiptMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptMatchRepository extends JpaRepository<ReceiptMatch, UUID> {
    List<ReceiptMatch> findByReceiptId(UUID receiptId);
    List<ReceiptMatch> findByBankTransactionId(UUID bankTransactionId);
    java.util.Optional<ReceiptMatch> findFirstByBankTransactionId(UUID bankTransactionId);
    boolean existsByReceiptIdAndBankTransactionId(UUID receiptId, UUID bankTransactionId);
    boolean existsByReceiptId(UUID receiptId);
    boolean existsByBankTransactionId(UUID bankTransactionId);
    
    @Query("SELECT COALESCE(SUM(rm.matchedAmount), 0) FROM ReceiptMatch rm WHERE rm.bankTransaction.id = :bankTransactionId")
    BigDecimal sumMatchedAmountByBankTransactionId(@Param("bankTransactionId") UUID bankTransactionId);
}
