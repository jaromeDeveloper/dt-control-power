package com.controltower.backend.files;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.controltower.backend.common.error.FileTooLargeException;
import com.controltower.backend.common.error.FileValidationException;

@Service
public class FileStorageService {

	private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
	private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
			"csv", Set.of("text/csv", "application/vnd.ms-excel"),
			"txt", Set.of("text/plain"),
			"json", Set.of("application/json", "text/json"),
			"pdf", Set.of("application/pdf"));

	private final Path storageRoot;

	FileStorageService(@Value("${app.file-storage.path}") String storagePath) {
		this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
	}

	public StoredFile store(MultipartFile file) {
		ValidatedFile validated = validate(file);
		String storedName = UUID.randomUUID() + "." + validated.extension();
		Path target = storageRoot.resolve(storedName).normalize();
		if (!target.startsWith(storageRoot)) {
			throw new FileValidationException("Invalid storage target");
		}

		Path temporary = null;
		try {
			Files.createDirectories(storageRoot);
			temporary = Files.createTempFile(storageRoot, ".upload-", ".tmp");
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			long copied = copyWithLimit(file, temporary, digest);
			moveIntoPlace(temporary, target);
			return new StoredFile(
					validated.originalName(), storedName, copied, validated.contentType(),
					HexFormat.of().formatHex(digest.digest()), target.toString());
		}
		catch (FileTooLargeException exception) {
			deleteQuietly(temporary);
			throw exception;
		}
		catch (IOException | NoSuchAlgorithmException exception) {
			deleteQuietly(temporary);
			throw new FileStorageException("The file could not be stored", exception);
		}
	}

	public void deleteQuietly(String storagePath) {
		if (storagePath != null) {
			Path candidate = Path.of(storagePath).toAbsolutePath().normalize();
			if (candidate.startsWith(storageRoot)) {
				deleteQuietly(candidate);
			}
		}
	}

	private ValidatedFile validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new FileValidationException("A non-empty file is required");
		}
		if (file.getSize() > MAX_SIZE_BYTES) {
			throw new FileTooLargeException();
		}

		String originalName;
		try {
			originalName = Path.of(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
					.getFileName().toString();
		}
		catch (InvalidPathException exception) {
			throw new FileValidationException("The file name is invalid");
		}
		int separator = originalName.lastIndexOf('.');
		if (separator <= 0 || separator == originalName.length() - 1) {
			throw new FileValidationException("The file extension is required");
		}
		if (originalName.length() > 255) {
			throw new FileValidationException("The file name cannot exceed 255 characters");
		}

		String extension = originalName.substring(separator + 1).toLowerCase(Locale.ROOT);
		String contentType = file.getContentType();
		Set<String> acceptedTypes = ALLOWED_TYPES.get(extension);
		if (acceptedTypes == null || contentType == null || !acceptedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new FileValidationException("Only CSV, TXT, JSON and PDF files are accepted");
		}
		return new ValidatedFile(originalName, extension, contentType.toLowerCase(Locale.ROOT));
	}

	private long copyWithLimit(MultipartFile source, Path target, MessageDigest digest) throws IOException {
		long total = 0;
		byte[] buffer = new byte[16 * 1024];
		try (InputStream input = new DigestInputStream(source.getInputStream(), digest);
				var output = Files.newOutputStream(target)) {
			int read;
			while ((read = input.read(buffer)) >= 0) {
				total += read;
				if (total > MAX_SIZE_BYTES) {
					throw new FileTooLargeException();
				}
				output.write(buffer, 0, read);
			}
		}
		return total;
	}

	private void moveIntoPlace(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, target);
		}
	}

	private void deleteQuietly(Path path) {
		if (path == null) return;
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException ignored) {
			// Best effort cleanup; the original failure remains the actionable error.
		}
	}

	private record ValidatedFile(String originalName, String extension, String contentType) {
	}

	public record StoredFile(
			String originalName,
			String storedName,
			long sizeBytes,
			String contentType,
			String sha256,
			String storagePath) {
	}

	public static class FileStorageException extends RuntimeException {
		FileStorageException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
