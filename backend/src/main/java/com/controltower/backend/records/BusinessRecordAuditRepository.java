package com.controltower.backend.records;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface BusinessRecordAuditRepository extends JpaRepository<BusinessRecordAudit, UUID> {
	Page<BusinessRecordAudit> findByProcess_Id(UUID processId, Pageable pageable);
}
