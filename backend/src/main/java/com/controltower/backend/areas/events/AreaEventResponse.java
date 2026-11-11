package com.controltower.backend.areas.events;

import java.time.Instant;
import java.util.UUID;

import com.controltower.backend.semaphores.SemaphoreColor;

public record AreaEventResponse(
		UUID eventId,
		UUID processId,
		String areaCode,
		AreaEventType eventType,
		AreaEventStatus status,
		UUID actorId,
		Instant occurredAt,
		String detail,
		String traceId,
		SemaphoreColor semaphore) {

	public static AreaEventResponse from(ProcessEvent event, SemaphoreColor semaphore) {
		return new AreaEventResponse(
				event.getId(),
				event.getProcess().getId(),
				event.getAreaCode(),
				event.getEventType(),
				event.getStatus(),
				event.getActor().getId(),
				event.getOccurredAt(),
				event.getDetail(),
				event.getTraceId(),
				semaphore);
	}
}
