package com.controltower.backend.dashboard;

import java.util.List;

import com.controltower.backend.semaphores.SemaphoreSummaryResponse;

public record DashboardSummaryResponse(
		long totalProcesses,
		long activeProcesses,
		long completedProcesses,
		long failedProcesses,
		long uploadedFiles,
		SemaphoreSummaryResponse semaphores,
		List<RecentEventResponse> recentEvents) {
}
