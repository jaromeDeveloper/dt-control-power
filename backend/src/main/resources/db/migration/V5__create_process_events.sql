CREATE TABLE process_events (
    id UUID PRIMARY KEY,
    process_id UUID NOT NULL,
    area_code VARCHAR(16) NOT NULL,
    event_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    actor_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail VARCHAR(500),
    trace_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    CONSTRAINT fk_process_events_process
        FOREIGN KEY (process_id) REFERENCES business_processes (id) ON DELETE CASCADE,
    CONSTRAINT fk_process_events_area
        FOREIGN KEY (area_code) REFERENCES production_areas (code),
    CONSTRAINT fk_process_events_actor
        FOREIGN KEY (actor_id) REFERENCES app_users (id),
    CONSTRAINT ck_process_events_type
        CHECK (event_type IN ('RECEIVED', 'STARTED', 'COMPLETED', 'WARNING', 'FAILED')),
    CONSTRAINT ck_process_events_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'WARNING', 'FAILED'))
);

CREATE INDEX ix_process_events_process_occurred
    ON process_events (process_id, occurred_at, id);
CREATE INDEX ix_process_events_process_area_occurred
    ON process_events (process_id, area_code, occurred_at, id);
CREATE UNIQUE INDEX uk_process_events_process_idempotency
    ON process_events (process_id, idempotency_key);
