package com.controltower.backend.records;

import java.time.Instant;
import java.util.UUID;

public record BusinessRecordResponse(
		UUID id, UUID processId, String reference, String value,
		long version, String updatedBy, Instant updatedAt) {
	static BusinessRecordResponse from(BusinessRecord record) {
		return new BusinessRecordResponse(record.getId(), record.getProcess().getId(),
				record.getReference(), record.getValue(), record.getVersion(),
				record.getUpdatedBy().getUsername(), record.getUpdatedAt());
	}
}
