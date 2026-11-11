package com.controltower.backend.processes;

public enum CheckpointStatus {
	PENDING,
	RUNNING,
	SUCCESS,
	WARNING,
	FAILED,
	TIMEOUT,
	CANCELLED
}
