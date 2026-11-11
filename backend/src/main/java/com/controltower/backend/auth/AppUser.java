package com.controltower.backend.auth;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(length = 80, nullable = false, unique = true)
	private String username;

	@Column(name = "password_hash", length = 100, nullable = false)
	private String passwordHash;

	@Column(name = "display_name", length = 120, nullable = false)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(length = 24, nullable = false)
	private UserRole role;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "user_area_permissions",
			joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "area_code", length = 16, nullable = false)
	private Set<String> areaCodes = new LinkedHashSet<>();

	protected AppUser() {
	}

	private AppUser(
			String username,
			String passwordHash,
			String displayName,
			UserRole role,
			Set<String> areaCodes) {
		this.id = UUID.randomUUID();
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
		this.active = true;
		this.createdAt = Instant.now();
		this.areaCodes.addAll(areaCodes);
	}

	static AppUser create(
			String username,
			String passwordHash,
			String displayName,
			UserRole role,
			Set<String> areaCodes) {
		return new AppUser(username, passwordHash, displayName, role, areaCodes);
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UserRole getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
	}

	public Set<String> getAreaCodes() {
		return Set.copyOf(areaCodes);
	}
}
