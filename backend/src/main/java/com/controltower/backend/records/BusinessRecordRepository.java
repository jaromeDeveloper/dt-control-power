package com.controltower.backend.records;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRecordRepository extends JpaRepository<BusinessRecord, UUID> {
	List<BusinessRecord> findByProcess_IdOrderByReferenceAsc(UUID processId);
}
