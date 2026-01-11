package com.frnholding.pocketaccount.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "corrections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String documentId;
    private String documentType;
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> fields;
    private String note;
    private Integer correctionVersion;
    private Instant savedAt;
    private String savedBy;
    private Integer normalizedTransactionsCreated;

    // Convert to domain
    public Correction toDomain() {
        return new Correction(id, documentId, documentType, fields, note, correctionVersion, savedAt, savedBy, normalizedTransactionsCreated);
    }

    // From domain
    public static CorrectionEntity fromDomain(Correction correction) {
        return new CorrectionEntity(correction.getId(), correction.getDocumentId(), correction.getDocumentType(),
                correction.getFields(), correction.getNote(), correction.getCorrectionVersion(),
                correction.getSavedAt(), correction.getSavedBy(), correction.getNormalizedTransactionsCreated());
    }
}