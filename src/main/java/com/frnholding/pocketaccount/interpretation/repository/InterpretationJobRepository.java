package com.frnholding.pocketaccount.interpretation.repository;

import com.frnholding.pocketaccount.interpretation.domain.InterpretationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterpretationJobRepository extends JpaRepository<InterpretationJob, String> {
}
