"use client";

import React, { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { ResetPasswordForm } from "@/features/auth/components/ResetPasswordForm";

function ResetPasswordContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";

  if (!token) {
    return (
      <div className="w-full max-w-md bg-white rounded-3xl border border-stone-200/90 shadow-xl p-8 text-center space-y-3">
        <h3 className="font-heading text-lg font-bold text-rose-700">Liên kết không hợp lệ</h3>
        <p className="text-xs text-neutral-500 leading-relaxed">
          Không tìm thấy mã xác thực token đặt lại mật khẩu trong liên kết của bạn. Vui lòng kiểm tra lại email hoặc gửi yêu cầu mới.
        </p>
      </div>
    );
  }

  return <ResetPasswordForm token={token} />;
}

export default function ResetPasswordPage() {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-amber-200">
      <Suspense
        fallback={
          <div className="w-full max-w-md bg-white rounded-3xl border border-stone-200 p-8 text-center text-xs text-neutral-500">
            Đang tải biểu mẫu...
          </div>
        }
      >
        <ResetPasswordContent />
      </Suspense>
    </div>
  );
}
