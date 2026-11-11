CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    validation_status VARCHAR(24) NOT NULL,
    CONSTRAINT uk_uploaded_files_stored_name UNIQUE (stored_name),
    CONSTRAINT fk_uploaded_files_user FOREIGN KEY (uploaded_by) REFERENCES app_users (id),
    CONSTRAINT ck_uploaded_files_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_uploaded_files_validation_status
        CHECK (validation_status IN ('VALIDATED', 'REJECTED'))
);

CREATE TABLE business_processes (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    created_by UUID NOT NULL,
    status VARCHAR(24) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    description VARCHAR(500),
    CONSTRAINT uk_business_processes_file UNIQUE (file_id),
    CONSTRAINT fk_business_processes_file FOREIGN KEY (file_id) REFERENCES uploaded_files (id),
    CONSTRAINT fk_business_processes_user FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT ck_business_processes_status
        CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE process_checkpoints (
    id UUID PRIMARY KEY,
    process_id UUID NOT NULL,
    stage VARCHAR(48) NOT NULL,
    area_code VARCHAR(16),
    status VARCHAR(24) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    actor_id UUID,
    trace_id VARCHAR(64),
    error_code VARCHAR(64),
    safe_message VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}',
    CONSTRAINT fk_process_checkpoints_process
        FOREIGN KEY (process_id) REFERENCES business_processes (id) ON DELETE CASCADE,
    CONSTRAINT fk_process_checkpoints_area
        FOREIGN KEY (area_code) REFERENCES production_areas (code),
    CONSTRAINT fk_process_checkpoints_actor
        FOREIGN KEY (actor_id) REFERENCES app_users (id),
    CONSTRAINT ck_process_checkpoints_duration CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT ck_process_checkpoints_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'WARNING', 'FAILED', 'TIMEOUT', 'CANCELLED'))
);

CREATE TABLE checkpoint_status_history (
    id UUID PRIMARY KEY,
    checkpoint_id UUID NOT NULL,
    from_status VARCHAR(24),
    to_status VARCHAR(24) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id UUID,
    CONSTRAINT fk_checkpoint_history_checkpoint
        FOREIGN KEY (checkpoint_id) REFERENCES process_checkpoints (id) ON DELETE CASCADE,
    CONSTRAINT fk_checkpoint_history_actor FOREIGN KEY (actor_id) REFERENCES app_users (id)
);

CREATE INDEX ix_uploaded_files_uploaded_at ON uploaded_files (uploaded_at);
CREATE INDEX ix_business_processes_status_started ON business_processes (status, started_at);
CREATE INDEX ix_process_checkpoints_process_started ON process_checkpoints (process_id, started_at);
CREATE INDEX ix_process_checkpoints_area ON process_checkpoints (area_code);
CREATE INDEX ix_checkpoint_history_checkpoint_changed ON checkpoint_status_history (checkpoint_id, changed_at);
