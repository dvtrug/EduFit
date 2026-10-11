"use client";

import React from "react";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";

export function AdminDisputesCard() {
  return (
    <div className="bg-white p-6 rounded-3xl border border-stone-200/80 shadow-xs">
      <div className="flex justify-between items-center mb-4">
        <div className="flex items-center gap-2">
          <AlertTriangle className="size-5 text-amber-500" />
          <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900">
            Tranh chấp & Khiếu nại cần can thiệp
          </h3>
        </div>
        <Link href="/dashboard/disputes" className="text-xs text-purple-700 font-bold hover:underline">
          Xem toàn bộ
        </Link>
      </div>

      <div className="space-y-3">
        <div className="p-4 rounded-2xl bg-amber-50/60 border border-amber-200/70 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-200 text-amber-900">
                HOÀN TIỀN
              </span>
              <span className="text-xs font-bold text-neutral-900">
                Khiếu nại buổi học bù Toán 9 (#DP-1042)
              </span>
            </div>
            <p className="text-xs text-neutral-600 mt-1">
              Phụ huynh Trần Thu Trang báo gia sư hủy lịch đột xuất trước 30 phút.
            </p>
          </div>
          <button
            type="button"
            className="px-3.5 py-1.5 bg-neutral-900 hover:bg-neutral-800 text-white rounded-xl text-xs font-bold shrink-0 transition-colors"
          >
            Giải quyết ngay
          </button>
        </div>
      </div>
    </div>
  );
}
