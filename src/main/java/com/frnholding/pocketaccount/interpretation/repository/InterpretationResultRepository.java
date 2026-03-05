package com.frnholding.pocketaccount.interpretation.repository;

import com.frnholding.pocketaccount.interpretation.domain.InterpretationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface InterpretationResultRepository extends JpaRepository<InterpretationResult, UUID> {
    @Query("SELECT ir FROM InterpretationResult ir WHERE ir.documentId = :documentId ORDER BY ir.interpretedAt DESC LIMIT 1")
    Optional<InterpretationResult> findByDocumentId(UUID documentId);
    
    Optional<InterpretationResult> findByJobId(UUID jobId);
}
