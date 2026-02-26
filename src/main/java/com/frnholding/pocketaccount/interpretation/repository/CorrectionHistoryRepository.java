package com.frnholding.pocketaccount.interpretation.repository;

import com.frnholding.pocketaccount.interpretation.repository.entity.CorrectionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CorrectionHistoryRepository extends JpaRepository<CorrectionHistoryEntity, UUID> {
    @Query("SELECT MAX(c.correctionVersion) FROM CorrectionHistoryEntity c WHERE c.documentId = :documentId")
    Optional<Integer> findMaxCorrectionVersionByDocumentId(@Param("documentId") UUID documentId);
}
