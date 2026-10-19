CREATE TABLE web_evaluation_session (
    id UUID PRIMARY KEY,
    session_code VARCHAR(80) NOT NULL,
    task_code VARCHAR(80) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_web_evaluation_expiry ON web_evaluation_session (expires_at);

CREATE TABLE web_evaluation_event (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES web_evaluation_session(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    metadata VARCHAR(120)
);

CREATE INDEX idx_web_evaluation_event_session ON web_evaluation_event (session_id, occurred_at);
