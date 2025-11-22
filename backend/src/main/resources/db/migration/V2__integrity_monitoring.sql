CREATE TABLE IF NOT EXISTS baseline_entry (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES watched_folder(id) ON DELETE CASCADE,
    path VARCHAR(1024) NOT NULL,
    full_hash VARCHAR(64),
    size BIGINT NOT NULL,
    mtime TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(folder_id, path)
);

CREATE TABLE IF NOT EXISTS integrity_alert (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES watched_folder(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    path VARCHAR(1024) NOT NULL,
    old_hash VARCHAR(64),
    new_hash VARCHAR(64),
    state VARCHAR(30) NOT NULL,
    raised_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);
