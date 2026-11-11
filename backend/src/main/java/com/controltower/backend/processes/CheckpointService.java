package com.controltower.backend.processes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.auth.AppUser;

@Service
public class CheckpointService {
	private final ProcessCheckpointRepository checkpoints;
	private final CheckpointStatusHistoryRepository history;

	CheckpointService(ProcessCheckpointRepository checkpoints, CheckpointStatusHistoryRepository history) {
		this.checkpoints = checkpoints;
		this.history = history;
	}

	@Transactional
	public ProcessCheckpoint recordSuccess(
			BusinessProcess process, CheckpointStage stage, String areaCode, AppUser actor) {
		ProcessCheckpoint checkpoint = checkpoints.save(ProcessCheckpoint.success(process, stage, areaCode, actor));
		history.save(new CheckpointStatusHistory(checkpoint, actor));
		return checkpoint;
	}

	@Transactional
	public ProcessCheckpoint recordRunning(
			BusinessProcess process, CheckpointStage stage, String areaCode, AppUser actor) {
		ProcessCheckpoint checkpoint = checkpoints.save(ProcessCheckpoint.running(process, stage, areaCode, actor));
		history.save(new CheckpointStatusHistory(checkpoint, actor));
		return checkpoint;
	}

	@Transactional
	public ProcessCheckpoint recordFailure(
			BusinessProcess process, CheckpointStage stage, AppUser actor,
			String errorCode, String safeMessage) {
		ProcessCheckpoint checkpoint = checkpoints.save(
				ProcessCheckpoint.failure(process, stage, actor, errorCode, safeMessage));
		history.save(new CheckpointStatusHistory(checkpoint, actor));
		return checkpoint;
	}
}
