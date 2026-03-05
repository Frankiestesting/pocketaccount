package com.frnholding.pocketaccount.interpretation.repository;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface StatementTransactionRepository extends JpaRepository<StatementTransaction, UUID> {
	java.util.List<StatementTransaction> findByInterpretationResult_JobId(UUID jobId);
	java.util.Optional<StatementTransaction> findFirstByBankTransactionId(java.util.UUID bankTransactionId);

	boolean existsByInterpretationResult_DocumentIdAndApprovedTrue(UUID documentId);

	boolean existsByInterpretationResult_JobIdAndApprovedTrue(UUID jobId);
}
