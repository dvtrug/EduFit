"use client";

import React from "react";
import { BookOpen, ArrowLeft } from "lucide-react";
import { EmptyState } from "@/shared/ui/EmptyState";

export default function AdminCatalogsPage() {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <div className="bg-white p-8 rounded-3xl border border-stone-200/90 shadow-xs">
        <EmptyState
          icon={BookOpen}
          theme="sky"
          badge="DANH MỤC MÔN HỌC & CHƯƠNG TRÌNH"
          title="Cấu hình danh mục môn học EduFit"
          description="Danh mục các môn học tiêu chuẩn từ Lớp 1 đến Lớp 12, Luyện thi ĐH và Kỹ năng mềm được đồng bộ tự động từ cơ sở dữ liệu định danh trung tâm."
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
