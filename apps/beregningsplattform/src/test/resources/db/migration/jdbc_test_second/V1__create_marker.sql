CREATE TABLE migration_marker (
    id BIGSERIAL PRIMARY KEY,
    marker_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO migration_marker (marker_value)
VALUES ('second-marker');
