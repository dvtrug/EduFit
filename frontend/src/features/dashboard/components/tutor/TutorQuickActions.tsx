"use client";

import React from "react";
import Link from "next/link";
import { Compass, Clock, BookOpen, ChevronRight } from "lucide-react";

export function TutorQuickActions() {
  return (
    <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs space-y-2">
      <h4 className="text-xs font-bold text-neutral-400 uppercase tracking-wider mb-2">
        Thao tác giảng dạy
      </h4>
      <Link
        href="/dashboard/schedule"
        className="w-full p-2.5 rounded-xl hover:bg-stone-50 flex items-center justify-between text-xs font-semibold text-neutral-700 transition-colors"
      >
        <div className="flex items-center gap-2.5">
          <Clock className="size-4 text-sky-600" />
          <span>Cập nhật lịch rảnh & bận</span>
        </div>
        <ChevronRight className="size-4 text-neutral-400" />
      </Link>
      <Link
        href="/dashboard/classes"
        className="w-full p-2.5 rounded-xl hover:bg-stone-50 flex items-center justify-between text-xs font-semibold text-neutral-700 transition-colors"
      >
        <div className="flex items-center gap-2.5">
          <BookOpen className="size-4 text-indigo-600" />
          <span>Quản lý danh sách lớp học</span>
        </div>
        <ChevronRight className="size-4 text-neutral-400" />
      </Link>
      <Link
        href="/dashboard/profile"
        className="w-full p-2.5 rounded-xl hover:bg-stone-50 flex items-center justify-between text-xs font-semibold text-neutral-700 transition-colors"
      >
        <div className="flex items-center gap-2.5">
          <Compass className="size-4 text-amber-600" />
          <span>Chỉnh sửa hồ sơ & học phí/giờ</span>
        </div>
        <ChevronRight className="size-4 text-neutral-400" />
      </Link>
    </div>
  );
}
