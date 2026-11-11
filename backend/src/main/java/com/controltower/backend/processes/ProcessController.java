package com.controltower.backend.processes;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessController {
	private final ProcessQueryService service;

	ProcessController(ProcessQueryService service) { this.service = service; }

	@GetMapping
	public PageResponse<ProcessResponse> findAll(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String areaCode,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.findAll(status, areaCode, page, size);
	}

	@GetMapping("/{processId}")
	public ProcessResponse findById(@PathVariable UUID processId) {
		return service.findById(processId);
	}

	@GetMapping("/{processId}/timeline")
	public List<CheckpointResponse> timeline(@PathVariable UUID processId) {
		return service.timeline(processId);
	}
}
