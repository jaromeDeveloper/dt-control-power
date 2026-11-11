package com.controltower.backend.records;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.auth.AppUser;
import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.common.error.ResourceNotFoundException;
import com.controltower.backend.processes.CheckpointService;
import com.controltower.backend.processes.CheckpointStage;

@Service
class BusinessRecordUpdateTransaction {
	private final EntityManager entityManager;
	private final BusinessRecordRepository records;
	private final BusinessRecordAuditRepository audit;
	private final CheckpointService checkpoints;

	BusinessRecordUpdateTransaction(EntityManager entityManager, BusinessRecordRepository records,
			BusinessRecordAuditRepository audit, CheckpointService checkpoints) {
		this.entityManager = entityManager;
		this.records = records;
		this.audit = audit;
		this.checkpoints = checkpoints;
	}

	@Transactional
	public BusinessRecordResponse update(UUID recordId, String value, String requestId, AppUserPrincipal principal) {
		BusinessRecord record = records.findById(recordId)
				.orElseThrow(() -> new ResourceNotFoundException("Business record"));
		AppUser actor = entityManager.getReference(AppUser.class, principal.id());
		checkpoints.recordRunning(record.getProcess(), CheckpointStage.BUSINESS_UPDATE_STARTED, null, actor);
		String oldValue = record.getValue();
		record.updateValue(value, actor);
		audit.save(new BusinessRecordAudit(record, actor, oldValue, value, requestId));
		checkpoints.recordSuccess(record.getProcess(), CheckpointStage.BUSINESS_UPDATE_COMPLETED, null, actor);
		records.flush();
		return BusinessRecordResponse.from(record);
	}
}
