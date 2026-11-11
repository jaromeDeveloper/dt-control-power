package com.controltower.backend.processes;

import java.time.Instant;
import java.util.UUID;

public record CheckpointResponse(
		UUID checkpointId,
		CheckpointStage stage,
		String areaCode,
		CheckpointStatus status,
		Instant startedAt,
		Instant finishedAt,
		Long durationMs,
		UUID actorId,
		String errorCode,
		String safeMessage,
		String traceId) {

	static CheckpointResponse from(ProcessCheckpoint checkpoint) {
		return new CheckpointResponse(
				checkpoint.getId(), checkpoint.getStage(), checkpoint.getAreaCode(), checkpoint.getStatus(),
				checkpoint.getStartedAt(), checkpoint.getFinishedAt(), checkpoint.getDurationMs(),
				checkpoint.getActor() == null ? null : checkpoint.getActor().getId(),
				checkpoint.getErrorCode(), checkpoint.getSafeMessage(), checkpoint.getTraceId());
	}
}
