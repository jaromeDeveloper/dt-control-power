package com.controltower.backend.processes;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CheckpointStatusHistoryRepository extends JpaRepository<CheckpointStatusHistory, UUID> {
}
