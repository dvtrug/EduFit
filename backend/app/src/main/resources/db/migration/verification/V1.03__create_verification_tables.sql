-- =========================================================================
-- EduFit Database Migration V1.03: Module Verification
-- =========================================================================

-- 1. BẢNG YÊU CẦU XÁC MINH DANH TÍNH GIA SƯ (VERIFICATION_REQUEST)
CREATE TABLE IF NOT EXISTS verification_request (
    request_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tutor_id            UUID NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_by         UUID,
    reviewed_at         TIMESTAMPTZ,

    CONSTRAINT fk_verification_tutor FOREIGN KEY (tutor_id) REFERENCES tutor_profile(tutor_id) ON DELETE CASCADE,
    CONSTRAINT fk_verification_reviewer FOREIGN KEY (reviewed_by) REFERENCES account(user_id),
    CONSTRAINT chk_verification_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- Chỉ cho phép tối đa 1 yêu cầu PENDING tại một thời điểm cho mỗi gia sư (BR-15)
CREATE UNIQUE INDEX IF NOT EXISTS uq_verification_pending ON verification_request (tutor_id) WHERE (status = 'PENDING');
CREATE INDEX IF NOT EXISTS idx_verification_tutor ON verification_request (tutor_id);
CREATE INDEX IF NOT EXISTS idx_verification_status ON verification_request (status);

-- 2. BẢNG HỒ SƠ VĂN BẰNG / CHỨNG CHỈ (CREDENTIAL)
CREATE TABLE IF NOT EXISTS credential (
    credential_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id      UUID NOT NULL,
    type            VARCHAR(30) NOT NULL,
    institution     VARCHAR(200) NOT NULL,
    year            SMALLINT,
    note            VARCHAR(500),

    CONSTRAINT fk_credential_request FOREIGN KEY (request_id) REFERENCES verification_request(request_id) ON DELETE CASCADE,
    CONSTRAINT chk_credential_type CHECK (type IN ('DEGREE', 'CERTIFICATE', 'TEACHING_LICENCE', 'STUDENT_CARD', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_credential_request ON credential (request_id);

-- 3. BẢNG TỆP TIN ĐÍNH KÈM CỦA VĂN BẰNG (CREDENTIAL_FILE)
CREATE TABLE IF NOT EXISTS credential_file (
    file_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credential_id   UUID NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_credential_file_cred FOREIGN KEY (credential_id) REFERENCES credential(credential_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_credential_file_credential ON credential_file (credential_id);
