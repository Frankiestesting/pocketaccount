package com.frnholding.pocketaccount.accounting.repository;

import com.frnholding.pocketaccount.accounting.domain.ReceiptMatch;
import com.frnholding.pocketaccount.accounting.domain.ReceiptMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptMatchRepository extends JpaRepository<ReceiptMatch, UUID> {
    List<ReceiptMatch> findByReceiptIdAndStatus(UUID receiptId, ReceiptMatchStatus status);
    List<ReceiptMatch> findByBankTransactionIdAndStatus(UUID bankTransactionId, ReceiptMatchStatus status);
    java.util.Optional<ReceiptMatch> findFirstByBankTransactionIdAndStatus(UUID bankTransactionId, ReceiptMatchStatus status);
    boolean existsByReceiptIdAndBankTransactionIdAndStatus(UUID receiptId, UUID bankTransactionId, ReceiptMatchStatus status);
    boolean existsByReceiptIdAndStatus(UUID receiptId, ReceiptMatchStatus status);
    boolean existsByBankTransactionIdAndStatus(UUID bankTransactionId, ReceiptMatchStatus status);
    
    @Query("SELECT COALESCE(SUM(rm.matchedAmount), 0) FROM ReceiptMatch rm WHERE rm.bankTransaction.id = :bankTransactionId AND rm.status = :status")
    BigDecimal sumMatchedAmountByBankTransactionId(@Param("bankTransactionId") UUID bankTransactionId,
                                                   @Param("status") ReceiptMatchStatus status);
}
