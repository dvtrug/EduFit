import { fetchApi, ApiResponse } from "@/lib/api";

export type UserRole = "STUDENT" | "PARENT" | "TUTOR" | "ADMIN";
export type AccountStatus = "ACTIVE" | "DEACTIVATED";

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  status: AccountStatus;
  redirectUrl?: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  role: "STUDENT" | "PARENT" | "TUTOR";
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export const authService = {
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
   * Yêu cầu gửi link đặt lại mật khẩu qua email
   */
  async forgotPassword(payload: ForgotPasswordPayload): Promise<ApiResponse<null>> {
    return fetchApi<null>("/api/v1/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  /**
   * Đặt lại mật khẩu bằng mã token 30 phút
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
