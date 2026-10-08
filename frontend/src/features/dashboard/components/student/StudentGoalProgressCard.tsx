"use client";

import React from "react";
import Link from "next/link";
import { Sparkles, ArrowRight } from "lucide-react";

export function StudentGoalProgressCard() {
  return (
    <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/80 shadow-xs flex flex-col justify-between gap-6">
      <div>
        <div className="flex justify-between items-center mb-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-stone-100 rounded-full text-xs font-semibold text-neutral-700">
            <Sparkles className="size-3.5 text-amber-500" />
            <span>Mục tiêu đang tập trung</span>
          </div>
          <span className="px-2.5 py-0.5 bg-amber-200 text-amber-950 rounded-full text-xs font-bold">
            Lớp 9
          </span>
        </div>

        <h3 className="font-heading text-lg sm:text-xl font-bold text-neutral-900 mb-1">
          Đạt 8+ điểm Toán trong kỳ thi tuyển sinh vào lớp 10
        </h3>
        <p className="text-xs text-neutral-500 font-medium">
          Lộ trình học kèm 1-1 cùng cô Nguyễn Hoàng Lan · 2 buổi/tuần
        </p>
      </div>

      <div className="space-y-2">
        <div className="flex justify-between items-center text-xs font-semibold">
          <span className="text-neutral-600">Tiến độ lộ trình học tập</span>
          <span className="text-emerald-600 font-bold text-sm">68%</span>
        </div>
        <div className="w-full h-3 bg-stone-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-emerald-500 rounded-full transition-all duration-700"
            style={{ width: "68%" }}
          />
        </div>
        <div className="flex justify-between items-center text-[11px] text-neutral-400 font-medium">
          <span>8 / 12 cột mốc đã hoàn thành</span>
          <span>Còn 4 cột mốc quan trọng</span>
        </div>
      </div>

      <div className="pt-3 flex items-center justify-between border-t border-stone-100 text-xs">
        <span className="text-neutral-500 font-medium">Buổi học tiếp theo: T5, 19:00</span>
        <Link
          href="/dashboard/progress"
          className="text-amber-600 hover:text-amber-700 font-bold flex items-center gap-1 transition-transform hover:translate-x-0.5"
        >
          Xem chi tiết lộ trình <ArrowRight className="size-3.5" />
        </Link>
      </div>
    </div>
  );
}
