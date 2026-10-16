"use client";

import React, { Suspense } from "react";
import { useAuth } from "@/context/AuthContext";
import { ProgressDashboardView } from "@/features/progress";

export default function StudentProgressPage() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      <div>
        <h1 className="font-heading font-extrabold text-2xl text-neutral-900 tracking-tight">
          Tiến độ học tập cá nhân
        </h1>
        <p className="text-xs text-neutral-500 mt-1">
          Theo dõi lộ trình các cột mốc mục tiêu và nhận xét từ gia sư đồng hành
        </p>
      </div>

      <Suspense fallback={<div className="py-12 text-center text-xs text-neutral-400">Đang tải tiến độ...</div>}>
        <ProgressDashboardView role="STUDENT" studentId={user.id} />
      </Suspense>
    </div>
  );
}
