package com.controltower.backend.areas.events;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ProcessEventRepository extends JpaRepository<ProcessEvent, UUID> {

	List<ProcessEvent> findByProcess_IdOrderByOccurredAtAscIdAsc(UUID processId);

	Optional<ProcessEvent> findByProcess_IdAndIdempotencyKey(UUID processId, String idempotencyKey);

	List<ProcessEvent> findAllByOrderByOccurredAtAscIdAsc();

	@EntityGraph(attributePaths = {"actor", "process"})
	List<ProcessEvent> findTop10ByOrderByOccurredAtDescIdDesc();
}
