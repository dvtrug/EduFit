"use client";

import React from "react";
import { ChevronRight } from "lucide-react";

export function StudentActiveClasses() {
  const classes = [
    {
      subject: "Môn Toán",
      badgeColor: "bg-amber-100 text-amber-900",
      borderColor: "hover:border-amber-300",
      frequency: "2 buổi/tuần",
      title: "Toán 9 Luyện Thi Vào 10",
      tutor: "Nguyễn Hoàng Lan",
      progress: "Hoàn thành 14/24 buổi",
    },
    {
      subject: "Tiếng Anh",
      badgeColor: "bg-sky-100 text-sky-900",
      borderColor: "hover:border-sky-300",
      frequency: "1 buổi/tuần",
      title: "Tiếng Anh Giao Tiếp & Ngữ Pháp 9",
      tutor: "Trần Hải Đăng",
      progress: "Hoàn thành 6/12 buổi",
    },
  ];

  return (
    <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/80 shadow-xs">
      <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900 mb-4">
        Lớp học đang diễn ra
      </h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {classes.map((cls, idx) => (
          <div
            key={idx}
            className={`p-4 rounded-2xl border border-stone-200 ${cls.borderColor} transition-colors flex flex-col justify-between gap-3`}
          >
            <div className="flex items-center justify-between">
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${cls.badgeColor}`}>
                {cls.subject}
              </span>
              <span className="text-xs text-neutral-400 font-medium">{cls.frequency}</span>
            </div>
            <div>
              <h4 className="text-sm font-bold text-neutral-900">{cls.title}</h4>
              <p className="text-xs text-neutral-500 mt-0.5">Gia sư: {cls.tutor}</p>
            </div>
            <div className="pt-2 border-t border-stone-100 flex items-center justify-between text-xs text-neutral-500">
              <span>{cls.progress}</span>
              <ChevronRight className="size-4 text-neutral-400" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
