"use client";

import React from "react";
import Link from "next/link";
import { Clock, User } from "lucide-react";

export function StudentNextSessionCard() {
  return (
    <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/80 shadow-xs">
      <div className="flex justify-between items-center mb-4">
        <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900">
          Buổi học sắp tới
        </h3>
        <Link href="/dashboard/schedule" className="text-xs text-amber-600 font-bold hover:underline">
          Xem toàn bộ lịch
        </Link>
      </div>

      <div className="p-4 bg-stone-50 border border-stone-200/70 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="size-12 rounded-2xl bg-amber-400 text-neutral-900 flex flex-col items-center justify-center shrink-0 font-bold">
            <span className="text-[10px] uppercase tracking-wider">T5</span>
            <span className="text-base leading-none">18</span>
          </div>
          <div>
            <div className="text-xs sm:text-sm font-bold text-neutral-900">
              Bài 14: Đồ thị hàm số bậc nhất và ứng dụng giải toán thực tế
            </div>
            <div className="flex items-center gap-3 text-xs text-neutral-500 mt-1 font-medium">
              <span className="flex items-center gap-1">
                <Clock className="size-3.5" /> 19:00 - 20:30
              </span>
              <span>·</span>
              <span className="flex items-center gap-1">
                <User className="size-3.5" /> Cô Nguyễn Hoàng Lan
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <span className="px-2.5 py-1 bg-emerald-50 text-emerald-800 rounded-full text-[11px] font-bold border border-emerald-200/50">
            ĐÃ XÁC NHẬN
          </span>
          <button
            type="button"
            className="px-4 py-2 bg-stone-900 hover:bg-neutral-800 text-white text-xs font-bold rounded-xl shadow-xs transition-colors shrink-0"
          >
            Vào phòng học
          </button>
        </div>
      </div>
    </div>
  );
}
