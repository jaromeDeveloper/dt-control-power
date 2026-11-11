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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.controltower.backend.auth.AppUser;
import com.controltower.backend.files.UploadedFile;

@Entity
@Table(name = "business_processes")
public class BusinessProcess {

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "file_id", nullable = false, unique = true)
	private UploadedFile file;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private AppUser createdBy;

	@Enumerated(EnumType.STRING)
	@Column(length = 24, nullable = false)
	private ProcessStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(length = 500)
	private String description;

	protected BusinessProcess() {
	}

	public BusinessProcess(UploadedFile file, AppUser createdBy, String description) {
		this.id = UUID.randomUUID();
		this.file = file;
		this.createdBy = createdBy;
		this.status = ProcessStatus.IN_PROGRESS;
		this.startedAt = Instant.now();
		this.description = description;
	}

	public void markCompleted() {
		if (status != ProcessStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only an active process can be completed");
		}
		status = ProcessStatus.COMPLETED;
		finishedAt = Instant.now();
	}

	public void markFailed() {
		if (status != ProcessStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only an active process can fail");
		}
		status = ProcessStatus.FAILED;
		finishedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UploadedFile getFile() { return file; }
	public AppUser getCreatedBy() { return createdBy; }
	public ProcessStatus getStatus() { return status; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getFinishedAt() { return finishedAt; }
	public String getDescription() { return description; }
}
