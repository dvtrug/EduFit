-- =========================================================================
-- EduFit Database Migration V1.08: Module Platform (Audit, AI & Matching Logs)
-- =========================================================================

-- 1. BẢNG NHẬT KÝ HÀNH ĐỘNG HỆ THỐNG / QUẢN TRỊ VIÊN (AUDIT_LOG)
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   UUID NOT NULL,
    action          VARCHAR(60) NOT NULL,
    target_type     VARCHAR(40) NOT NULL,
    target_id       UUID NOT NULL,
    reason          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES account(user_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_log (actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_log (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log (created_at);

-- 2. BẢNG NHẬT KÝ GỌI DỊCH VỤ TRÍ TUỆ NHÂN TẠO (AI_REQUEST_LOG)
CREATE TABLE IF NOT EXISTS ai_request_log (
    log_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    feature         VARCHAR(40) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    latency_ms      INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ai_log_user FOREIGN KEY (user_id) REFERENCES account(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_log_feature CHECK (feature IN ('TUTOR_BIO', 'MATCH_EXPLANATION')),
    CONSTRAINT chk_ai_log_status CHECK (status IN ('SUCCESS', 'ERROR', 'TIMEOUT'))
);

CREATE INDEX IF NOT EXISTS idx_ai_log_user ON ai_request_log (user_id, created_at);

-- 3. BẢNG NHẬT KÝ TÌM KIẾM & GHÉP ĐÔI GIA SƯ (MATCHING_RUN_LOG)
CREATE TABLE IF NOT EXISTS matching_run_log (
    run_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    student_id      UUID NOT NULL,
    goal_id         UUID NOT NULL,
    result_count    INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_matching_log_user FOREIGN KEY (user_id) REFERENCES account(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_matching_log_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_matching_log_goal FOREIGN KEY (goal_id) REFERENCES learning_goal(goal_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_matching_log_student ON matching_run_log (student_id, created_at);
CREATE INDEX IF NOT EXISTS idx_matching_log_created ON matching_run_log (created_at);
