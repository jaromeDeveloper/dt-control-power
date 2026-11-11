CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(24) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT ck_app_users_role
        CHECK (role IN ('ADMIN', 'OPERATOR', 'SUPERVISOR', 'AUDITOR'))
);

CREATE TABLE user_area_permissions (
    user_id UUID NOT NULL,
    area_code VARCHAR(16) NOT NULL,
    CONSTRAINT pk_user_area_permissions PRIMARY KEY (user_id, area_code),
    CONSTRAINT fk_user_area_permissions_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_area_permissions_area
        FOREIGN KEY (area_code) REFERENCES production_areas (code)
);

CREATE INDEX ix_app_users_active_role ON app_users (active, role);
