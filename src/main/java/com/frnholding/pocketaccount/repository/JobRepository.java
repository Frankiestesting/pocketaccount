package com.frnholding.pocketaccount.repository;

import com.frnholding.pocketaccount.domain.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
}