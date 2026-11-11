package com.controltower.backend.dashboard;

import java.time.Instant;
import java.util.UUID;

import com.controltower.backend.areas.events.AreaEventStatus;
import com.controltower.backend.areas.events.AreaEventType;
import com.controltower.backend.areas.events.ProcessEvent;

public record RecentEventResponse(
		UUID processId,
		String areaCode,
		AreaEventType eventType,
		AreaEventStatus status,
		String actor,
		Instant occurredAt) {

	static RecentEventResponse from(ProcessEvent event) {
		return new RecentEventResponse(
				event.getProcess().getId(),
				event.getAreaCode(),
				event.getEventType(),
				event.getStatus(),
				event.getActor().getUsername(),
				event.getOccurredAt());
	}
}
