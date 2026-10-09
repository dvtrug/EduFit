"use client";

import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { profileService } from "@/services/profile";
import { loginSchema } from "../schemas/authSchemas";
import type { LoginInput, AuthFieldErrors, AuthUser } from "../types";

export function useLoginForm() {
  const router = useRouter();
  const { login } = useAuth();

  const [formData, setFormData] = useState<LoginInput>({
    email: "",
    password: "",
  });
  const [fieldErrors, setFieldErrors] = useState<AuthFieldErrors<LoginInput>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const setEmail = useCallback((email: string) => {
    setFormData((prev) => ({ ...prev, email }));
    setFieldErrors((prev) => ({ ...prev, email: undefined }));
    setGeneralError(null);
  }, []);

  const setPassword = useCallback((password: string) => {
    setFormData((prev) => ({ ...prev, password }));
    setFieldErrors((prev) => ({ ...prev, password: undefined }));
    setGeneralError(null);
  }, []);

  const fillQuickLogin = useCallback((email: string, pass: string) => {
    setFormData({ email, password: pass });
    setFieldErrors({});
    setGeneralError(null);
  }, []);

  const resolvePostLoginRedirect = useCallback(
    async (user: AuthUser) => {
      if (user.role === "STUDENT") {
        try {
          const profile = await profileService.getMyStudentProfile();
          if (!profile.profileComplete) {
            router.push("/onboarding/student");
            return;
          }
        } catch {
          router.push("/onboarding/student");
          return;
        }
      } else if (user.role === "TUTOR") {
        try {
          const profile = await profileService.getMyTutorProfile();
          const hasOnboarded =
            typeof window !== "undefined" &&
            localStorage.getItem("edufit_tutor_onboarding_completed");
          if (!profile?.bio && !hasOnboarded) {
            router.push("/onboarding/tutor");
            return;
          }
        } catch {
          router.push("/onboarding/tutor");
          return;
        }
      } else if (user.role === "PARENT") {
        const hasOnboarded =
          typeof window !== "undefined" &&
          localStorage.getItem("edufit_parent_onboarding_completed");
        if (!hasOnboarded) {
          router.push("/onboarding/parent");
          return;
        }
      }

      router.push(user.redirectUrl || "/dashboard");
    },
    [router]
  );

  const handleSubmit = useCallback(
    async (e?: React.FormEvent) => {
      if (e) e.preventDefault();
      setFieldErrors({});
      setGeneralError(null);

      // Client Domain Validation với Zod
      const parseResult = loginSchema.safeParse({
        email: formData.email.trim(),
        password: formData.password,
      });

      if (!parseResult.success) {
        const errors: AuthFieldErrors<LoginInput> = {};
        for (const issue of parseResult.error.issues) {
          const field = issue.path[0] as keyof LoginInput;
          if (field && !errors[field]) {
            errors[field] = issue.message;
          }
        }
        setFieldErrors(errors);
        return false;
      }

      setIsSubmitting(true);
      try {
        const loggedUser = await login(parseResult.data);
        await resolvePostLoginRedirect(loggedUser);
        return true;
      } catch (err: unknown) {
        if (err instanceof Error) {
          setGeneralError(err.message);
        } else {
          setGeneralError("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.");
        }
        return false;
      } finally {
        setIsSubmitting(false);
      }
    },
    [formData, login, resolvePostLoginRedirect]
  );

  return {
    email: formData.email,
    password: formData.password,
    setEmail,
    setPassword,
    fieldErrors,
    generalError,
    isSubmitting,
    fillQuickLogin,
    handleSubmit,
  };
}
