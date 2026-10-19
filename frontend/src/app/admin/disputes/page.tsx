"use client";

import React from "react";
import { AlertTriangle, ArrowLeft } from "lucide-react";
import { EmptyState } from "@/shared/ui/EmptyState";

export default function AdminDisputesPage() {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <div className="bg-white p-8 rounded-3xl border border-stone-200/90 shadow-xs">
        <EmptyState
          icon={AlertTriangle}
          theme="amber"
          badge="HỆ THỐNG TRỌNG TÀI & KHIẾU NẠI"
          title="Không có tranh chấp nào cần giải quyết"
          description="Hiện tại không có tranh chấp hoặc khiếu nại chưa xử lý giữa học sinh/phụ huynh và gia sư. Hệ thống sẽ hiển thị các vụ việc phát sinh tại đây khi nhận được khiếu nại."
          secondaryAction={{
            label: "Về trang tổng quan",
            href: "/admin/dashboard",
            icon: ArrowLeft,
          }}
        />
      </div>
    </div>
  );
}
