import React from "react";
import { ForgotPasswordForm } from "@/features/auth/components/ForgotPasswordForm";

export const metadata = {
  title: "Quên mật khẩu - EduFit",
  description: "Khôi phục mật khẩu tài khoản EduFit qua email",
};

export default function ForgotPasswordPage() {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-amber-200">
      <ForgotPasswordForm />
    </div>
  );
}
