package com.controltower.backend.processes;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.common.error.RequestValidationException;
import com.controltower.backend.common.error.ResourceNotFoundException;

@Service
public class ProcessQueryService {
	private final BusinessProcessRepository processes;
	private final ProcessCheckpointRepository checkpoints;

	ProcessQueryService(BusinessProcessRepository processes, ProcessCheckpointRepository checkpoints) {
		this.processes = processes;
		this.checkpoints = checkpoints;
	}

	@Transactional(readOnly = true)
	public PageResponse<ProcessResponse> findAll(
			String status, String areaCode, int page, int size) {
		ProcessStatus parsedStatus = parseStatus(status);
		String normalizedArea = areaCode == null || areaCode.isBlank()
				? null : areaCode.strip().toUpperCase(Locale.ROOT);
		PageRequest request = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
				Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id")));
		Page<ProcessResponse> result = processes.findFiltered(parsedStatus, normalizedArea, request)
				.map(ProcessResponse::from);
		return PageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public ProcessResponse findById(UUID processId) {
		return ProcessResponse.from(processes.findById(processId)
				.orElseThrow(() -> new ResourceNotFoundException("Process")));
	}

	@Transactional(readOnly = true)
	public List<CheckpointResponse> timeline(UUID processId) {
		if (!processes.existsById(processId)) throw new ResourceNotFoundException("Process");
		return checkpoints.findByProcess_IdOrderByStartedAtAsc(processId).stream()
				.map(CheckpointResponse::from).toList();
	}

	private ProcessStatus parseStatus(String status) {
		if (status == null || status.isBlank()) return null;
		try {
			return ProcessStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException exception) {
			throw new RequestValidationException("Unknown process status");
		}
	}
}
