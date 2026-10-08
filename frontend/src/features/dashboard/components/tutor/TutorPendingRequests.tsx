"use client";

import React from "react";
import Link from "next/link";

export function TutorPendingRequests() {
  const requests = [
    {
      name: "Nguyễn Hải Long (Lớp 11)",
      subject: "Toán 11 & Hình Không Gian",
      note: "Muốn học 2 buổi/tuần, cần lấy lại gốc hình không gian trước kỳ thi giữa kỳ.",
      match: "96% AI Match",
    },
    {
      name: "Phạm Thùy Linh (Lớp 9)",
      subject: "Toán 9 Ôn Thi Vào 10 Chuyên",
      note: "Phụ huynh gửi lời mời kết nối trực tiếp từ gợi ý của nền tảng.",
      match: "92% AI Match",
    },
  ];

  return (
    <div className="bg-white p-6 rounded-3xl border border-stone-200/80 shadow-xs">
      <div className="flex justify-between items-center mb-4">
        <div className="flex items-center gap-2">
          <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900">
            Yêu cầu kết nối đang chờ phản hồi
          </h3>
          <span className="size-5 bg-amber-400 text-neutral-950 font-bold text-[10px] rounded-full flex items-center justify-center">
            2
          </span>
        </div>
        <Link href="/dashboard/requests" className="text-xs text-sky-600 font-bold hover:underline">
          Xem tất cả
        </Link>
      </div>

      <div className="space-y-3">
        {requests.map((req, idx) => (
          <div
            key={idx}
            className="p-4 rounded-2xl border border-stone-200 hover:border-amber-300 transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-3"
          >
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-xs sm:text-sm font-bold text-neutral-900">{req.name}</h4>
                <span className="px-2 py-0.5 bg-amber-50 text-amber-900 text-[10px] font-bold rounded-md border border-amber-200">
                  {req.match}
                </span>
              </div>
              <p className="text-xs font-semibold text-neutral-700 mt-0.5">{req.subject}</p>
              <p className="text-xs text-neutral-500 mt-1 italic font-sans">{`"${req.note}"`}</p>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                className="px-3 py-1.5 border border-stone-200 hover:bg-stone-50 text-neutral-700 rounded-xl text-xs font-bold transition-colors"
              >
                Chi tiết
              </button>
              <button
                type="button"
                className="px-3.5 py-1.5 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-bold shadow-2xs transition-colors"
              >
                Nhận lớp
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
