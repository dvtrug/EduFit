import React from "react";
import { RegisterForm } from "@/features/auth/components/RegisterForm";

/**
 * Trang Đăng ký Tài khoản (App Router Page)
 * Tách biệt theo kiến trúc Feature-based, mã nguồn siêu gọn nhẹ (< 30 dòng)
 */
export default function RegisterPage() {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-amber-200">
      <RegisterForm />
    </div>
  );
}
