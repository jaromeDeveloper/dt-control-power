package com.controltower.backend.records;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.common.error.RequestValidationException;
import com.controltower.backend.common.error.ResourceNotFoundException;
import com.controltower.backend.observability.RequestCorrelation;

@Service
public class BusinessRecordService {
	private static final Logger LOGGER = LoggerFactory.getLogger(BusinessRecordService.class);
	private final BusinessRecordRepository records;
	private final BusinessRecordUpdateTransaction updateTransaction;
	private final BusinessUpdateFailureRecorder failureRecorder;

	BusinessRecordService(BusinessRecordRepository records,
			BusinessRecordUpdateTransaction updateTransaction,
			BusinessUpdateFailureRecorder failureRecorder) {
		this.records = records;
		this.updateTransaction = updateTransaction;
		this.failureRecorder = failureRecorder;
	}

	@Transactional(readOnly = true)
	public List<BusinessRecordResponse> findByProcess(UUID processId) {
		return records.findByProcess_IdOrderByReferenceAsc(processId).stream()
				.map(BusinessRecordResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public BusinessRecordResponse findById(UUID id) {
		return BusinessRecordResponse.from(records.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Business record")));
	}

	public BusinessRecordResponse update(UUID id, String requestedValue, AppUserPrincipal principal) {
		String value = validateValue(requestedValue);
		String requestId = RequestCorrelation.currentRequestId();
		try {
			BusinessRecordResponse response = updateTransaction.update(id, value, requestId, principal);
			LOGGER.info(
					"business.record_updated processId={} recordId={} version={} checkpointStage=BUSINESS_UPDATE_COMPLETED",
					response.processId(), response.id(), response.version());
			return response;
		}
		catch (RuntimeException exception) {
			try {
				failureRecorder.record(id, value, requestId, principal);
				LOGGER.warn(
						"business.record_update_failed recordId={} errorType={}",
						id, exception.getClass().getSimpleName());
			}
			catch (RuntimeException recordingFailure) {
				LOGGER.warn(
						"business.record_failure_audit_persist_failed recordId={} errorType={}",
						id, recordingFailure.getClass().getSimpleName());
			}
			throw exception;
		}
	}

	private String validateValue(String requestedValue) {
		if (requestedValue == null || requestedValue.isBlank()) {
			throw new RequestValidationException("Value is required");
		}
		String value = requestedValue.strip();
		if (value.length() > 200) throw new RequestValidationException("Value cannot exceed 200 characters");
		return value;
	}
}
