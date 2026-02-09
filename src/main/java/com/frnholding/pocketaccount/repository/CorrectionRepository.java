package com.frnholding.pocketaccount.repository;

import com.frnholding.pocketaccount.domain.CorrectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorrectionRepository extends JpaRepository<CorrectionEntity, Long> {

    @Query("SELECT MAX(c.correctionVersion) FROM CorrectionEntity c WHERE c.documentId = :documentId")
    Optional<Integer> findMaxCorrectionVersionByDocumentId(@Param("documentId") String documentId);

    Optional<CorrectionEntity> findTopByDocumentIdOrderByCorrectionVersionDesc(String documentId);
}