"use client";

import React from "react";
import Link from "next/link";
import { Search, CheckCircle2, TrendingUp, Calendar } from "lucide-react";
import { StudentGoalProgressCard } from "./StudentGoalProgressCard";
import { StudentNextSessionCard } from "./StudentNextSessionCard";
import { StudentActiveClasses } from "./StudentActiveClasses";
import { StudentQuickActions } from "./StudentQuickActions";

interface StudentDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

export function StudentDashboardView({ user }: StudentDashboardViewProps) {
  const studentName = user.fullName || "Minh Anh";

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Greeting & Quick Search Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Chào {studentName} 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            Hôm nay bạn có 1 buổi học Toán lúc 19:00 · Lớp 9 Ôn thi vào 10
          </p>
        </div>
        <Link
          href="/dashboard/tutors"
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-amber-500 hover:bg-amber-600 text-neutral-900 font-bold text-xs rounded-xl shadow-xs transition-all hover:scale-[1.02] shrink-0"
        >
          <Search className="size-4" />
          <span>Tìm gia sư</span>
        </Link>
      </div>

      {/* 2. Main 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        {/* Main Column (8 Cols) */}
        <div className="lg:col-span-8 space-y-6">
          <StudentGoalProgressCard />
          <StudentNextSessionCard />
          <StudentActiveClasses />
        </div>

        {/* Side Column (4 Cols) */}
        <div className="lg:col-span-4 space-y-6">
          {/* Micro Metrics */}
          <div className="grid grid-cols-1 gap-3.5">
            <div className="p-4 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center shrink-0">
                <CheckCircle2 className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 font-semibold">Cột mốc hoàn thành</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900">8 / 12</div>
              </div>
            </div>

            <div className="p-4 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-sky-100 text-sky-800 flex items-center justify-center shrink-0">
                <TrendingUp className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 font-semibold">Điểm bài tập trung bình</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900">
                  8.4 <span className="text-xs font-normal text-neutral-400">/ 10</span>
                </div>
              </div>
            </div>

            <div className="p-4 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center shrink-0">
                <Calendar className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 font-semibold">Chuỗi ngày học tập</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900">
                  6 ngày <span className="text-xs font-bold text-emerald-600">liên tiếp 🔥</span>
                </div>
              </div>
            </div>
          </div>

          <StudentQuickActions />
        </div>
      </div>
    </div>
  );
}
