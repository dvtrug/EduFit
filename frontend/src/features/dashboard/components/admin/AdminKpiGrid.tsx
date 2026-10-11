"use client";

import React from "react";
import { Users, BookOpen, DollarSign, ShieldAlert } from "lucide-react";

export function AdminKpiGrid() {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Tổng người dùng</span>
          <Users className="size-4 text-purple-700" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">1,248</div>
          <div className="text-[11px] text-emerald-600 font-semibold mt-0.5">+142 tài khoản mới tuần này</div>
        </div>
      </div>

      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Lớp học hoạt động</span>
          <BookOpen className="size-4 text-indigo-600" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">142 lớp</div>
          <div className="text-[11px] text-indigo-700 font-semibold mt-0.5">Tỷ lệ duy trì buổi: 98.4%</div>
        </div>
      </div>

      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Doanh số nền tảng (GMV)</span>
          <DollarSign className="size-4 text-emerald-600" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">38.500.000 đ</div>
          <div className="text-[11px] text-emerald-600 font-semibold mt-0.5">Phí nền tảng (10%): 3.850.000 đ</div>
        </div>
      </div>

      <div className="bg-white p-5 rounded-2xl border border-red-200 shadow-xs flex flex-col justify-between bg-red-50/20">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider text-red-700">Yêu cầu cần xử lý</span>
          <ShieldAlert className="size-4 text-red-600" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-red-700">8 KYC · 2 Khiếu nại</div>
          <div className="text-[11px] text-red-600 font-semibold mt-0.5">Ưu tiên xử lý trong ngày</div>
        </div>
      </div>
    </div>
  );
}
