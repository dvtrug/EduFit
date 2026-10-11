import React from "react";

/**
 * Layout tập trung dùng chung cho toàn bộ các trang xác thực (Route Group (auth))
 * Căn giữa màn hình, không hiển thị sidebar dashboard
 */
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-amber-200">
      {children}
    </div>
  );
}
