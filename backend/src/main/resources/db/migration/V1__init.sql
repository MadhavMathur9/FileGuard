CREATE TABLE watched_folder (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(1024) NOT NULL UNIQUE,
    recursive BOOLEAN NOT NULL DEFAULT TRUE,
    mode VARCHAR(50) NOT NULL, -- DEDUP, INTEGRITY, BOTH
    exclude_globs TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE folder_rule (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES watched_folder(id) ON DELETE CASCADE,
    trigger VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE file_entry (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES watched_folder(id) ON DELETE CASCADE,
    path VARCHAR(1024) NOT NULL,
    size BIGINT NOT NULL,
    mtime TIMESTAMP WITH TIME ZONE NOT NULL,
    partial_hash VARCHAR(64),
    full_hash VARCHAR(64),
    status VARCHAR(50) NOT NULL,
    first_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(folder_id, path)
);
CREATE INDEX idx_file_entry_size ON file_entry(size);
CREATE INDEX idx_file_entry_full_hash ON file_entry(full_hash);

CREATE TABLE duplicate_group (
    id BIGSERIAL PRIMARY KEY,
    full_hash VARCHAR(64) NOT NULL UNIQUE,
    original_file_id BIGINT REFERENCES file_entry(id) ON DELETE SET NULL,
    member_count INT NOT NULL DEFAULT 1,
    wasted_bytes BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE baseline_entry (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES watched_folder(id) ON DELETE CASCADE,
    path VARCHAR(1024) NOT NULL,
    full_hash VARCHAR(64) NOT NULL,
    size BIGINT NOT NULL,
    mtime TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE integrity_alert (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES watched_folder(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    path VARCHAR(1024) NOT NULL,
    old_hash VARCHAR(64),
    new_hash VARCHAR(64),
    state VARCHAR(50) NOT NULL,
    raised_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE quarantine_item (
    id BIGSERIAL PRIMARY KEY,
    file_entry_id BIGINT NOT NULL REFERENCES file_entry(id) ON DELETE CASCADE,
    original_path VARCHAR(1024) NOT NULL,
    quarantine_path VARCHAR(1024) NOT NULL,
    full_hash VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    quarantined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    restored_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    ts TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_path VARCHAR(1024) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    result VARCHAR(50) NOT NULL,
    dry_run BOOLEAN NOT NULL DEFAULT FALSE
);
