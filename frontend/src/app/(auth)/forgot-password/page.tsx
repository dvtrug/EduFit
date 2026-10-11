import React from "react";
import { ForgotPasswordForm } from "@/features/auth";

export const metadata = {
  title: "Quên mật khẩu - EduFit",
  description: "Khôi phục mật khẩu tài khoản EduFit qua email",
};

/**
 * Trang Quên Mật Khẩu (App Router Page)
 */
export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
