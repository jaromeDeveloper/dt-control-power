package com.controltower.backend.files;

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
@Table(name = "uploaded_files")
public class UploadedFile {

	@Id
	private UUID id;

	@Column(name = "original_name", length = 255, nullable = false)
	private String originalName;

	@Column(name = "stored_name", length = 255, nullable = false, unique = true)
	private String storedName;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "mime_type", length = 120, nullable = false)
	private String mimeType;

	@Column(length = 64, nullable = false)
	private String sha256;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_by", nullable = false)
	private AppUser uploadedBy;

	@Column(name = "uploaded_at", nullable = false)
	private Instant uploadedAt;

	@Column(name = "storage_path", length = 500, nullable = false)
	private String storagePath;

	@Enumerated(EnumType.STRING)
	@Column(name = "validation_status", length = 24, nullable = false)
	private FileValidationStatus validationStatus;

	protected UploadedFile() {
	}

	public UploadedFile(
			String originalName,
			String storedName,
			long sizeBytes,
			String mimeType,
			String sha256,
			AppUser uploadedBy,
			String storagePath) {
		this.id = UUID.randomUUID();
		this.originalName = originalName;
		this.storedName = storedName;
		this.sizeBytes = sizeBytes;
		this.mimeType = mimeType;
		this.sha256 = sha256;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = Instant.now();
		this.storagePath = storagePath;
		this.validationStatus = FileValidationStatus.VALIDATED;
	}

	public UUID getId() { return id; }
	public String getOriginalName() { return originalName; }
	public long getSizeBytes() { return sizeBytes; }
	public String getMimeType() { return mimeType; }
	public String getSha256() { return sha256; }
	public AppUser getUploadedBy() { return uploadedBy; }
	public Instant getUploadedAt() { return uploadedAt; }
	public FileValidationStatus getValidationStatus() { return validationStatus; }
}
