package com.frnholding.pocketaccount.accounting.repository;

import com.frnholding.pocketaccount.accounting.domain.ReceiptMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptMatchRepository extends JpaRepository<ReceiptMatch, UUID> {
    List<ReceiptMatch> findByReceiptId(UUID receiptId);
    List<ReceiptMatch> findByBankTransactionId(UUID bankTransactionId);
}
