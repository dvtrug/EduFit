"use client";

import React from "react";
import Link from "next/link";
import {
  CreditCard,
  ArrowLeft,
  Calendar,
  Clock,
  Sparkles,
  BookOpen,
} from "lucide-react";

export default function StudentPaymentsPage() {
  return (
    <div className="max-w-4xl mx-auto space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div>
        <Link
          href="/student/dashboard"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-neutral-900 transition-colors mb-2"
        >
          <ArrowLeft className="size-3.5" />
          <span>Quay lại Tổng quan</span>
        </Link>
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight flex items-center gap-2.5">
          <CreditCard className="size-7 text-amber-500" />
          Học phí & Thanh toán lớp học
        </h1>
        <p className="text-xs sm:text-sm text-neutral-500 mt-1">
          Theo dõi trạng thái học phí các buổi học gia sư và thông tin thanh toán trực tiếp hoặc qua phụ huynh.
        </p>
      </div>

      {/* Notice Card */}
      <div className="p-6 rounded-3xl bg-amber-50/70 border border-amber-200 text-amber-900 space-y-3">
        <div className="flex items-center gap-2 text-amber-800 font-bold text-sm">
          <Clock className="size-5 text-amber-600" />
          <span>Phân hệ Nạp học phí & Cổng thanh toán trực tuyến</span>
        </div>
        <p className="text-xs leading-relaxed text-amber-900/80">
          Hiện tại phân hệ nạp học phí tự động trực tuyến đang được tích hợp nâng cấp. Học sinh có thể
          liên kết tài khoản với Phụ huynh để phụ huynh quản lý các gói học phí, hoặc trao đổi trực tiếp
          cùng Gia sư để thống nhất phương thức thanh toán từng buổi học.
        </p>
      </div>

      {/* Quick Action Navigation */}
      <div className="p-6 bg-white rounded-3xl border border-stone-200 shadow-xs space-y-4">
        <h2 className="text-sm font-bold text-neutral-900 uppercase tracking-wider">
          Lối tắt thao tác
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Link
            href="/student/classes"
            className="p-4 rounded-2xl border border-stone-200 hover:border-amber-400 transition-colors flex items-center gap-3.5"
          >
            <div className="size-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center shrink-0">
              <BookOpen className="size-5" />
            </div>
            <div>
              <h3 className="text-xs sm:text-sm font-bold text-neutral-900">Danh sách lớp học</h3>
              <p className="text-xs text-neutral-500 mt-0.5">Kiểm tra số buổi học đã hoàn thành</p>
            </div>
          </Link>

          <Link
            href="/student/schedule"
            className="p-4 rounded-2xl border border-stone-200 hover:border-sky-400 transition-colors flex items-center gap-3.5"
          >
            <div className="size-10 rounded-xl bg-sky-100 text-sky-800 flex items-center justify-center shrink-0">
              <Calendar className="size-5" />
            </div>
            <div>
              <h3 className="text-xs sm:text-sm font-bold text-neutral-900">Thời khóa biểu tuần</h3>
              <p className="text-xs text-neutral-500 mt-0.5">Theo dõi lịch học các buổi sắp diễn ra</p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}
