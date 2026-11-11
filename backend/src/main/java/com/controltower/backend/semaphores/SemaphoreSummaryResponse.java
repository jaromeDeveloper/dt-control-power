package com.controltower.backend.semaphores;

import java.util.List;

public record SemaphoreSummaryResponse(List<AreaSummary> areas) {

	public record AreaSummary(
			String areaCode,
			long active,
			long green,
			long yellow,
			long red,
			long gray) {
	}
}
