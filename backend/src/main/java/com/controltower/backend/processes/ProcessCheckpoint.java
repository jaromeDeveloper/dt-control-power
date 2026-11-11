package com.controltower.backend.processes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.controltower.backend.auth.AppUser;

@Entity
@Table(name = "process_checkpoints")
public class ProcessCheckpoint {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "process_id", nullable = false)
	private BusinessProcess process;

	@Enumerated(EnumType.STRING)
	@Column(length = 48, nullable = false)
	private CheckpointStage stage;

	@Column(name = "area_code", length = 16)
	private String areaCode;

	@Enumerated(EnumType.STRING)
	@Column(length = 24, nullable = false)
	private CheckpointStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "duration_ms")
	private Long durationMs;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private AppUser actor;

	@Column(name = "trace_id", length = 64)
	private String traceId;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "safe_message", length = 500)
	private String safeMessage;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> metadata;

	protected ProcessCheckpoint() {
	}

	public static ProcessCheckpoint success(
			BusinessProcess process,
			CheckpointStage stage,
			String areaCode,
			AppUser actor) {
		Instant now = Instant.now();
		ProcessCheckpoint checkpoint = new ProcessCheckpoint();
		checkpoint.id = UUID.randomUUID();
		checkpoint.process = process;
		checkpoint.stage = stage;
		checkpoint.areaCode = areaCode;
		checkpoint.status = CheckpointStatus.SUCCESS;
		checkpoint.startedAt = now;
		checkpoint.finishedAt = now;
		checkpoint.durationMs = 0L;
		checkpoint.actor = actor;
		checkpoint.metadata = Map.of();
		return checkpoint;
	}

	public static ProcessCheckpoint running(
			BusinessProcess process,
			CheckpointStage stage,
			String areaCode,
			AppUser actor) {
		ProcessCheckpoint checkpoint = new ProcessCheckpoint();
		checkpoint.id = UUID.randomUUID();
		checkpoint.process = process;
		checkpoint.stage = stage;
		checkpoint.areaCode = areaCode;
		checkpoint.status = CheckpointStatus.RUNNING;
		checkpoint.startedAt = Instant.now();
		checkpoint.actor = actor;
		checkpoint.metadata = Map.of();
		return checkpoint;
	}

	public static ProcessCheckpoint failure(
			BusinessProcess process,
			CheckpointStage stage,
			AppUser actor,
			String errorCode,
			String safeMessage) {
		Instant now = Instant.now();
		ProcessCheckpoint checkpoint = new ProcessCheckpoint();
		checkpoint.id = UUID.randomUUID();
		checkpoint.process = process;
		checkpoint.stage = stage;
		checkpoint.status = CheckpointStatus.FAILED;
		checkpoint.startedAt = now;
		checkpoint.finishedAt = now;
		checkpoint.durationMs = 0L;
		checkpoint.actor = actor;
		checkpoint.errorCode = errorCode;
		checkpoint.safeMessage = safeMessage;
		checkpoint.metadata = Map.of();
		return checkpoint;
	}

	public UUID getId() { return id; }
	public CheckpointStage getStage() { return stage; }
	public String getAreaCode() { return areaCode; }
	public CheckpointStatus getStatus() { return status; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getFinishedAt() { return finishedAt; }
	public Long getDurationMs() { return durationMs; }
	public AppUser getActor() { return actor; }
	public String getTraceId() { return traceId; }
	public String getErrorCode() { return errorCode; }
	public String getSafeMessage() { return safeMessage; }
}
