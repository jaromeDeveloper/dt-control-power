package com.controltower.backend.records;

import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
		UUID id, UUID recordId, UUID processId, String actor,
		String oldValue, String newValue, AuditOutcome outcome,
		Instant occurredAt, String requestId) {
	static AuditResponse from(BusinessRecordAudit audit) {
		return new AuditResponse(audit.getId(), audit.getRecord().getId(), audit.getProcess().getId(),
				audit.getActor().getUsername(), audit.getOldValue(), audit.getNewValue(),
				audit.getOutcome(), audit.getOccurredAt(), audit.getRequestId());
	}
}
