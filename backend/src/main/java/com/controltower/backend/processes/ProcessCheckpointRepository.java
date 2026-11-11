package com.controltower.backend.processes;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessCheckpointRepository extends JpaRepository<ProcessCheckpoint, UUID> {

	List<ProcessCheckpoint> findByProcess_IdOrderByStartedAtAsc(UUID processId);
}
