package com.controltower.backend.areas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_areas")
public class ProductionArea {

	@Id
	@Column(length = 16, nullable = false, updatable = false)
	private String code;

	@Column(length = 80, nullable = false)
	private String name;

	@Column(name = "warning_after_seconds", nullable = false)
	private int warningAfterSeconds;

	@Column(name = "timeout_after_seconds", nullable = false)
	private int timeoutAfterSeconds;

	@Column(nullable = false)
	private boolean active;

	protected ProductionArea() {
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getWarningAfterSeconds() {
		return warningAfterSeconds;
	}

	public int getTimeoutAfterSeconds() {
		return timeoutAfterSeconds;
	}

	public boolean isActive() {
		return active;
	}
}
