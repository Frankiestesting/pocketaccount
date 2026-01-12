package com.frnholding.pocketaccount.interpretation.repository;

import com.frnholding.pocketaccount.interpretation.domain.InterpretationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterpretationResultRepository extends JpaRepository<InterpretationResult, Long> {
    Optional<InterpretationResult> findByDocumentId(String documentId);
}
