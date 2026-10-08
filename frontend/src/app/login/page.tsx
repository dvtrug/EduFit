import React, { Suspense } from "react";
import { LoginForm } from "@/features/auth/components/LoginForm";

/**
 * Trang Đăng nhập (App Router Page)
 * Tách biệt theo kiến trúc Feature-based, mã nguồn siêu gọn nhẹ (< 40 dòng)
 */
export default function LoginPage() {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-amber-200">
      <Suspense
        fallback={
          <div className="w-full max-w-md p-8 bg-white rounded-3xl border border-stone-200 shadow-md text-center text-sm text-neutral-500">
            Đang tải biểu mẫu đăng nhập...
          </div>
        }
      >
        <LoginForm />
      </Suspense>
    </div>
  );
}
