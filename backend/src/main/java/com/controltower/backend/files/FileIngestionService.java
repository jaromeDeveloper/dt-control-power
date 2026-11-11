package com.controltower.backend.files;

import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.common.error.FileValidationException;
import com.controltower.backend.common.error.ResourceNotFoundException;
import com.controltower.backend.files.FileStorageService.StoredFile;
import com.controltower.backend.processes.BusinessProcess;
import com.controltower.backend.processes.BusinessProcessRepository;
import com.controltower.backend.processes.ProcessCreationService;
import com.controltower.backend.processes.ProcessResponse;

@Service
public class FileIngestionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileIngestionService.class);
	private final FileStorageService storage;
	private final ProcessCreationService processCreation;
	private final UploadedFileRepository files;
	private final BusinessProcessRepository processes;

	FileIngestionService(FileStorageService storage, ProcessCreationService processCreation,
			UploadedFileRepository files, BusinessProcessRepository processes) {
		this.storage = storage;
		this.processCreation = processCreation;
		this.files = files;
		this.processes = processes;
	}

	public FileResponse upload(MultipartFile file, String areaCode, String description, AppUserPrincipal principal) {
		String normalizedDescription = normalizeDescription(description);
		String normalizedArea = areaCode == null || areaCode.isBlank()
				? null : areaCode.strip().toUpperCase(Locale.ROOT);
		StoredFile stored = storage.store(file);
		try {
			ProcessResponse process = processCreation.create(stored, normalizedArea, normalizedDescription, principal);
			LOGGER.info(
					"business.file_ingested processId={} fileId={} status={} checkpointStages=FILE_RECEIVED,FILE_VALIDATED",
					process.processId(), process.file().fileId(), process.status());
			return process.file();
		}
		catch (RuntimeException exception) {
			storage.deleteQuietly(stored.storagePath());
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public FileResponse findById(UUID fileId) {
		UploadedFile file = files.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File"));
		BusinessProcess process = processes.findByFile_Id(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("Process"));
		return FileResponse.from(file, process.getId());
	}

	private String normalizeDescription(String description) {
		if (description == null || description.isBlank()) return null;
		String value = description.strip();
		if (value.length() > 500) throw new FileValidationException("Description cannot exceed 500 characters");
		return value;
	}
}
