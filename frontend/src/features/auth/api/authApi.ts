import { fetchApi, ApiResponse } from "@/lib/api";
import type {
  AuthUser,
  UserRole,
  AccountStatus,
  RegisterInput,
  LoginInput,
  ForgotPasswordInput,
  ChangePasswordInput,
} from "../types";

export type { AuthUser, UserRole, AccountStatus };

export type RegisterPayload = RegisterInput;
export type LoginPayload = LoginInput;
export type ForgotPasswordPayload = ForgotPasswordInput;
export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}
export type ChangePasswordPayload = ChangePasswordInput;

export const authApi = {
  /**
   * Đăng ký tài khoản người dùng mới (UC1.1)
   */
  async register(payload: RegisterPayload): Promise<ApiResponse<AuthUser>> {
    return fetchApi<AuthUser>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  /**
   * Đăng nhập xác thực và thiết lập phiên EDUFIT_SESSION (UC1.2)
   */
  async login(payload: LoginPayload): Promise<ApiResponse<AuthUser>> {
    return fetchApi<AuthUser>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  /**
   * Đọc thông tin người dùng từ phiên đăng nhập hiện tại
   */
  async getMe(): Promise<ApiResponse<AuthUser>> {
    return fetchApi<AuthUser>("/api/v1/auth/me", {
      method: "GET",
    });
  },

  /**
   * Đăng xuất và thu hồi phiên làm việc trên server (UC1.3)
   */
  async logout(): Promise<ApiResponse<null>> {
    return fetchApi<null>("/api/v1/auth/logout", {
      method: "POST",
    });
  },

  /**
   * Yêu cầu gửi link đặt lại mật khẩu qua email (UC1.4)
   */
  async forgotPassword(payload: ForgotPasswordPayload): Promise<ApiResponse<null>> {
    return fetchApi<null>("/api/v1/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  /**
   * Đặt lại mật khẩu bằng mã token 30 phút (UC1.4)
   */
  async resetPassword(payload: ResetPasswordPayload): Promise<ApiResponse<null>> {
    return fetchApi<null>("/api/v1/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  /**
   * Đổi mật khẩu cho người dùng đang đăng nhập (UC1.4)
   */
  async changePassword(payload: ChangePasswordPayload): Promise<ApiResponse<null>> {
    return fetchApi<null>("/api/v1/auth/change-password", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};
