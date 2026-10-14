-- Durable BR-25 admission. The actor row serializes short quota transactions only.
CREATE TABLE ai_match_quota_actor (
    user_id UUID PRIMARY KEY REFERENCES account(user_id) ON DELETE CASCADE
);

CREATE TABLE ai_match_reservation (
    reservation_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES account(user_id) ON DELETE CASCADE,
    reserved_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    finished_at TIMESTAMPTZ,
    CONSTRAINT chk_ai_match_reservation_status
        CHECK (status IN ('RESERVED', 'SUCCESS', 'ERROR', 'TIMEOUT', 'EXPIRED')),
    CONSTRAINT chk_ai_match_reservation_lease CHECK (expires_at > reserved_at),
    CONSTRAINT chk_ai_match_reservation_terminal
        CHECK ((status = 'RESERVED' AND finished_at IS NULL)
            OR (status <> 'RESERVED' AND finished_at IS NOT NULL))
);

CREATE INDEX idx_ai_match_reservation_window ON ai_match_reservation(user_id, reserved_at);
CREATE INDEX idx_ai_match_reservation_pending ON ai_match_reservation(user_id, expires_at)
    WHERE status = 'RESERVED';

-- Preserve existing attempts, including failed/time-out calls. No double count with audit logs.
INSERT INTO ai_match_quota_actor(user_id)
SELECT DISTINCT user_id FROM ai_request_log WHERE feature = 'MATCH_EXPLANATION';

INSERT INTO ai_match_reservation(reservation_id, user_id, reserved_at, expires_at, status, finished_at)
SELECT log_id, user_id, created_at, created_at + INTERVAL '45 seconds', status, created_at
FROM ai_request_log WHERE feature = 'MATCH_EXPLANATION';
