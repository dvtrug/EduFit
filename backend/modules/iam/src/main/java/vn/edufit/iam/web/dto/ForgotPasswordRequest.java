package vn.edufit.iam.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload yêu cầu đặt lại mật khẩu khi quên (UC1.3).
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email
) {}
