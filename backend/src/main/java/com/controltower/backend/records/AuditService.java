package com.controltower.backend.records;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.processes.PageResponse;

@Service
public class AuditService {
	private final BusinessRecordAuditRepository audit;

	AuditService(BusinessRecordAuditRepository audit) { this.audit = audit; }

	@Transactional(readOnly = true)
	public PageResponse<AuditResponse> find(UUID processId, int page, int size) {
		PageRequest request = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
				Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id")));
		Page<BusinessRecordAudit> result = processId == null
				? audit.findAll(request)
				: audit.findByProcess_Id(processId, request);
		return PageResponse.from(result.map(AuditResponse::from));
	}
}
