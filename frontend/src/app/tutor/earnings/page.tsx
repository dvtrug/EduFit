"use client";

import React from "react";
import { FinanceComingSoonNotice } from "@/shared/ui/FinanceComingSoonNotice";

export default function TutorEarningsPage() {
  return (
    <FinanceComingSoonNotice
      role="TUTOR"
      title="Báo cáo thu nhập & Đối soát giảng dạy"
      subtitle="Theo dõi thu nhập các lớp kèm cặp 1-1 và bảng kê đối soát học phí"
      description="Chức năng rút tiền và ví thu nhập điện tử hiện chưa khả dụng. Gia sư nhận thù lao giảng dạy trực tiếp từ phụ huynh hoặc học sinh theo đơn giá thỏa thuận cho mỗi buổi dạy hoàn thành."
      badge="Sắp ra mắt"
    />
  );
}
