"use client";

import React, { useState } from "react";
import { Globe, Clock, Coins, Check } from "lucide-react";
import { useToast } from "@/shared/ui/Toast";

export function DisplaySettingsTab() {
  const { showToast } = useToast();

  const [language, setLanguage] = useState("vi");
  const [dateFormat, setDateFormat] = useState("DD/MM/YYYY");
  const [currency, setCurrency] = useState("VND");
  const [saving, setSaving] = useState(false);

  const handleSave = () => {
    setSaving(true);
    setTimeout(() => {
      setSaving(false);
      showToast("Đã lưu tùy chọn hiển thị thành công!", "success");
    }, 400);
  };

  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-stone-100">
        <h2 className="font-heading text-xl font-bold text-neutral-900">
          Tùy chọn hiển thị & Khu vực
        </h2>
        <p className="text-xs text-neutral-500 mt-1">
          Cài đặt ngôn ngữ, định dạng ngày tháng và tiền tệ sử dụng trong nền tảng EduFit.
        </p>
      </div>

      <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/90 shadow-xs space-y-5 max-w-xl">
        {/* 1. Ngôn ngữ */}
        <div>
          <label className="flex items-center gap-2 text-xs font-semibold text-neutral-700 mb-2">
            <Globe className="size-4 text-amber-600" />
            <span>Ngôn ngữ giao diện</span>
          </label>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="w-full px-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-800"
          >
            <option value="vi">Tiếng Việt (Mặc định)</option>
            <option value="en">English (Coming soon)</option>
          </select>
        </div>

        {/* 2. Tiền tệ */}
        <div>
          <label className="flex items-center gap-2 text-xs font-semibold text-neutral-700 mb-2">
            <Coins className="size-4 text-emerald-600" />
            <span>Đơn vị tiền tệ hiển thị</span>
          </label>
          <select
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
            className="w-full px-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-800"
          >
            <option value="VND">VNĐ (Việt Nam Đồng)</option>
            <option value="USD">USD ($)</option>
          </select>
        </div>

        {/* 3. Định dạng ngày */}
        <div>
          <label className="flex items-center gap-2 text-xs font-semibold text-neutral-700 mb-2">
            <Clock className="size-4 text-sky-600" />
            <span>Định dạng ngày & giờ</span>
          </label>
          <select
            value={dateFormat}
            onChange={(e) => setDateFormat(e.target.value)}
            className="w-full px-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-800"
          >
            <option value="DD/MM/YYYY">DD/MM/YYYY (Ví dụ: 12/10/2026)</option>
            <option value="YYYY-MM-DD">YYYY-MM-DD (Ví dụ: 2026-10-12)</option>
          </select>
          <p className="text-[11px] text-neutral-400 mt-1 pl-1">
            Múi giờ chuẩn: Asia/Ho_Chi_Minh (GMT+7)
          </p>
        </div>

        <div className="pt-2">
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center gap-2 px-6 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all cursor-pointer"
          >
            <Check className="size-3.5" />
            <span>{saving ? "Đang lưu..." : "Lưu tùy chọn"}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
