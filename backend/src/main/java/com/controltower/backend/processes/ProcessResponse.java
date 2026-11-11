package com.controltower.backend.processes;

import java.time.Instant;
import java.util.UUID;

import com.controltower.backend.files.FileResponse;

public record ProcessResponse(
		UUID processId,
		ProcessStatus status,
		Instant startedAt,
		Instant finishedAt,
		String description,
		String createdBy,
		FileResponse file) {

	static ProcessResponse from(BusinessProcess process) {
		return new ProcessResponse(
				process.getId(), process.getStatus(), process.getStartedAt(), process.getFinishedAt(),
				process.getDescription(), process.getCreatedBy().getUsername(),
				FileResponse.from(process.getFile(), process.getId()));
	}
}
