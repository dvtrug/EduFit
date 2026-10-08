"use client";

import React from "react";
import Link from "next/link";
import { ShieldCheck, ArrowRight } from "lucide-react";

export function AdminKycQueueCard() {
  return (
    <div className="bg-white p-5 rounded-2xl border border-stone-200/80 shadow-xs space-y-3">
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <ShieldCheck className="size-4 text-purple-700" />
          <h4 className="text-xs font-bold text-neutral-900 uppercase tracking-wider">
            Hàng đợi KYC Gia sư
          </h4>
        </div>
        <span className="size-5 bg-red-500 text-white font-bold text-[10px] rounded-full flex items-center justify-center">
          8
        </span>
      </div>
      <p className="text-xs text-neutral-500">
        Hiện có 8 hồ sơ gia sư mới nộp giấy tờ (bằng tốt nghiệp, chứng chỉ IELTS) đang chờ phê duyệt.
      </p>
      <Link
        href="/dashboard/kyc-queue"
        className="w-full py-2.5 px-3 bg-purple-50 hover:bg-purple-100 text-purple-900 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-colors"
      >
        <span>Vào trang kiểm duyệt KYC</span>
        <ArrowRight className="size-3.5" />
      </Link>
    </div>
  );
}
