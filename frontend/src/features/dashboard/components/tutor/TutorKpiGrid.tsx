"use client";

import React from "react";
import { Calendar, Users, DollarSign, Star } from "lucide-react";

export function TutorKpiGrid() {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Số buổi tháng này</span>
          <Calendar className="size-4 text-sky-600" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">28 / 32</div>
          <div className="text-[11px] text-emerald-600 font-semibold mt-0.5">87% kế hoạch tháng</div>
        </div>
      </div>

      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Học sinh đang dạy</span>
          <Users className="size-4 text-indigo-600" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">6 học sinh</div>
          <div className="text-[11px] text-neutral-500 font-medium mt-0.5">Tất cả lớp đang duy trì tốt</div>
        </div>
      </div>

      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Thu nhập tạm tính</span>
          <DollarSign className="size-4 text-emerald-600" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">8.400.000 đ</div>
          <div className="text-[11px] text-neutral-400 font-medium mt-0.5">Kỳ quyết toán: 15 hàng tháng</div>
        </div>
      </div>

      <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs flex flex-col justify-between">
        <div className="flex items-center justify-between text-neutral-400">
          <span className="text-xs font-bold uppercase tracking-wider">Đánh giá trung bình</span>
          <Star className="size-4 text-amber-500 fill-amber-500" />
        </div>
        <div className="mt-3">
          <div className="text-2xl font-extrabold font-heading text-neutral-900">4.9 / 5.0</div>
          <div className="text-[11px] text-amber-700 font-semibold mt-0.5">Từ 18 đánh giá phụ huynh & học sinh</div>
        </div>
      </div>
    </div>
  );
}
