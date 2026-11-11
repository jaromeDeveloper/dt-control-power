package com.controltower.backend.semaphores;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.controltower.backend.areas.AreaService;
import com.controltower.backend.areas.AreaService.AreaResponse;
import com.controltower.backend.areas.events.AreaEventStatus;
import com.controltower.backend.areas.events.AreaEventType;
import com.controltower.backend.areas.events.ProcessEvent;
import com.controltower.backend.areas.events.ProcessEventRepository;
import com.controltower.backend.common.error.ResourceNotFoundException;
import com.controltower.backend.processes.BusinessProcess;
import com.controltower.backend.processes.BusinessProcessRepository;
import com.controltower.backend.semaphores.SemaphoreSummaryResponse.AreaSummary;

@Service
public class SemaphoreService {
	private final AreaService areas;
	private final BusinessProcessRepository processes;
	private final ProcessEventRepository events;

	SemaphoreService(
			AreaService areas,
			BusinessProcessRepository processes,
			ProcessEventRepository events) {
		this.areas = areas;
		this.processes = processes;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public ProcessSemaphoresResponse findByProcess(UUID processId) {
		if (!processes.existsById(processId)) {
			throw new ResourceNotFoundException("Business process");
		}
		return new ProcessSemaphoresResponse(processId, currentAreas(processId));
	}

	@Transactional(readOnly = true)
	public SemaphoreSummaryResponse summary() {
		List<AreaResponse> configuredAreas = areas.findActiveAreas();
		List<BusinessProcess> allProcesses = processes.findAll();
		Map<ProcessAreaKey, ProcessEvent> latest = latestByProcessAndArea(
				events.findAllByOrderByOccurredAtAscIdAsc());
		Instant now = Instant.now();

		List<AreaSummary> summaries = configuredAreas.stream()
				.map(area -> summarize(area, allProcesses, latest, now))
				.toList();
		return new SemaphoreSummaryResponse(summaries);
	}

	public SemaphoreColor colorForEvent(ProcessEvent event) {
		if (event.getEventType() == AreaEventType.FAILED || event.getStatus() == AreaEventStatus.FAILED) {
			return SemaphoreColor.RED;
		}
		if (event.getEventType() == AreaEventType.COMPLETED || event.getStatus() == AreaEventStatus.SUCCESS) {
			return SemaphoreColor.GREEN;
		}
		if (event.getEventType() == AreaEventType.RECEIVED || event.getStatus() == AreaEventStatus.PENDING) {
			return SemaphoreColor.GRAY;
		}
		return SemaphoreColor.YELLOW;
	}

	private List<AreaSemaphoreResponse> currentAreas(UUID processId) {
		Map<String, ProcessEvent> latest = events.findByProcess_IdOrderByOccurredAtAscIdAsc(processId)
				.stream()
				.collect(Collectors.toMap(
						ProcessEvent::getAreaCode,
						Function.identity(),
						(previous, replacement) -> replacement));
		Instant now = Instant.now();
		return areas.findActiveAreas().stream()
				.map(area -> evaluate(area, latest.get(area.code()), now))
				.toList();
	}

	private AreaSummary summarize(
			AreaResponse area,
			List<BusinessProcess> allProcesses,
			Map<ProcessAreaKey, ProcessEvent> latest,
			Instant now) {
		long green = 0;
		long yellow = 0;
		long red = 0;
		long gray = 0;
		for (BusinessProcess process : allProcesses) {
			SemaphoreColor color = evaluate(
					area, latest.get(new ProcessAreaKey(process.getId(), area.code())), now).color();
			switch (color) {
				case GREEN -> green++;
				case YELLOW -> yellow++;
				case RED -> red++;
				case GRAY -> gray++;
			}
		}
		return new AreaSummary(area.code(), yellow + gray, green, yellow, red, gray);
	}

	private Map<ProcessAreaKey, ProcessEvent> latestByProcessAndArea(List<ProcessEvent> allEvents) {
		Map<ProcessAreaKey, ProcessEvent> latest = new HashMap<>();
		for (ProcessEvent event : allEvents) {
			latest.put(new ProcessAreaKey(event.getProcess().getId(), event.getAreaCode()), event);
		}
		return latest;
	}

	private AreaSemaphoreResponse evaluate(AreaResponse area, ProcessEvent event, Instant now) {
		if (event == null) {
			return new AreaSemaphoreResponse(area.code(), SemaphoreColor.GRAY, SemaphoreStatus.PENDING, null);
		}
		if (event.getEventType() == AreaEventType.FAILED || event.getStatus() == AreaEventStatus.FAILED) {
			return response(event, SemaphoreColor.RED, SemaphoreStatus.FAILED);
		}
		if (event.getEventType() == AreaEventType.COMPLETED || event.getStatus() == AreaEventStatus.SUCCESS) {
			return response(event, SemaphoreColor.GREEN, SemaphoreStatus.COMPLETED);
		}
		if (event.getEventType() == AreaEventType.RECEIVED || event.getStatus() == AreaEventStatus.PENDING) {
			return response(event, SemaphoreColor.GRAY, SemaphoreStatus.PENDING);
		}

		long elapsed = Math.max(0, Duration.between(event.getOccurredAt(), now).getSeconds());
		if (elapsed >= area.timeoutAfterSeconds()) {
			return response(event, SemaphoreColor.RED, SemaphoreStatus.TIMEOUT);
		}
		if (event.getEventType() == AreaEventType.WARNING
				|| event.getStatus() == AreaEventStatus.WARNING
				|| elapsed >= area.warningAfterSeconds()) {
			return response(event, SemaphoreColor.YELLOW, SemaphoreStatus.WARNING);
		}
		return response(event, SemaphoreColor.YELLOW, SemaphoreStatus.RUNNING);
	}

	private AreaSemaphoreResponse response(
			ProcessEvent event, SemaphoreColor color, SemaphoreStatus status) {
		return new AreaSemaphoreResponse(event.getAreaCode(), color, status, event.getOccurredAt());
	}

	private record ProcessAreaKey(UUID processId, String areaCode) {
	}
}
