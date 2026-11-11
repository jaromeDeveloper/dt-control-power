package com.controltower.backend.dashboard;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.controltower.backend.areas.events.ProcessEventRepository;
import com.controltower.backend.files.UploadedFileRepository;
import com.controltower.backend.processes.BusinessProcessRepository;
import com.controltower.backend.processes.ProcessStatus;
import com.controltower.backend.semaphores.SemaphoreService;
import com.controltower.backend.semaphores.SemaphoreSummaryResponse;

@Service
public class DashboardService {
	private static final EnumSet<ProcessStatus> ACTIVE_STATUSES =
			EnumSet.of(ProcessStatus.CREATED, ProcessStatus.IN_PROGRESS);

	private final BusinessProcessRepository processes;
	private final UploadedFileRepository files;
	private final ProcessEventRepository events;
	private final SemaphoreService semaphores;

	DashboardService(
			BusinessProcessRepository processes,
			UploadedFileRepository files,
			ProcessEventRepository events,
			SemaphoreService semaphores) {
		this.processes = processes;
		this.files = files;
		this.events = events;
		this.semaphores = semaphores;
	}

	@Transactional(readOnly = true)
	public DashboardSummaryResponse summary() {
		long totalProcesses = processes.count();
		long activeProcesses = processes.countByStatusIn(ACTIVE_STATUSES);
		long completedProcesses = processes.countByStatus(ProcessStatus.COMPLETED);
		long failedProcesses = processes.countByStatus(ProcessStatus.FAILED);
		long uploadedFiles = files.count();
		SemaphoreSummaryResponse semaphoreSummary = semaphores.summary();
		List<RecentEventResponse> recentEvents = events.findTop10ByOrderByOccurredAtDescIdDesc().stream()
				.map(RecentEventResponse::from)
				.toList();

		return new DashboardSummaryResponse(
				totalProcesses,
				activeProcesses,
				completedProcesses,
				failedProcesses,
				uploadedFiles,
				semaphoreSummary,
				recentEvents);
	}
}
