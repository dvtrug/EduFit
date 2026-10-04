-- =========================================================================
-- EduFit Database Migration V1.05: Module Scheduling
-- =========================================================================

-- 1. BẢNG BUỔI HỌC (TUTORING_SESSION)
CREATE TABLE IF NOT EXISTS tutoring_session (
    session_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id                UUID NOT NULL,
    tutor_id                UUID NOT NULL,
    student_id              UUID NOT NULL,
    start_at                TIMESTAMPTZ NOT NULL,
    end_at                  TIMESTAMPTZ NOT NULL,
    mode                    VARCHAR(20) NOT NULL,
    place_or_link           VARCHAR(500),
    repeat_note             VARCHAR(300),
    message                 VARCHAR(500),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PROPOSED',
    proposed_by             UUID NOT NULL,
    responded_by            UUID,
    responded_at            TIMESTAMPTZ,
    response_reason         VARCHAR(300),
    expires_at              TIMESTAMPTZ NOT NULL,
    cancelled_by            UUID,
    cancelled_at            TIMESTAMPTZ,
    cancel_reason           VARCHAR(40),
    cancel_comment          VARCHAR(300),
    is_late_cancel          BOOLEAN NOT NULL DEFAULT false,
    outcome_recorded_by     UUID,
    outcome_recorded_at     TIMESTAMPTZ,
    version                 INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_session_class FOREIGN KEY (class_id) REFERENCES class(class_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_tutor FOREIGN KEY (tutor_id) REFERENCES tutor_profile(tutor_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_proposed_by FOREIGN KEY (proposed_by) REFERENCES account(user_id),
    CONSTRAINT fk_session_responded_by FOREIGN KEY (responded_by) REFERENCES account(user_id),
    CONSTRAINT fk_session_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES account(user_id),
    CONSTRAINT fk_session_outcome_by FOREIGN KEY (outcome_recorded_by) REFERENCES account(user_id),
    CONSTRAINT chk_session_time CHECK (end_at > start_at),
    CONSTRAINT chk_session_mode CHECK (mode IN ('ONLINE', 'OFFLINE')),
    CONSTRAINT chk_session_status CHECK (status IN ('PROPOSED', 'SCHEDULED', 'COMPLETED', 'ABSENT', 'CANCELLED', 'REJECTED', 'WITHDRAWN', 'EXPIRED')),
    CONSTRAINT chk_session_cancel_reason CHECK (cancel_reason IS NULL OR cancel_reason IN ('SCHEDULE_CONFLICT', 'PERSONAL_REASON', 'OTHER_PARTY_UNAVAILABLE', 'OTHER')),

    -- Ràng buộc loại trừ (Exclusion Constraint): Chống trùng lịch dạy của Gia sư (NFR-17, BR-42)
    CONSTRAINT ex_tutoring_session_no_tutor_overlap EXCLUDE USING gist (
        tutor_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status = 'SCHEDULED'),

    -- Ràng buộc loại trừ (Exclusion Constraint): Chống trùng lịch học của Học viên
    CONSTRAINT ex_tutoring_session_no_student_overlap EXCLUDE USING gist (
        student_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status = 'SCHEDULED')
);

CREATE INDEX IF NOT EXISTS idx_tutoring_session_class_status ON tutoring_session (class_id, status);
CREATE INDEX IF NOT EXISTS idx_tutoring_session_tutor_start ON tutoring_session (tutor_id, start_at);
CREATE INDEX IF NOT EXISTS idx_tutoring_session_student_start ON tutoring_session (student_id, start_at);

-- 2. BẢNG ĐỀ XUẤT ĐỔI LỊCH BUỔI HỌC (SESSION_RESCHEDULE_PROPOSAL)
CREATE TABLE IF NOT EXISTS session_reschedule_proposal (
    proposal_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          UUID NOT NULL,
    proposed_by         UUID NOT NULL,
    new_start_at        TIMESTAMPTZ NOT NULL,
    new_end_at          TIMESTAMPTZ NOT NULL,
    reason              VARCHAR(300),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at          TIMESTAMPTZ NOT NULL,
    responded_by        UUID,
    responded_at        TIMESTAMPTZ,
    response_reason     VARCHAR(300),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reschedule_session FOREIGN KEY (session_id) REFERENCES tutoring_session(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_reschedule_proposed_by FOREIGN KEY (proposed_by) REFERENCES account(user_id),
    CONSTRAINT fk_reschedule_responded_by FOREIGN KEY (responded_by) REFERENCES account(user_id),
    CONSTRAINT chk_reschedule_time CHECK (new_end_at > new_start_at),
    CONSTRAINT chk_reschedule_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED'))
);

-- Chỉ cho phép tối đa 1 đề xuất đổi lịch PENDING cho mỗi buổi học tại 1 thời điểm
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_reschedule_pending 
    ON session_reschedule_proposal (session_id) 
    WHERE (status = 'PENDING');

CREATE INDEX IF NOT EXISTS idx_reschedule_session ON session_reschedule_proposal (session_id);

-- 3. BẢNG LỊCH SỬ THAY ĐỔI TRẠNG THÁI BUỔI HỌC (SESSION_HISTORY)
CREATE TABLE IF NOT EXISTS session_history (
    history_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id              UUID NOT NULL,
    reschedule_proposal_id  UUID,
    action                  VARCHAR(40) NOT NULL,
    actor_user_id           UUID,
    reason                  VARCHAR(300),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_session_hist_session FOREIGN KEY (session_id) REFERENCES tutoring_session(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_hist_proposal FOREIGN KEY (reschedule_proposal_id) REFERENCES session_reschedule_proposal(proposal_id) ON DELETE SET NULL,
    CONSTRAINT fk_session_hist_actor FOREIGN KEY (actor_user_id) REFERENCES account(user_id)
);

CREATE INDEX IF NOT EXISTS idx_session_history_session ON session_history (session_id, created_at);
