// API Gateway
export { authApi, authApi as authService } from "./api/authApi";
export type {
  RegisterPayload,
  LoginPayload,
  ForgotPasswordPayload,
  ResetPasswordPayload,
  ChangePasswordPayload,
} from "./api/authApi";

// Components
export { LoginForm } from "./components/LoginForm";
export { RegisterForm } from "./components/RegisterForm";
export { ForgotPasswordForm } from "./components/ForgotPasswordForm";
export { ResetPasswordForm } from "./components/ResetPasswordForm";
export { QuickLoginPills } from "./components/QuickLoginPills";
export { RoleSelector, type RoleType } from "./components/RoleSelector";
export { SocialAuthButtons } from "./components/SocialAuthButtons";
export { AuthCardHeader } from "./components/AuthCardHeader";

// Headless Hooks
export { useLoginForm } from "./hooks/useLoginForm";
export { useRegisterForm, type RegisterFormErrors } from "./hooks/useRegisterForm";
export { useForgotPasswordForm, useResetPasswordForm } from "./hooks/usePasswordReset";

// Schemas & Criteria Evaluator
export {
  loginSchema,
  registerSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  changePasswordSchema,
  evaluatePasswordCriteria,
} from "./schemas/authSchemas";

// Domain Types
export type {
  AuthUser,
  UserRole,
  AccountStatus,
  AuthFieldErrors,
  LoginInput,
  RegisterInput,
  ForgotPasswordInput,
  ResetPasswordInput,
  ChangePasswordInput,
  PasswordCriteriaResult,
} from "./types";
