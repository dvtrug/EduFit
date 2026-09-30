package vn.edufit.iam.api.dto;

import java.util.UUID;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;

/**
 * DTO an toàn đại diện cho thông tin cơ bản của người dùng, phục vụ giao tiếp giữa các module.
 *
 * <p>Tuyệt đối không chứa các trường nhạy cảm như password_hash, token, hay thông tin lockout.
 */
public record UserSummaryView(
    UUID id,
    String email,
    String fullName,
    AccountRole role,
    AccountStatus status
) {}
