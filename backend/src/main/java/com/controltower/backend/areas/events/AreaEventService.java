package com.controltower.backend.areas.events;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.controltower.backend.areas.AreaService;
import com.controltower.backend.areas.AreaService.AreaResponse;
import com.controltower.backend.auth.AppUser;
import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.common.error.InvalidTransitionException;
import com.controltower.backend.common.error.RequestValidationException;
import com.controltower.backend.common.error.ResourceNotFoundException;
import com.controltower.backend.processes.BusinessProcess;
import com.controltower.backend.processes.BusinessProcessRepository;
import com.controltower.backend.processes.CheckpointService;
import com.controltower.backend.processes.CheckpointStage;
import com.controltower.backend.semaphores.SemaphoreService;

@Service
public class AreaEventService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AreaEventService.class);
	private static final int MAX_DETAIL_LENGTH = 500;
	private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;
	private static final Map<AreaEventType, AreaEventStatus> REQUIRED_STATUSES = requiredStatuses();

	private final EntityManager entityManager;
	private final AreaService areas;
	private final BusinessProcessRepository processes;
	private final ProcessEventRepository events;
	private final CheckpointService checkpoints;
	private final SemaphoreService semaphores;

	AreaEventService(
			EntityManager entityManager,
			AreaService areas,
			BusinessProcessRepository processes,
			ProcessEventRepository events,
			CheckpointService checkpoints,
			SemaphoreService semaphores) {
		this.entityManager = entityManager;
		this.areas = areas;
		this.processes = processes;
		this.events = events;
		this.checkpoints = checkpoints;
		this.semaphores = semaphores;
	}

	@Transactional
	public AreaEventResponse create(
			UUID processId,
			String areaCode,
			AreaEventType eventType,
			AreaEventStatus status,
			String detail,
			String idempotencyKey,
			AppUserPrincipal principal) {
		BusinessProcess process = processes.findById(processId)
				.orElseThrow(() -> new ResourceNotFoundException("Business process"));
		AreaResponse area = areas.validateEventWriteAccess(areaCode, principal);
		validateEvent(eventType, status);
		String safeDetail = normalizeDetail(detail);
		String key = normalizeIdempotencyKey(idempotencyKey);

		if (key != null) {
			ProcessEvent existing = events.findByProcess_IdAndIdempotencyKey(processId, key).orElse(null);
			if (existing != null) {
				validateIdempotentReplay(existing, areaCode, eventType, status, safeDetail);
				return toResponse(existing);
			}
		}

		List<ProcessEvent> history = events.findByProcess_IdOrderByOccurredAtAscIdAsc(processId);
		Map<String, ProcessEvent> latest = latestByArea(history);
		validateSequence(areaCode, latest);
		validateTransition(latest.get(areaCode), eventType);

		AppUser actor = entityManager.getReference(AppUser.class, principal.id());
		ProcessEvent event = events.save(new ProcessEvent(
				process, area.code(), eventType, status, actor, safeDetail, key));
		checkpoints.recordSuccess(process, CheckpointStage.AREA_EVENT_RECORDED, area.code(), actor);
		latest.put(area.code(), event);
		updateProcessStatus(process, event, latest, actor);
		logAfterCommit(event);
		return toResponse(event);
	}

	private void logAfterCommit(ProcessEvent event) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				LOGGER.info(
						"business.area_event_recorded processId={} eventId={} areaCode={} eventType={} status={} checkpointStage=AREA_EVENT_RECORDED",
						event.getProcess().getId(), event.getId(), event.getAreaCode(),
						event.getEventType(), event.getStatus());
			}
		});
	}

	private void updateProcessStatus(
			BusinessProcess process,
			ProcessEvent event,
			Map<String, ProcessEvent> latest,
			AppUser actor) {
		if (event.getEventType() == AreaEventType.FAILED) {
			process.markFailed();
			checkpoints.recordFailure(
					process,
					CheckpointStage.PROCESS_FAILED,
					actor,
					"AREA_EVENT_FAILED",
					"A production area reported a terminal failure");
			return;
		}
		if (event.getEventType() == AreaEventType.COMPLETED
				&& areas.findActiveAreas().stream()
						.allMatch(area -> isCompleted(latest.get(area.code())))) {
			process.markCompleted();
			checkpoints.recordSuccess(process, CheckpointStage.PROCESS_COMPLETED, null, actor);
		}
	}

	@Transactional(readOnly = true)
	public List<AreaEventResponse> findByProcess(UUID processId) {
		if (!processes.existsById(processId)) {
			throw new ResourceNotFoundException("Business process");
		}
		return events.findByProcess_IdOrderByOccurredAtAscIdAsc(processId).stream()
				.map(this::toResponse)
				.toList();
	}

	private AreaEventResponse toResponse(ProcessEvent event) {
		return AreaEventResponse.from(event, semaphores.colorForEvent(event));
	}

	private void validateEvent(AreaEventType eventType, AreaEventStatus status) {
		if (eventType == null || status == null) {
			throw new RequestValidationException("Event type and status are required");
		}
		if (REQUIRED_STATUSES.get(eventType) != status) {
			throw new RequestValidationException("The event status does not match the event type");
		}
	}

	private void validateSequence(String areaCode, Map<String, ProcessEvent> latest) {
		List<AreaResponse> configured = areas.findActiveAreas();
		int targetIndex = -1;
		for (int index = 0; index < configured.size(); index++) {
			if (configured.get(index).code().equals(areaCode)) {
				targetIndex = index;
				break;
			}
		}
		if (targetIndex < 0) {
			throw new RequestValidationException("The production area is not active");
		}
		for (int index = 0; index < targetIndex; index++) {
			ProcessEvent previous = latest.get(configured.get(index).code());
			if (!isCompleted(previous)) {
				throw new InvalidTransitionException("Previous production areas must be completed first");
			}
		}
	}

	private void validateTransition(ProcessEvent previous, AreaEventType requested) {
		AreaEventType current = previous == null ? null : previous.getEventType();
		boolean allowed = switch (requested) {
			case RECEIVED -> current == null;
			case STARTED -> current == null || current == AreaEventType.RECEIVED;
			case WARNING -> current == AreaEventType.RECEIVED || current == AreaEventType.STARTED;
			case COMPLETED -> current == AreaEventType.STARTED || current == AreaEventType.WARNING;
			case FAILED -> current == AreaEventType.RECEIVED
					|| current == AreaEventType.STARTED
					|| current == AreaEventType.WARNING;
		};
		if (!allowed) {
			throw new InvalidTransitionException("The requested area event is not valid for its current state");
		}
	}

	private boolean isCompleted(ProcessEvent event) {
		return event != null
				&& event.getEventType() == AreaEventType.COMPLETED
				&& event.getStatus() == AreaEventStatus.SUCCESS;
	}

	private Map<String, ProcessEvent> latestByArea(List<ProcessEvent> history) {
		return history.stream().collect(Collectors.toMap(
				ProcessEvent::getAreaCode,
				Function.identity(),
				(previous, replacement) -> replacement));
	}

	private void validateIdempotentReplay(
			ProcessEvent existing,
			String areaCode,
			AreaEventType eventType,
			AreaEventStatus status,
			String detail) {
		if (!existing.getAreaCode().equals(areaCode)
				|| existing.getEventType() != eventType
				|| existing.getStatus() != status
				|| !Objects.equals(existing.getDetail(), detail)) {
			throw new InvalidTransitionException("The idempotency key was already used for a different request");
		}
	}

	private String normalizeDetail(String detail) {
		if (detail == null || detail.isBlank()) {
			return null;
		}
		String normalized = detail.strip();
		if (normalized.length() > MAX_DETAIL_LENGTH) {
			throw new RequestValidationException("Detail cannot exceed 500 characters");
		}
		return normalized;
	}

	private String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return null;
		}
		String normalized = idempotencyKey.strip();
		if (normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
			throw new RequestValidationException("Idempotency key cannot exceed 128 characters");
		}
		return normalized;
	}

	private static Map<AreaEventType, AreaEventStatus> requiredStatuses() {
		Map<AreaEventType, AreaEventStatus> statuses = new EnumMap<>(AreaEventType.class);
		statuses.put(AreaEventType.RECEIVED, AreaEventStatus.PENDING);
		statuses.put(AreaEventType.STARTED, AreaEventStatus.RUNNING);
		statuses.put(AreaEventType.COMPLETED, AreaEventStatus.SUCCESS);
		statuses.put(AreaEventType.WARNING, AreaEventStatus.WARNING);
		statuses.put(AreaEventType.FAILED, AreaEventStatus.FAILED);
		return Map.copyOf(statuses);
	}
}
