package com.controltower.backend.processes;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.areas.AreaService;
import com.controltower.backend.auth.AppUser;
import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.files.FileStorageService.StoredFile;
import com.controltower.backend.files.UploadedFile;
import com.controltower.backend.files.UploadedFileRepository;
import com.controltower.backend.records.BusinessRecordInitializationService;

@Service
public class ProcessCreationService {
	private final EntityManager entityManager;
	private final AreaService areas;
	private final UploadedFileRepository files;
	private final BusinessProcessRepository processes;
	private final CheckpointService checkpointService;
	private final BusinessRecordInitializationService recordInitialization;

	ProcessCreationService(EntityManager entityManager, AreaService areas, UploadedFileRepository files,
			BusinessProcessRepository processes, CheckpointService checkpointService,
			BusinessRecordInitializationService recordInitialization) {
		this.entityManager = entityManager;
		this.areas = areas;
		this.files = files;
		this.processes = processes;
		this.checkpointService = checkpointService;
		this.recordInitialization = recordInitialization;
	}

	@Transactional
	public ProcessResponse create(StoredFile stored, String areaCode, String description, AppUserPrincipal principal) {
		areas.validateUploadAccess(areaCode, principal);
		AppUser actor = entityManager.getReference(AppUser.class, principal.id());
		UploadedFile file = files.save(new UploadedFile(stored.originalName(), stored.storedName(),
				stored.sizeBytes(), stored.contentType(), stored.sha256(), actor, stored.storagePath()));
		BusinessProcess process = processes.save(new BusinessProcess(file, actor, description));
		recordInitialization.initialize(process, actor);
		createCheckpoint(process, CheckpointStage.FILE_RECEIVED, areaCode, actor);
		createCheckpoint(process, CheckpointStage.FILE_VALIDATED, areaCode, actor);
		return ProcessResponse.from(process);
	}

	private void createCheckpoint(BusinessProcess process, CheckpointStage stage, String areaCode, AppUser actor) {
		checkpointService.recordSuccess(process, stage, areaCode, actor);
	}
}
