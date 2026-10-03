-- =========================================================================
-- EduFit Database Migration V1.06: Module Progress & Learning Plan
-- =========================================================================

-- 1. BẢNG GHI CHÚ BUỔI HỌC CỦA GIA SƯ (SESSION_NOTE)
CREATE TABLE IF NOT EXISTS session_note (
    note_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL,
    tutor_id        UUID NOT NULL,
    note_text       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_session_note_session UNIQUE (session_id),
    CONSTRAINT fk_session_note_session FOREIGN KEY (session_id) REFERENCES tutoring_session(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_note_tutor FOREIGN KEY (tutor_id) REFERENCES tutor_profile(tutor_id) ON DELETE CASCADE,
    CONSTRAINT chk_session_note_len CHECK (note_text IS NULL OR length(note_text) <= 5000)
);

CREATE INDEX IF NOT EXISTS idx_session_note_tutor ON session_note (tutor_id);

-- 2. BẢNG KẾ HOẠCH HỌC TẬP CỦA LỚP HỌC (LEARNING_PLAN)
CREATE TABLE IF NOT EXISTS learning_plan (
    plan_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id        UUID NOT NULL,
    summary         VARCHAR(1000),
    version         INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_learning_plan_class UNIQUE (class_id),
    CONSTRAINT fk_learning_plan_class FOREIGN KEY (class_id) REFERENCES class(class_id) ON DELETE CASCADE
);

-- 3. BẢNG CỘT MỐC HỌC TẬP (MILESTONE)
CREATE TABLE IF NOT EXISTS milestone (
    milestone_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    target_date     DATE,
    weight          SMALLINT NOT NULL DEFAULT 1,
    status          VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    achieved_date   DATE,
    is_archived     BOOLEAN NOT NULL DEFAULT false,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_milestone_plan FOREIGN KEY (plan_id) REFERENCES learning_plan(plan_id) ON DELETE CASCADE,
    CONSTRAINT chk_milestone_weight CHECK (weight BETWEEN 1 AND 5),
    CONSTRAINT chk_milestone_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'ACHIEVED'))
);

CREATE INDEX IF NOT EXISTS idx_milestone_plan ON milestone (plan_id, sort_order);

-- 4. BẢNG LỊCH SỬ THAY ĐỔI KẾ HOẠCH HỌC TẬP (PLAN_HISTORY)
CREATE TABLE IF NOT EXISTS plan_history (
    history_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID NOT NULL,
    milestone_id    UUID,
    action          VARCHAR(40) NOT NULL,
    actor_user_id   UUID NOT NULL,
    old_value       VARCHAR(500),
    new_value       VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_plan_hist_plan FOREIGN KEY (plan_id) REFERENCES learning_plan(plan_id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_hist_milestone FOREIGN KEY (milestone_id) REFERENCES milestone(milestone_id) ON DELETE SET NULL,
    CONSTRAINT fk_plan_hist_actor FOREIGN KEY (actor_user_id) REFERENCES account(user_id)
);

CREATE INDEX IF NOT EXISTS idx_plan_history_plan ON plan_history (plan_id, created_at);
