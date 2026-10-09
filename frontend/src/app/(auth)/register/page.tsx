import React from "react";
import { RegisterForm } from "@/features/auth";

export const metadata = {
  title: "Đăng ký - EduFit",
  description: "Đăng ký tài khoản kết nối gia sư EduFit",
};

/**
 * Trang Đăng ký Tài khoản (App Router Page)
 * Tách biệt theo kiến trúc Feature-based, mã nguồn siêu gọn nhẹ
 */
export default function RegisterPage() {
  return <RegisterForm />;
}
