package vn.edufit.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload đổi mật khẩu cho người dùng đã đăng nhập (UC1.4).
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    String newPassword
) {}
