"use client";

import React from "react";
import Link from "next/link";
import { Clock, CalendarPlus } from "lucide-react";
import { EmptyState } from "@/shared/ui/EmptyState";

export interface UpcomingClassItem {
  time: string;
  title: string;
  topic: string;
  statusBadge: string;
  actionText: string;
  isPrimary: boolean;
}

interface TutorUpcomingClassesProps {
  classes?: UpcomingClassItem[];
}

export function TutorUpcomingClasses({ classes = [] }: TutorUpcomingClassesProps) {
  if (classes.length === 0) {
    return (
      <div className="bg-white p-6 rounded-3xl border border-stone-200/80 shadow-xs space-y-4">
        <div className="flex justify-between items-center">
          <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900">
            Lịch dạy hôm nay
          </h3>
          <span className="text-xs text-neutral-400 font-medium">0 buổi</span>
        </div>

        <EmptyState
          icon={CalendarPlus}
          theme="sky"
          compact
          title="Chưa có lịch dạy nào được xếp hôm nay"
          description="Cập nhật ma trận khung giờ rảnh trong tuần để hệ thống tự động gợi ý lịch học tương thích tới học sinh & phụ huynh."
          action={{
            label: "Cập nhật ma trận lịch rảnh",
            href: "/dashboard/profile",
            icon: Clock,
          }}
        />
      </div>
    );
  }

  return (
    <div className="bg-white p-6 rounded-3xl border border-stone-200/80 shadow-xs">
      <div className="flex justify-between items-center mb-4">
        <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900">
          Lịch dạy hôm nay
        </h3>
        <Link href="/dashboard/schedule" className="text-xs text-sky-600 font-bold hover:underline">
          Xem toàn bộ tuần
        </Link>
      </div>

      <div className="space-y-3">
        {classes.map((cls, idx) => (
          <div
            key={idx}
            className="p-4 rounded-2xl bg-stone-50 border border-stone-200/70 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
          >
            <div className="flex items-center gap-3.5">
              <div
                className={`size-11 rounded-xl font-bold text-xs flex flex-col items-center justify-center shrink-0 ${
                  cls.isPrimary ? "bg-sky-100 text-sky-800" : "bg-stone-200 text-neutral-800"
                }`}
              >
                <span>{cls.time}</span>
              </div>
              <div>
                <h4 className="text-xs sm:text-sm font-bold text-neutral-900">{cls.title}</h4>
                <p className="text-xs text-neutral-500 mt-0.5">{cls.topic}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span
                className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                  cls.isPrimary
                    ? "bg-sky-50 text-sky-800 border border-sky-100"
                    : "bg-stone-100 text-neutral-700"
                }`}
              >
                {cls.statusBadge}
              </span>
              <button
                type="button"
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                  cls.isPrimary
                    ? "bg-stone-900 hover:bg-neutral-800 text-white shadow-xs"
                    : "border border-stone-200 hover:bg-stone-50 text-neutral-700"
                }`}
              >
                {cls.actionText}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
