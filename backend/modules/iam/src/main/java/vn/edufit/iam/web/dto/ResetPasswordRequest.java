package vn.edufit.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload cập nhật mật khẩu mới qua token đặt lại (UC1.3).
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Mã token không được để trống")
    String token,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    String newPassword
) {}
