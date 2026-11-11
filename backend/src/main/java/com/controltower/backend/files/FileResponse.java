package com.controltower.backend.files;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
		UUID fileId,
		UUID processId,
		String name,
		long sizeBytes,
		String contentType,
		String sha256,
		String uploadedBy,
		Instant uploadedAt,
		FileValidationStatus status) {

	public static FileResponse from(UploadedFile file, UUID processId) {
		return new FileResponse(
				file.getId(), processId, file.getOriginalName(), file.getSizeBytes(),
				file.getMimeType(), file.getSha256(), file.getUploadedBy().getUsername(),
				file.getUploadedAt(), file.getValidationStatus());
	}
}
