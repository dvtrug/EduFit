"use client";

import React from "react";
import Link from "next/link";
import { MessageSquare, Sparkles } from "lucide-react";

export function ParentActiveTutors() {
  const tutors = [
    {
      name: "Nguyễn Hoàng Lan",
      subject: "Toán 9 Luyện Thi Vào 10",
      rating: "5.0 ★ (24 đánh giá)",
      recentNote: "Minh Đức tiếp thu bài hình học rất tốt, đã biết vận dụng định lý vào chứng minh tứ giác nội tiếp.",
      progress: "88% mục tiêu kỳ 1",
    },
    {
      name: "Trần Hải Đăng",
      subject: "Tiếng Anh 9 Ôn Thi Tuyển Sinh",
      rating: "4.9 ★ (19 đánh giá)",
      recentNote: "Ngữ pháp phần câu gián tiếp còn nhầm lẫn lùi thì, tuần này thầy sẽ cho làm thêm 1 phiếu bài tập củng cố.",
      progress: "75% mục tiêu kỳ 1",
    },
  ];

  return (
    <div className="bg-white p-6 rounded-3xl border border-stone-200/80 shadow-xs">
      <div className="flex justify-between items-center mb-4">
        <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900">
          Gia sư trực tiếp đồng hành
        </h3>
        <Link href="/dashboard/tutors" className="text-xs text-emerald-700 font-bold hover:underline">
          Tìm thêm gia sư
        </Link>
      </div>

      <div className="space-y-4">
        {tutors.map((tutor, idx) => (
          <div
            key={idx}
            className="p-4 rounded-2xl border border-stone-200/80 hover:border-emerald-300 transition-colors space-y-3"
          >
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-3">
                <div className="size-10 rounded-xl bg-emerald-100 text-emerald-800 font-bold text-sm flex items-center justify-center shrink-0">
                  {tutor.name.charAt(0)}
                </div>
                <div>
                  <h4 className="text-xs sm:text-sm font-bold text-neutral-900">{tutor.name}</h4>
                  <div className="flex items-center gap-2 text-xs text-neutral-500">
                    <span className="font-semibold text-neutral-700">{tutor.subject}</span>
                    <span>·</span>
                    <span className="text-amber-600 font-medium">{tutor.rating}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 self-end sm:self-auto">
                <button
                  type="button"
                  className="px-3 py-1.5 border border-stone-200 hover:bg-stone-50 text-neutral-700 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors"
                >
                  <MessageSquare className="size-3.5" />
                  <span>Nhắn tin</span>
                </button>
                <Link
                  href="/dashboard/progress"
                  className="px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 rounded-xl text-xs font-bold transition-colors"
                >
                  Sổ theo dõi
                </Link>
              </div>
            </div>

            <div className="p-3 bg-stone-50 rounded-xl border border-stone-100 text-xs text-neutral-600 space-y-1">
              <div className="flex items-center gap-1.5 font-bold text-neutral-800">
                <Sparkles className="size-3 text-emerald-600" />
                <span>Nhận xét buổi học gần nhất:</span>
              </div>
              <p className="italic text-neutral-500 font-sans">{`"${tutor.recentNote}"`}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
