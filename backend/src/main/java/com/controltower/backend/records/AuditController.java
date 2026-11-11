package com.controltower.backend.records;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.controltower.backend.processes.PageResponse;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
	private final AuditService service;

	AuditController(AuditService service) { this.service = service; }

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'AUDITOR')")
	public PageResponse<AuditResponse> find(
			@RequestParam(required = false) UUID processId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.find(processId, page, size);
	}
}
