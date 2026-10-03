-- =========================================================================
-- EduFit Database Migration V1.04: Module Connection & Class (Engagement)
-- =========================================================================

-- 1. BẢNG LỜI MỜI LIÊN KẾT PHỤ HUYNH - HỌC VIÊN (LINK_INVITATION)
CREATE TABLE IF NOT EXISTS link_invitation (
    invitation_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inviter_user_id     UUID NOT NULL,
    invited_user_id     UUID NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ NOT NULL,
    responded_at        TIMESTAMPTZ,
    reinvite_after      TIMESTAMPTZ,

    CONSTRAINT fk_invitation_inviter FOREIGN KEY (inviter_user_id) REFERENCES account(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_invited FOREIGN KEY (invited_user_id) REFERENCES account(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_invitation_different_users CHECK (inviter_user_id <> invited_user_id),
    CONSTRAINT chk_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN', 'EXPIRED'))
);

-- Chỉ cho phép tối đa 1 lời mời PENDING giữa hai người dùng bất kể ai là người gửi (BR-29)
CREATE UNIQUE INDEX IF NOT EXISTS uq_link_invitation_pending 
    ON link_invitation (LEAST(inviter_user_id, invited_user_id), GREATEST(inviter_user_id, invited_user_id)) 
    WHERE (status = 'PENDING');

CREATE INDEX IF NOT EXISTS idx_link_invitation_inviter ON link_invitation (inviter_user_id);
CREATE INDEX IF NOT EXISTS idx_link_invitation_invited ON link_invitation (invited_user_id);

-- 2. BẢNG LIÊN KẾT PHỤ HUYNH - HỌC VIÊN CHÍNH THỨC (PARENT_STUDENT_LINK)
CREATE TABLE IF NOT EXISTS parent_student_link (
    link_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_user_id  UUID NOT NULL,
    student_id      UUID NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    confirmed_at    TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    revoked_by      UUID,
    revoke_reason   TEXT,

    CONSTRAINT uk_parent_student UNIQUE (parent_user_id, student_id),
    CONSTRAINT fk_pslink_parent FOREIGN KEY (parent_user_id) REFERENCES account(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pslink_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_pslink_revoked_by FOREIGN KEY (revoked_by) REFERENCES account(user_id),
    CONSTRAINT chk_pslink_status CHECK (status IN ('CONFIRMED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_parent_student_parent ON parent_student_link (parent_user_id);
CREATE INDEX IF NOT EXISTS idx_parent_student_student ON parent_student_link (student_id);

-- 3. BẢNG YÊU CẦU KẾT NỐI HỌC TẬP (CONNECTION_REQUEST)
CREATE TABLE IF NOT EXISTS connection_request (
    request_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL,
    tutor_id            UUID NOT NULL,
    goal_id             UUID NOT NULL,
    sent_by_user_id     UUID NOT NULL,
    desired_slots       JSONB,
    message             VARCHAR(500),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reject_reason       VARCHAR(300),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ NOT NULL,
    responded_at        TIMESTAMPTZ,

    CONSTRAINT fk_conn_req_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_conn_req_tutor FOREIGN KEY (tutor_id) REFERENCES tutor_profile(tutor_id) ON DELETE CASCADE,
    CONSTRAINT fk_conn_req_goal FOREIGN KEY (goal_id) REFERENCES learning_goal(goal_id) ON DELETE CASCADE,
    CONSTRAINT fk_conn_req_sender FOREIGN KEY (sent_by_user_id) REFERENCES account(user_id),
    CONSTRAINT chk_conn_req_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED'))
);

-- Không cho phép gửi trùng yêu cầu kết nối đang ở trạng thái PENDING cho cùng 1 cặp học viên - gia sư (BR-35)
CREATE UNIQUE INDEX IF NOT EXISTS uq_connection_request_pending 
    ON connection_request (student_id, tutor_id) 
    WHERE (status = 'PENDING');

CREATE INDEX IF NOT EXISTS idx_connection_request_student ON connection_request (student_id);
CREATE INDEX IF NOT EXISTS idx_connection_request_tutor ON connection_request (tutor_id);

-- 4. BẢNG LỚP HỌC 1-1 / HỢP ĐỒNG GIẢNG DẠY (CLASS == ENGAGEMENT)
CREATE TABLE IF NOT EXISTS class (
    class_id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_request_id   UUID NOT NULL,
    student_id              UUID NOT NULL,
    tutor_id                UUID NOT NULL,
    goal_id                 UUID NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    started_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at                TIMESTAMPTZ,
    end_reason              VARCHAR(300),

    CONSTRAINT uk_class_connection_request UNIQUE (connection_request_id),
    CONSTRAINT fk_class_connection_request FOREIGN KEY (connection_request_id) REFERENCES connection_request(request_id),
    CONSTRAINT fk_class_student FOREIGN KEY (student_id) REFERENCES student_profile(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_class_tutor FOREIGN KEY (tutor_id) REFERENCES tutor_profile(tutor_id) ON DELETE CASCADE,
    CONSTRAINT fk_class_goal FOREIGN KEY (goal_id) REFERENCES learning_goal(goal_id) ON DELETE CASCADE,
    CONSTRAINT chk_class_status CHECK (status IN ('ACTIVE', 'ENDED'))
);

-- Chỉ cho phép 1 lớp học ACTIVE tại một thời điểm giữa 1 học viên và 1 gia sư
CREATE UNIQUE INDEX IF NOT EXISTS uq_class_student_tutor_active 
    ON class (student_id, tutor_id) 
    WHERE (status = 'ACTIVE');

CREATE INDEX IF NOT EXISTS idx_class_student ON class (student_id);
CREATE INDEX IF NOT EXISTS idx_class_tutor ON class (tutor_id);
