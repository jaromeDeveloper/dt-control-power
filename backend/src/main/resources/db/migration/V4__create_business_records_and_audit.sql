CREATE TABLE business_records (
    id UUID PRIMARY KEY,
    process_id UUID NOT NULL,
    reference VARCHAR(80) NOT NULL,
    record_value VARCHAR(200) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_business_records_process_reference UNIQUE (process_id, reference),
    CONSTRAINT fk_business_records_process
        FOREIGN KEY (process_id) REFERENCES business_processes (id) ON DELETE CASCADE,
    CONSTRAINT fk_business_records_user FOREIGN KEY (updated_by) REFERENCES app_users (id)
);

CREATE TABLE business_record_audit (
    id UUID PRIMARY KEY,
    record_id UUID NOT NULL,
    process_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    old_value VARCHAR(200),
    new_value VARCHAR(200),
    outcome VARCHAR(24) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_business_record_audit_record
        FOREIGN KEY (record_id) REFERENCES business_records (id) ON DELETE CASCADE,
    CONSTRAINT fk_business_record_audit_process
        FOREIGN KEY (process_id) REFERENCES business_processes (id) ON DELETE CASCADE,
    CONSTRAINT fk_business_record_audit_actor FOREIGN KEY (actor_id) REFERENCES app_users (id),
    CONSTRAINT ck_business_record_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX ix_business_records_process ON business_records (process_id);
CREATE INDEX ix_business_record_audit_record_occurred ON business_record_audit (record_id, occurred_at);
CREATE INDEX ix_business_record_audit_process_occurred ON business_record_audit (process_id, occurred_at);
