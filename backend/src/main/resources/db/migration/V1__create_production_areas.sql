CREATE TABLE production_areas (
    code VARCHAR(16) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    warning_after_seconds INTEGER NOT NULL,
    timeout_after_seconds INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_production_areas_warning_positive
        CHECK (warning_after_seconds > 0),
    CONSTRAINT ck_production_areas_timeout_after_warning
        CHECK (timeout_after_seconds > warning_after_seconds)
);

INSERT INTO production_areas (
    code,
    name,
    warning_after_seconds,
    timeout_after_seconds,
    active
) VALUES
    ('PROD_A', 'Producción A', 60, 180, TRUE),
    ('PROD_B', 'Producción B', 60, 180, TRUE),
    ('PROD_C', 'Producción C', 60, 180, TRUE);
