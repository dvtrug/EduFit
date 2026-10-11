ALTER TABLE parent_student_link DROP CONSTRAINT uk_parent_student;
CREATE UNIQUE INDEX uq_parent_student_confirmed
    ON parent_student_link (parent_user_id, student_id)
    WHERE status = 'CONFIRMED';

ALTER TABLE link_invitation ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE parent_student_link ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE connection_request ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE connection_invite_quota (
    user_id UUID PRIMARY KEY REFERENCES account(user_id) ON DELETE CASCADE,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempts INT NOT NULL
);
