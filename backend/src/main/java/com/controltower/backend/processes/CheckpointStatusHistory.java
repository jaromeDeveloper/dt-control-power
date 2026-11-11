package com.controltower.backend.processes;

import java.time.Instant;
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

import com.controltower.backend.auth.AppUser;

@Entity
@Table(name = "checkpoint_status_history")
public class CheckpointStatusHistory {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "checkpoint_id", nullable = false)
	private ProcessCheckpoint checkpoint;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 24)
	private CheckpointStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", length = 24, nullable = false)
	private CheckpointStatus toStatus;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private AppUser actor;

	protected CheckpointStatusHistory() {
	}

	public CheckpointStatusHistory(ProcessCheckpoint checkpoint, AppUser actor) {
		this.id = UUID.randomUUID();
		this.checkpoint = checkpoint;
		this.toStatus = checkpoint.getStatus();
		this.changedAt = Instant.now();
		this.actor = actor;
	}
}
