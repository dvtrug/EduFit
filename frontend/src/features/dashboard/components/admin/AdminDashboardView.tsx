"use client";

import React from "react";
import { Download, Settings } from "lucide-react";
import { AdminKpiGrid } from "./AdminKpiGrid";
import { AdminDisputesCard } from "./AdminDisputesCard";
import { AdminSubjectDemandCard } from "./AdminSubjectDemandCard";
import { AdminKycQueueCard } from "./AdminKycQueueCard";
import { AdminSystemHealthCard } from "./AdminSystemHealthCard";

interface AdminDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

export function AdminDashboardView({ user }: AdminDashboardViewProps) {
  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Header Greeting & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Bảng điều hành Tổng thể Hệ thống EduFit 📊
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            Toàn hệ thống hiện có 1,248 tài khoản, 142 lớp học đang diễn ra và 8 hồ sơ gia sư chờ duyệt.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 bg-white hover:bg-stone-50 border border-stone-200 text-neutral-800 rounded-xl text-xs font-bold shadow-2xs transition-colors shrink-0"
          >
            <Download className="size-3.5 text-neutral-500" />
            <span>Xuất dữ liệu Excel</span>
          </button>
          <button
            type="button"
            className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-purple-700 hover:bg-purple-800 text-white rounded-xl text-xs font-bold shadow-xs transition-colors shrink-0"
          >
            <Settings className="size-3.5" />
            <span>Cấu hình tham số</span>
          </button>
        </div>
      </div>

      {/* 2. Admin KPI Row */}
      <AdminKpiGrid />

      {/* 3. 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        <div className="lg:col-span-8 space-y-6">
          <AdminDisputesCard />
          <AdminSubjectDemandCard />
        </div>

        <div className="lg:col-span-4 space-y-6">
          <AdminKycQueueCard />
          <AdminSystemHealthCard />
        </div>
      </div>
    </div>
  );
}
