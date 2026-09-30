package vn.edufit.iam.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.edufit.iam.api.AccountRole;

/**
 * Payload yêu cầu đăng ký tài khoản người dùng mới (UC1.1).
 */
public record RegisterRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email,

    @NotBlank(message = "Mật khẩu không được để trống")
    String password,

    @NotBlank(message = "Họ và tên không được để trống")
    String fullName,

    @NotNull(message = "Vai trò người dùng không được để trống")
    AccountRole role
) {}
