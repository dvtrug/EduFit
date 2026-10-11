"use client";

import { useState, useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "../api/authApi";
import {
  forgotPasswordSchema,
  resetPasswordSchema,
  evaluatePasswordCriteria,
} from "../schemas/authSchemas";

/**
 * Headless hook xử lý yêu cầu gửi email khôi phục mật khẩu (Forgot Password)
 */
export function useForgotPasswordForm() {
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleEmailChange = useCallback((val: string) => {
    setEmail(val);
    setFieldError(null);
    setGeneralError(null);
  }, []);

  const handleSubmit = useCallback(
    async (e?: React.FormEvent) => {
      if (e) e.preventDefault();
      setFieldError(null);
      setGeneralError(null);

      const parseResult = forgotPasswordSchema.safeParse({ email: email.trim() });
      if (!parseResult.success) {
        setFieldError(parseResult.error.issues[0]?.message || "Email không hợp lệ");
        return false;
      }

      setIsSubmitting(true);
      try {
        await authApi.forgotPassword({ email: parseResult.data.email });
        setSubmitted(true);
        return true;
      } catch (err: unknown) {
        if (err instanceof Error) {
          setGeneralError(err.message);
        } else {
          setGeneralError("Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.");
        }
        return false;
      } finally {
        setIsSubmitting(false);
      }
    },
    [email]
  );

  return {
    email,
    setEmail: handleEmailChange,
    fieldError,
    generalError,
    isSubmitting,
    submitted,
    handleSubmit,
  };
}

/**
 * Headless hook xử lý đặt lại mật khẩu bằng mã token (Reset Password)
 */
export function useResetPasswordForm(token: string) {
  const router = useRouter();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{ newPassword?: string; confirmPassword?: string }>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const passwordCriteria = useMemo(() => {
    return evaluatePasswordCriteria(newPassword);
  }, [newPassword]);

  const handleNewPasswordChange = useCallback((val: string) => {
    setNewPassword(val);
    setFieldErrors((prev) => ({ ...prev, newPassword: undefined }));
    setGeneralError(null);
  }, []);

  const handleConfirmPasswordChange = useCallback((val: string) => {
    setConfirmPassword(val);
    setFieldErrors((prev) => ({ ...prev, confirmPassword: undefined }));
    setGeneralError(null);
  }, []);

  const handleSubmit = useCallback(
    async (e?: React.FormEvent) => {
      if (e) e.preventDefault();
      setFieldErrors({});
      setGeneralError(null);

      const parseResult = resetPasswordSchema.safeParse({
        token,
        newPassword,
        confirmPassword,
      });

      if (!parseResult.success) {
        const errors: { newPassword?: string; confirmPassword?: string } = {};
        for (const issue of parseResult.error.issues) {
          const field = issue.path[0] as "newPassword" | "confirmPassword";
          if (field && !errors[field]) {
            errors[field] = issue.message;
          }
        }
        setFieldErrors(errors);
        return false;
      }

      setIsSubmitting(true);
      try {
        await authApi.resetPassword({
          token,
          newPassword,
        });
        setSuccess(true);
        setTimeout(() => {
          router.push("/login");
        }, 3000);
        return true;
      } catch (err: unknown) {
        if (err instanceof Error) {
          setGeneralError(err.message);
        } else {
          setGeneralError("Mã token không hợp lệ hoặc đã hết hạn (30 phút). Vui lòng yêu cầu lại.");
        }
        return false;
      } finally {
        setIsSubmitting(false);
      }
    },
    [token, newPassword, confirmPassword, router]
  );

  return {
    newPassword,
    confirmPassword,
    setNewPassword: handleNewPasswordChange,
    setConfirmPassword: handleConfirmPasswordChange,
    passwordCriteria,
    fieldErrors,
    generalError,
    isSubmitting,
    success,
    handleSubmit,
  };
}
