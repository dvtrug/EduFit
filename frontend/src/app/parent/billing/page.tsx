"use client";

import React from "react";
import Link from "next/link";
import {
  CreditCard,
  ArrowLeft,
  ShieldCheck,
  Building,
  CheckCircle2,
  Clock,
  PhoneCall,
  Calendar,
} from "lucide-react";

export default function ParentBillingPage() {
  return (
    <div className="max-w-4xl mx-auto space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div>
        <Link
          href="/parent/dashboard"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-neutral-900 transition-colors mb-2"
        >
          <ArrowLeft className="size-3.5" />
          <span>Quay lại Tổng quan</span>
        </Link>
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight flex items-center gap-2.5">
          <CreditCard className="size-7 text-emerald-600" />
          Gói học & Thanh toán học phí
        </h1>
        <p className="text-xs sm:text-sm text-neutral-500 mt-1">
          Theo dõi học phí các lớp học 1-1 của con em và thông tin phương thức thanh toán an toàn.
        </p>
      </div>

      {/* Sandbox/Notice Card */}
      <div className="p-6 rounded-3xl bg-amber-50/70 border border-amber-200 text-amber-900 space-y-3">
        <div className="flex items-center gap-2 text-amber-800 font-bold text-sm">
          <Clock className="size-5 text-amber-600" />
          <span>Thông báo: Phân hệ Cổng thanh toán trực tuyến đang được nâng cấp</span>
        </div>
        <p className="text-xs leading-relaxed text-amber-900/80">
          EduFit đang hoàn thiện tích hợp cổng thanh toán tự động (VNPAY & MoMo Sandbox). Hiện tại,
          học phí các lớp học 1-1 sẽ được thanh toán trực tiếp theo thỏa thuận giữa phụ huynh và gia sư
          hoặc qua tài khoản ngân hàng chuyên dụng của trung tâm.
        </p>
      </div>

      {/* Bank Transfer Info Card */}
      <div className="p-6 bg-white rounded-3xl border border-stone-200 shadow-xs space-y-5">
        <h2 className="text-base font-bold text-neutral-900 flex items-center gap-2">
          <Building className="size-5 text-emerald-600" />
          <span>Phương thức thanh toán qua chuyển khoản ngân hàng</span>
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-stone-50 border border-stone-200/70 space-y-2">
            <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
              Tài khoản nhận thanh toán
            </span>
            <div className="space-y-1">
              <p className="font-bold text-neutral-900">Ngân hàng TMCP Quân Đội (MB Bank)</p>
              <p className="text-neutral-600">Số tài khoản: <span className="font-mono font-bold text-neutral-900">0333 888 999</span></p>
              <p className="text-neutral-600">Chủ tài khoản: <span className="font-bold text-neutral-900">EDUFIT EDUCATION CORP</span></p>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-stone-50 border border-stone-200/70 space-y-2">
            <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
              Cú pháp chuyển khoản
            </span>
            <div className="space-y-1">
              <p className="font-mono font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1.5 rounded-lg border border-emerald-200 inline-block">
                HOCPHI [SĐT Phụ Huynh] [Tên Con]
              </p>
              <p className="text-[11px] text-neutral-500 mt-1">
                Hệ thống tự động kích hoạt gói buổi học trong vòng 15 phút sau khi nhận giao dịch.
              </p>
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-stone-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-xs text-neutral-500">
            <ShieldCheck className="size-4 text-emerald-600" />
            <span>Mọi khoản học phí đều được lưu ký an toàn cho tới khi buổi học hoàn thành.</span>
          </div>

          <div className="flex items-center gap-2.5">
            <Link
              href="/parent/schedule"
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-stone-200 hover:bg-stone-50 text-neutral-700 text-xs font-bold transition-colors"
            >
              <Calendar className="size-3.5" />
              <span>Xem lịch học các con</span>
            </Link>
            <Link
              href="/parent/dashboard"
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-colors shadow-2xs"
            >
              <span>Về Bảng điều khiển</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
