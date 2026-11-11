package com.controltower.backend.semaphores;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/semaphores")
public class SemaphoreController {
	private final SemaphoreService service;

	SemaphoreController(SemaphoreService service) {
		this.service = service;
	}

	@GetMapping
	public ProcessSemaphoresResponse findByProcess(@RequestParam UUID processId) {
		return service.findByProcess(processId);
	}

	@GetMapping("/summary")
	public SemaphoreSummaryResponse summary() {
		return service.summary();
	}
}
