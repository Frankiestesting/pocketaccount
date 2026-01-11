package com.frnholding.pocketaccount.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {
    @Id
    private String id;
    private String documentId;
    private String status;
    private Instant created;
    private String pipeline;
    private boolean useOcr;
    private boolean useAi;
    private String languageHint;

    // Convert to domain
    public Job toDomain() {
        return new Job(id, documentId, status, created, pipeline, useOcr, useAi, languageHint);
    }

    // From domain
    public static JobEntity fromDomain(Job job) {
        return new JobEntity(job.getId(), job.getDocumentId(), job.getStatus(), job.getCreated(),
                           job.getPipeline(), job.isUseOcr(), job.isUseAi(), job.getLanguageHint());
    }
}