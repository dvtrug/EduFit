"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Plus, CreditCard, Sparkles, Clock } from "lucide-react";
import { ParentChildSwitcher, ChildInfo } from "./ParentChildSwitcher";
import { ParentKpiGrid } from "./ParentKpiGrid";
import { ParentActiveTutors } from "./ParentActiveTutors";
import { ParentQuickActions } from "./ParentQuickActions";

interface ParentDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

const DEFAULT_CHILDREN: ChildInfo[] = [
  {
    id: 0,
    name: "Trần Minh Đức",
    grade: "Lớp 9",
    tutorsCount: 2,
    completedLessons: "14 / 16",
    avgScore: "9.2",
    tuition: "2.400.000 đ",
    nextSession: "19:00 Tối nay (Toán)",
  },
  {
    id: 1,
    name: "Trần Thảo Linh",
    grade: "Lớp 7",
    tutorsCount: 1,
    completedLessons: "8 / 12",
    avgScore: "8.6",
    tuition: "1.600.000 đ",
    nextSession: "17:30 Chiều mai (Tiếng Anh)",
  },
];

export function ParentDashboardView({ user }: ParentDashboardViewProps) {
  const parentName = user.fullName || "Chị Mai";
  const [selectedChild, setSelectedChild] = useState<number>(0);
  const currentChild = DEFAULT_CHILDREN[selectedChild];

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Greeting & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Chào {parentName}, tổng quan học tập con em 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            Bạn đang theo dõi {DEFAULT_CHILDREN.length} học sinh cùng 3 gia sư đang trực tiếp dạy kèm.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Link
            href="/dashboard/link-student"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 bg-white hover:bg-stone-50 border border-stone-200 text-neutral-800 rounded-xl text-xs font-bold shadow-2xs transition-colors shrink-0"
          >
            <Plus className="size-3.5 text-neutral-500" />
            <span>Liên kết thêm con</span>
          </Link>
          <Link
            href="/dashboard/tuition"
            className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors shrink-0"
          >
            <CreditCard className="size-3.5" />
            <span>Thanh toán học phí</span>
          </Link>
        </div>
      </div>

      {/* 2. Child Switcher Tabs */}
      <ParentChildSwitcher
        childrenList={DEFAULT_CHILDREN}
        selectedChild={selectedChild}
        onSelect={setSelectedChild}
      />

      {/* 3. KPI Grid */}
      <ParentKpiGrid child={currentChild} />

      {/* 4. 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        <div className="lg:col-span-8 space-y-6">
          <ParentActiveTutors />
        </div>

        <div className="lg:col-span-4 space-y-6">
          <ParentQuickActions />
        </div>
      </div>
    </div>
  );
}
