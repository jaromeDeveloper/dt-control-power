package com.controltower.backend.records;

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
import com.controltower.backend.processes.BusinessProcess;

@Entity
@Table(name = "business_record_audit")
public class BusinessRecordAudit {
	@Id private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false)
	private BusinessRecord record;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "process_id", nullable = false)
	private BusinessProcess process;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false)
	private AppUser actor;
	@Column(name = "old_value", length = 200) private String oldValue;
	@Column(name = "new_value", length = 200) private String newValue;
	@Enumerated(EnumType.STRING)
	@Column(length = 24, nullable = false) private AuditOutcome outcome;
	@Column(name = "occurred_at", nullable = false) private Instant occurredAt;
	@Column(name = "request_id", length = 36, nullable = false) private String requestId;

	protected BusinessRecordAudit() { }

	public BusinessRecordAudit(BusinessRecord record, AppUser actor,
			String oldValue, String newValue, String requestId) {
		this(record, actor, oldValue, newValue, requestId, AuditOutcome.SUCCESS);
	}

	public static BusinessRecordAudit failed(BusinessRecord record, AppUser actor,
			String attemptedValue, String requestId) {
		return new BusinessRecordAudit(
				record, actor, record.getValue(), attemptedValue, requestId, AuditOutcome.FAILED);
	}

	private BusinessRecordAudit(BusinessRecord record, AppUser actor,
			String oldValue, String newValue, String requestId, AuditOutcome outcome) {
		this.id = UUID.randomUUID();
		this.record = record;
		this.process = record.getProcess();
		this.actor = actor;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.outcome = outcome;
		this.occurredAt = Instant.now();
		this.requestId = requestId;
	}

	public UUID getId() { return id; }
	public BusinessRecord getRecord() { return record; }
	public BusinessProcess getProcess() { return process; }
	public AppUser getActor() { return actor; }
	public String getOldValue() { return oldValue; }
	public String getNewValue() { return newValue; }
	public AuditOutcome getOutcome() { return outcome; }
	public Instant getOccurredAt() { return occurredAt; }
	public String getRequestId() { return requestId; }
}
