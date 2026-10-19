"use client";

import React from "react";
import { FinanceComingSoonNotice } from "@/shared/ui/FinanceComingSoonNotice";

export default function ParentBillingPage() {
  return (
    <FinanceComingSoonNotice
      role="PARENT"
      title="Gói học & Quản lý học phí con em"
      subtitle="Quản lý chi phí học tập của các con và lịch sử đối soát học phí gia sư"
      description="Chức năng thanh toán học phí qua thẻ hoặc ví điện tử hiện chưa khả dụng. Quý phụ huynh vui lòng thanh toán trực tiếp cho gia sư bằng tiền mặt hoặc chuyển khoản ngân hàng dựa trên số buổi thực dạy đã điểm danh."
      badge="Sắp ra mắt"
    />
  );
}
