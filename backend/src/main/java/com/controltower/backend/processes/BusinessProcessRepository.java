package com.controltower.backend.processes;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessProcessRepository extends JpaRepository<BusinessProcess, UUID> {

	Optional<BusinessProcess> findByFile_Id(UUID fileId);

	long countByStatus(ProcessStatus status);

	long countByStatusIn(Collection<ProcessStatus> statuses);

	@Query("""
			select process from BusinessProcess process
			where (:status is null or process.status = :status)
			  and (:areaCode is null or exists (
			      select checkpoint.id from ProcessCheckpoint checkpoint
			      where checkpoint.process = process and checkpoint.areaCode = :areaCode
			  ))
			""")
	Page<BusinessProcess> findFiltered(
			@Param("status") ProcessStatus status,
			@Param("areaCode") String areaCode,
			Pageable pageable);
}
