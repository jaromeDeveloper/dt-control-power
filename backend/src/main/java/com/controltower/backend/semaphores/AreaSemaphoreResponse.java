package com.controltower.backend.semaphores;

import java.time.Instant;

public record AreaSemaphoreResponse(
		String areaCode,
		SemaphoreColor color,
		SemaphoreStatus status,
		Instant lastEventAt) {
}
