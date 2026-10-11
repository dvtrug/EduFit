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

export type AuthFieldErrors<T> = Partial<Record<keyof T, string>>;

export type {
  RegisterInput,
  LoginInput,
  ForgotPasswordInput,
  ResetPasswordInput,
  ChangePasswordInput,
  PasswordCriteriaResult,
} from "../schemas/authSchemas";
