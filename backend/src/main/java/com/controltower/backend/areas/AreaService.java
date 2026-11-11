package com.controltower.backend.areas;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.controltower.backend.auth.AppUserPrincipal;
import com.controltower.backend.auth.UserRole;
import com.controltower.backend.common.error.FileValidationException;
import com.controltower.backend.common.error.ForbiddenOperationException;
import com.controltower.backend.common.error.RequestValidationException;

@Service
public class AreaService {

	private final ProductionAreaRepository repository;

	AreaService(ProductionAreaRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<AreaResponse> findActiveAreas() {
		return repository.findByActiveTrueOrderByCodeAsc().stream()
				.map(AreaResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public void validateUploadAccess(String areaCode, AppUserPrincipal principal) {
		if (principal.role() == UserRole.AUDITOR) {
			throw new ForbiddenOperationException("The auditor role has read-only access");
		}
		if (areaCode == null) {
			return;
		}

		ProductionArea area = repository.findById(areaCode)
				.filter(ProductionArea::isActive)
				.orElseThrow(() -> new FileValidationException("The selected production area is not active"));
		if (principal.role() == UserRole.OPERATOR && !principal.areaCodes().contains(area.getCode())) {
			throw new ForbiddenOperationException("The selected production area is not assigned to this operator");
		}
	}

	@Transactional(readOnly = true)
	public AreaResponse validateEventWriteAccess(String areaCode, AppUserPrincipal principal) {
		if (principal.role() == UserRole.AUDITOR) {
			throw new ForbiddenOperationException("The auditor role has read-only access");
		}
		ProductionArea area = repository.findById(areaCode)
				.filter(ProductionArea::isActive)
				.orElseThrow(() -> new RequestValidationException("The production area is not active"));
		if (principal.role() == UserRole.OPERATOR && !principal.areaCodes().contains(area.getCode())) {
			throw new ForbiddenOperationException("The production area is not assigned to this operator");
		}
		return AreaResponse.from(area);
	}

	public record AreaResponse(
			String code,
			String name,
			int warningAfterSeconds,
			int timeoutAfterSeconds) {

		static AreaResponse from(ProductionArea area) {
			return new AreaResponse(
					area.getCode(),
					area.getName(),
					area.getWarningAfterSeconds(),
					area.getTimeoutAfterSeconds());
		}
	}
}
