package com.controltower.backend.records;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.controltower.backend.auth.AppUserPrincipal;

@RestController
@RequestMapping("/api/v1/business-records")
public class BusinessRecordController {
	private final BusinessRecordService service;

	BusinessRecordController(BusinessRecordService service) { this.service = service; }

	@GetMapping
	public List<BusinessRecordResponse> findByProcess(@RequestParam UUID processId) {
		return service.findByProcess(processId);
	}

	@GetMapping("/{recordId}")
	public BusinessRecordResponse findById(@PathVariable UUID recordId) {
		return service.findById(recordId);
	}

	@PatchMapping("/{recordId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPERVISOR')")
	public BusinessRecordResponse update(@PathVariable UUID recordId,
			@Valid @RequestBody UpdateValueRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return service.update(recordId, request.value(), principal);
	}

	public record UpdateValueRequest(@NotBlank @Size(max = 200) String value) { }
}
