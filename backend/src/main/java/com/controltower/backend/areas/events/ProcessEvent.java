package com.controltower.backend.areas.events;

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
@Table(name = "process_events")
public class ProcessEvent {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "process_id", nullable = false)
	private BusinessProcess process;

	@Column(name = "area_code", length = 16, nullable = false)
	private String areaCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", length = 24, nullable = false)
	private AreaEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(length = 24, nullable = false)
	private AreaEventStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false)
	private AppUser actor;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(length = 500)
	private String detail;

	@Column(name = "trace_id", length = 64)
	private String traceId;

	@Column(name = "idempotency_key", length = 128)
	private String idempotencyKey;

	protected ProcessEvent() {
	}

	public ProcessEvent(
			BusinessProcess process,
			String areaCode,
			AreaEventType eventType,
			AreaEventStatus status,
			AppUser actor,
			String detail,
			String idempotencyKey) {
		this.id = UUID.randomUUID();
		this.process = process;
		this.areaCode = areaCode;
		this.eventType = eventType;
		this.status = status;
		this.actor = actor;
		this.occurredAt = Instant.now();
		this.detail = detail;
		this.idempotencyKey = idempotencyKey;
	}

	public UUID getId() { return id; }
	public BusinessProcess getProcess() { return process; }
	public String getAreaCode() { return areaCode; }
	public AreaEventType getEventType() { return eventType; }
	public AreaEventStatus getStatus() { return status; }
	public AppUser getActor() { return actor; }
	public Instant getOccurredAt() { return occurredAt; }
	public String getDetail() { return detail; }
	public String getTraceId() { return traceId; }
}
