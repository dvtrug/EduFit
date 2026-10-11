"use client";

import React from "react";
import Link from "next/link";
import { ShieldCheck, ArrowRight, CheckCircle2 } from "lucide-react";

interface AdminKycQueueCardProps {
  pendingCount?: number;
}

export function AdminKycQueueCard({ pendingCount = 0 }: AdminKycQueueCardProps) {
  const isAllClear = pendingCount === 0;

  return (
    <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs space-y-3">
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <ShieldCheck className="size-4 text-purple-700" />
          <h4 className="text-xs font-bold text-neutral-900 uppercase tracking-wider">
            Hàng đợi KYC Gia sư
          </h4>
        </div>
        {isAllClear ? (
          <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 font-bold text-[10px] rounded-full flex items-center gap-1 border border-emerald-200">
            <CheckCircle2 className="size-3" /> Hoàn tất
          </span>
        ) : (
          <span className="size-5 bg-red-500 text-white font-bold text-[10px] rounded-full flex items-center justify-center">
            {pendingCount}
          </span>
        )}
      </div>

      <p className="text-xs text-neutral-500 leading-relaxed font-sans">
        {isAllClear
          ? "Hiện không có hồ sơ gia sư nào tồn đọng. Mọi chứng chỉ và văn bằng đã được thẩm định."
          : `Hiện có ${pendingCount} hồ sơ gia sư mới nộp giấy tờ (bằng tốt nghiệp, chứng chỉ IELTS) đang chờ phê duyệt.`}
      </p>

      <Link
        href="/dashboard/kyc-queue"
        className="w-full py-2.5 px-3 bg-purple-50 hover:bg-purple-100 text-purple-900 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
      >
        <span>{isAllClear ? "Mở hàng đợi kiểm duyệt" : "Vào duyệt hồ sơ ngay"}</span>
        <ArrowRight className="size-3.5" />
      </Link>
    </div>
  );
}
