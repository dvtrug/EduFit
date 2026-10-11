import { authApi } from "@/features/auth/api/authApi";

export { authApi, authApi as authService } from "@/features/auth/api/authApi";
export type {
  RegisterPayload,
  LoginPayload,
  ForgotPasswordPayload,
  ResetPasswordPayload,
  ChangePasswordPayload,
} from "@/features/auth/api/authApi";

export type {
  AuthUser,
  UserRole,
  AccountStatus,
} from "@/features/auth/types";

export default authApi;
