package com.controltower.backend.records;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.auth.AppUser;
import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.processes.CheckpointService;
import com.controltower.backend.processes.CheckpointStage;

@Service
class BusinessUpdateFailureRecorder {
	private final EntityManager entityManager;
	private final BusinessRecordRepository records;
	private final BusinessRecordAuditRepository audit;
	private final CheckpointService checkpoints;

	BusinessUpdateFailureRecorder(EntityManager entityManager, BusinessRecordRepository records,
			BusinessRecordAuditRepository audit, CheckpointService checkpoints) {
		this.entityManager = entityManager;
		this.records = records;
		this.audit = audit;
		this.checkpoints = checkpoints;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(UUID recordId, String attemptedValue, String requestId, AppUserPrincipal principal) {
		records.findById(recordId).ifPresent(record -> {
			AppUser actor = entityManager.getReference(AppUser.class, principal.id());
			audit.save(BusinessRecordAudit.failed(record, actor, attemptedValue, requestId));
			checkpoints.recordFailure(record.getProcess(), CheckpointStage.BUSINESS_UPDATE_FAILED,
					actor, "BUSINESS_UPDATE_FAILED", "Business record update failed");
		});
	}
}
