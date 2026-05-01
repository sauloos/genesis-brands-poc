CREATE TABLE conversation_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_id    UUID NOT NULL REFERENCES brands(id),
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE conversation_turns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES conversation_sessions(id),
    sequence_num    INT NOT NULL,
    question        TEXT NOT NULL,
    answer          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_turns_session ON conversation_turns (session_id, sequence_num);
