package com.controltower.backend.areas.events;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.controltower.backend.auth.AppUserPrincipal;

@RestController
@RequestMapping("/api/v1/processes/{processId}")
public class AreaEventController {
	private final AreaEventService service;

	AreaEventController(AreaEventService service) {
		this.service = service;
	}

	@PostMapping("/areas/{areaCode}/events")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPERVISOR')")
	public AreaEventResponse create(
			@PathVariable UUID processId,
			@PathVariable String areaCode,
			@Valid @RequestBody CreateAreaEventRequest request,
			@RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return service.create(
				processId,
				areaCode,
				request.eventType(),
				request.status(),
				request.detail(),
				idempotencyKey,
				principal);
	}

	@GetMapping("/events")
	public List<AreaEventResponse> findByProcess(@PathVariable UUID processId) {
		return service.findByProcess(processId);
	}

	public record CreateAreaEventRequest(
			@NotNull AreaEventType eventType,
			@NotNull AreaEventStatus status,
			@Size(max = 500) String detail) {
	}
}
