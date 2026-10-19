CREATE TABLE content_item (
    id UUID PRIMARY KEY,
    content_type VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500) NOT NULL,
    summary VARCHAR(3000),
    image_url VARCHAR(1500),
    canonical_url VARCHAR(2000) NOT NULL,
    canonical_url_hash VARCHAR(64) NOT NULL,
    author VARCHAR(300),
    language VARCHAR(10) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    primary_source VARCHAR(20) NOT NULL,
    source_score DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_content_item_url_hash ON content_item (canonical_url_hash);
CREATE INDEX idx_content_item_published_at ON content_item (published_at DESC);
CREATE INDEX idx_content_item_normalized_title ON content_item (normalized_title);

CREATE TABLE content_item_category (
    content_item_id UUID NOT NULL REFERENCES content_item(id) ON DELETE CASCADE,
    category VARCHAR(80) NOT NULL,
    PRIMARY KEY (content_item_id, category)
);

CREATE TABLE content_item_tag (
    content_item_id UUID NOT NULL REFERENCES content_item(id) ON DELETE CASCADE,
    tag VARCHAR(80) NOT NULL,
    PRIMARY KEY (content_item_id, tag)
);

CREATE TABLE content_occurrence (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL REFERENCES content_item(id) ON DELETE CASCADE,
    source_code VARCHAR(20) NOT NULL,
    external_id VARCHAR(500) NOT NULL,
    external_url VARCHAR(2000) NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_occurrence_source_external UNIQUE (source_code, external_id)
);

CREATE INDEX idx_occurrence_content_item ON content_occurrence (content_item_id);

CREATE TABLE source_refresh_state (
    source_code VARCHAR(20) PRIMARY KEY,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    health VARCHAR(30) NOT NULL,
    last_error VARCHAR(1000),
    last_item_count INTEGER NOT NULL DEFAULT 0
);
