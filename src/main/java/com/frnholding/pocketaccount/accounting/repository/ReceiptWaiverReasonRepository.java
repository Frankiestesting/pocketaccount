package com.frnholding.pocketaccount.accounting.repository;

import com.frnholding.pocketaccount.accounting.repository.entity.ReceiptWaiverReasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptWaiverReasonRepository extends JpaRepository<ReceiptWaiverReasonEntity, String> {
}
