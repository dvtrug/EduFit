-- =========================================================================
-- EduFit Database Migration V1.07: Module Review & Notification
-- =========================================================================

-- 1. BẢNG ĐÁNH GIÁ GIA SƯ (REVIEW)
CREATE TABLE IF NOT EXISTS review (
    review_id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tutor_id                        UUID NOT NULL,
    student_id                      UUID NOT NULL,
    reviewer_user_id                UUID NOT NULL,
    reviewer_type                   VARCHAR(20) NOT NULL,
    rating                          SMALLINT NOT NULL,
    comment                         TEXT NOT NULL,
    is_edited                       BOOLEAN NOT NULL DEFAULT false,
    completed_sessions_at_review    INT NOT NULL,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_review_tutor_student UNIQUE (tutor_id, student_id),
    CONSTRAINT fk_review_tutor FOREIGN KEY (tutor_id) REFERENCES tutor_profile(tutor_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES account(user_id),
    CONSTRAINT chk_review_reviewer_type CHECK (reviewer_type IN ('STUDENT', 'PARENT')),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_comment_len CHECK (length(comment) >= 10 AND length(comment) <= 1000)
);

CREATE INDEX IF NOT EXISTS idx_review_tutor ON review (tutor_id);
CREATE INDEX IF NOT EXISTS idx_review_student ON review (student_id);

-- 2. BẢNG THÔNG BÁO NGƯỜI DÙNG (NOTIFICATION)
CREATE TABLE IF NOT EXISTS notification (
    notification_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    about_student_id    UUID,
    type                VARCHAR(60) NOT NULL,
    title               VARCHAR(200) NOT NULL,
    short_text          VARCHAR(500) NOT NULL,
    target_type         VARCHAR(40),
    target_id           UUID,
    is_read             BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES account(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_student FOREIGN KEY (about_student_id) REFERENCES student_profile(student_id) ON DELETE SET NULL
);

-- Tối ưu truy vấn danh sách thông báo chưa đọc và phân trang thông báo của người dùng (BR-61, BR-62)
CREATE INDEX IF NOT EXISTS idx_notification_user_unread ON notification (user_id, is_read, created_at);
