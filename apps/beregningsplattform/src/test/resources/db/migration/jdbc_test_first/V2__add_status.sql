ALTER TABLE migration_marker
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'created';
