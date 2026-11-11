package com.controltower.backend.records;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import com.controltower.backend.auth.AppUser;
import com.controltower.backend.processes.BusinessProcess;

@Entity
@Table(name = "business_records")
public class BusinessRecord {
	@Id private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "process_id", nullable = false)
	private BusinessProcess process;
	@Column(length = 80, nullable = false) private String reference;
	@Column(name = "record_value", length = 200, nullable = false) private String value;
	@Version private long version;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "updated_by", nullable = false)
	private AppUser updatedBy;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;

	protected BusinessRecord() { }

	public BusinessRecord(BusinessProcess process, String reference, String value, AppUser actor) {
		this.id = UUID.randomUUID();
		this.process = process;
		this.reference = reference;
		this.value = value;
		this.updatedBy = actor;
		this.updatedAt = Instant.now();
	}

	public void updateValue(String value, AppUser actor) {
		this.value = value;
		this.updatedBy = actor;
		this.updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public BusinessProcess getProcess() { return process; }
	public String getReference() { return reference; }
	public String getValue() { return value; }
	public long getVersion() { return version; }
	public AppUser getUpdatedBy() { return updatedBy; }
	public Instant getUpdatedAt() { return updatedAt; }
}
