"use client";

import React from "react";
import Link from "next/link";

export function TutorUpcomingClasses() {
  const classes = [
    {
      time: "17:30",
      title: "Toán 9 Luyện Đề Tuyển Sinh · Học sinh: Trần Minh Đức",
      topic: "Chuyên đề: Phương trình bậc hai & Định lý Vi-ét (Buổi 15/24)",
      statusBadge: "Sắp diễn ra",
      actionText: "Vào dạy",
      isPrimary: true,
    },
    {
      time: "19:30",
      title: "Toán 8 Nâng Cao · Học sinh: Lê Quỳnh Nga",
      topic: "Chuyên đề: Định lý Talet và tam giác đồng dạng (Buổi 8/16)",
      statusBadge: "Tối nay",
      actionText: "Chuẩn bị bài",
      isPrimary: false,
    },
  ];

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
                    : "bg-stone-200 text-neutral-700"
                }`}
              >
                {cls.statusBadge}
              </span>
              <button
                type="button"
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-colors shrink-0 ${
                  cls.isPrimary
                    ? "bg-sky-600 hover:bg-sky-700 text-white shadow-2xs"
                    : "bg-stone-200 hover:bg-stone-300 text-neutral-800"
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
