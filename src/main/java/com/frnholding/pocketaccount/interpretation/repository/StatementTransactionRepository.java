package com.frnholding.pocketaccount.interpretation.repository;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatementTransactionRepository extends JpaRepository<StatementTransaction, Long> {
	java.util.List<StatementTransaction> findByInterpretationResult_JobId(String jobId);
	java.util.Optional<StatementTransaction> findFirstByBankTransactionId(java.util.UUID bankTransactionId);

	boolean existsByInterpretationResult_DocumentIdAndApprovedTrue(String documentId);

	boolean existsByInterpretationResult_JobIdAndApprovedTrue(String jobId);
}
