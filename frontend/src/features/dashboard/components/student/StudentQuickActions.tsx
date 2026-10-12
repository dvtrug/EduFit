"use client";

import React from "react";
import Link from "next/link";
import { Search, CreditCard, MessageSquare, ChevronRight } from "lucide-react";

export function StudentQuickActions() {
  return (
    <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs space-y-2">
      <h4 className="text-xs font-bold text-neutral-400 uppercase tracking-wider mb-2">
        Thao tác nhanh
      </h4>
      <Link
        href="/dashboard/tutors"
        className="w-full p-2.5 rounded-xl hover:bg-stone-50 flex items-center justify-between text-xs font-semibold text-neutral-700 transition-colors"
      >
        <div className="flex items-center gap-2.5">
          <Search className="size-4 text-neutral-500" />
          <span>Tìm gia sư môn mới</span>
        </div>
        <ChevronRight className="size-4 text-neutral-400" />
      </Link>
      <Link
        href="/dashboard/payments"
        className="w-full p-2.5 rounded-xl hover:bg-stone-50 flex items-center justify-between text-xs font-semibold text-neutral-700 transition-colors"
      >
        <div className="flex items-center gap-2.5">
          <CreditCard className="size-4 text-neutral-500" />
          <span>Nạp học phí / Thanh toán</span>
        </div>
        <ChevronRight className="size-4 text-neutral-400" />
      </Link>
      <button
        type="button"
        className="w-full p-2.5 rounded-xl hover:bg-stone-50 flex items-center justify-between text-xs font-semibold text-neutral-700 transition-colors text-left"
      >
        <div className="flex items-center gap-2.5">
          <MessageSquare className="size-4 text-neutral-500" />
          <span>Nhắn tin cho gia sư</span>
        </div>
        <ChevronRight className="size-4 text-neutral-400" />
      </button>
    </div>
  );
}
