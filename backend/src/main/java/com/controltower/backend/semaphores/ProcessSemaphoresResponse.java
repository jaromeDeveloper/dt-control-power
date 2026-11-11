package com.controltower.backend.semaphores;

import java.util.List;
import java.util.UUID;

public record ProcessSemaphoresResponse(UUID processId, List<AreaSemaphoreResponse> areas) {
}
