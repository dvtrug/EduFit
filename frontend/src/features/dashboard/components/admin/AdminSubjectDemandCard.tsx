"use client";

import React from "react";

export function AdminSubjectDemandCard() {
  const subjects = [
    { name: "Toán Cấp 2 & Ôn Thi Vào 10", percentage: "42%", count: "528 yêu cầu", barColor: "bg-purple-700", textColor: "text-purple-700" },
    { name: "Tiếng Anh & IELTS Foundation", percentage: "31%", count: "385 yêu cầu", barColor: "bg-indigo-600", textColor: "text-indigo-600" },
    { name: "Ngữ Văn Lớp 9", percentage: "18%", count: "224 yêu cầu", barColor: "bg-sky-600", textColor: "text-sky-600" },
  ];

  return (
    <div className="bg-white p-6 rounded-3xl border border-stone-200/80 shadow-xs">
      <h3 className="font-heading text-base sm:text-lg font-bold text-neutral-900 mb-4">
        Nhu cầu kết nối theo bộ môn (Top Demand)
      </h3>
      <div className="space-y-3">
        {subjects.map((item, idx) => (
          <div key={idx}>
            <div className="flex justify-between text-xs font-bold mb-1">
              <span className="text-neutral-800">{item.name}</span>
              <span className={item.textColor}>
                {item.percentage} ({item.count})
              </span>
            </div>
            <div className="w-full h-2 bg-stone-100 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full ${item.barColor}`}
                style={{ width: item.percentage }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
