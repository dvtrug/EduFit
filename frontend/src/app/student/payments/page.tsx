"use client";

import React from "react";
import { FinanceComingSoonNotice } from "@/shared/ui/FinanceComingSoonNotice";

export default function StudentPaymentsPage() {
  return (
    <FinanceComingSoonNotice
      role="STUDENT"
      title="Học phí & Quản lý đóng học"
      subtitle="Theo dõi học phí các lớp học 1-1 và lịch sử thanh toán trực tiếp"
      description="Chức năng nạp và thanh toán học phí trực tuyến hiện chưa khả dụng. Học sinh và phụ huynh thanh toán học phí trực tiếp cho gia sư theo số buổi học thực tế được ghi nhận trong Lịch học."
      badge="Sắp ra mắt"
    />
  );
}
