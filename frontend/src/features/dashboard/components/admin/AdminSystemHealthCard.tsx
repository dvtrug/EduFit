"use client";

import React from "react";
import { Activity, CheckCircle2 } from "lucide-react";

export function AdminSystemHealthCard() {
  return (
    <div className="bg-white dark:bg-stone-900 p-5 rounded-2xl border border-stone-200/80 dark:border-stone-800 shadow-xs space-y-3 transition-colors">
      <div className="flex items-center gap-2">
        <Activity className="size-4 text-emerald-600 dark:text-emerald-400" />
        <h4 className="text-xs font-bold text-neutral-400 dark:text-stone-500 uppercase tracking-wider">
          Trạng thái hệ thống
        </h4>
      </div>
      <div className="space-y-2 text-xs">
        <div className="flex items-center justify-between">
          <span className="text-neutral-600 dark:text-stone-400">Spring Boot API</span>
          <span className="inline-flex items-center gap-1 text-emerald-700 dark:text-emerald-400 font-bold">
            <CheckCircle2 className="size-3.5 text-emerald-600 dark:text-emerald-400" /> Hoạt động
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-neutral-600 dark:text-stone-400">PostgreSQL (Dev DB)</span>
          <span className="inline-flex items-center gap-1 text-emerald-700 dark:text-emerald-400 font-bold">
            <CheckCircle2 className="size-3.5 text-emerald-600 dark:text-emerald-400" /> Kết nối tốt
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-neutral-600 dark:text-stone-400">Stateful Session Cookie</span>
          <span className="inline-flex items-center gap-1 text-emerald-700 dark:text-emerald-400 font-bold">
            <CheckCircle2 className="size-3.5 text-emerald-600 dark:text-emerald-400" /> Khả dụng
          </span>
        </div>
      </div>
    </div>
  );
}
