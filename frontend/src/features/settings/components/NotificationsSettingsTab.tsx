"use client";

import React, { useState } from "react";
import { Bell, Mail, Smartphone, ShieldCheck, Check } from "lucide-react";
import { useToast } from "@/shared/ui/Toast";

export function NotificationsSettingsTab() {
  const { showToast } = useToast();

  const [settings, setSettings] = useState({
    emailLessonReminder: true,
    emailNewRequests: true,
    emailWeeklyDigest: false,
    inAppSound: true,
    inAppDesktopAlerts: true,
    kycStatusAlerts: true,
  });

  const [saving, setSaving] = useState(false);

  const toggle = (key: keyof typeof settings) => {
    setSettings((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleSave = () => {
    setSaving(true);
    setTimeout(() => {
      setSaving(false);
      showToast("Đã lưu tùy chọn thông báo thành công!", "success");
    }, 400);
  };

  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-stone-100">
        <h2 className="font-heading text-xl font-bold text-neutral-900">
          Cài đặt thông báo
        </h2>
        <p className="text-xs text-neutral-500 mt-1">
          Chọn loại thông báo bạn muốn nhận qua Email và trên trình duyệt ứng dụng.
        </p>
      </div>

      {/* 1. Email Notifications */}
      <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/90 shadow-xs space-y-4">
        <div className="flex items-center gap-2 text-xs font-bold text-neutral-800 pb-2 border-b border-stone-100">
          <Mail className="size-4 text-sky-600" />
          <span>Thông báo qua Thư điện tử (Email)</span>
        </div>

        <div className="divide-y divide-stone-100">
          <div className="py-3.5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold text-neutral-800">
                Nhắc nhở lịch học sắp diễn ra
              </p>
              <p className="text-[11px] text-neutral-500 mt-0.5">
                Nhận email nhắc nhở trước 24 giờ và 1 giờ khi buổi học bắt đầu.
              </p>
            </div>
            <button
              type="button"
              onClick={() => toggle("emailLessonReminder")}
              className={`w-11 h-6 rounded-full transition-colors p-0.5 shrink-0 cursor-pointer ${
                settings.emailLessonReminder ? "bg-amber-500" : "bg-stone-200"
              }`}
            >
              <div
                className={`size-5 rounded-full bg-white shadow-xs transition-transform ${
                  settings.emailLessonReminder ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          <div className="py-3.5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold text-neutral-800">
                Yêu cầu kết nối và tin nhắn mới
              </p>
              <p className="text-[11px] text-neutral-500 mt-0.5">
                Thông báo ngay khi có học viên gửi đề nghị học hoặc gia sư xác nhận buổi dạy.
              </p>
            </div>
            <button
              type="button"
              onClick={() => toggle("emailNewRequests")}
              className={`w-11 h-6 rounded-full transition-colors p-0.5 shrink-0 cursor-pointer ${
                settings.emailNewRequests ? "bg-amber-500" : "bg-stone-200"
              }`}
            >
              <div
                className={`size-5 rounded-full bg-white shadow-xs transition-transform ${
                  settings.emailNewRequests ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          <div className="py-3.5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold text-neutral-800">
                Bản tin tổng hợp tiến độ hàng tuần
              </p>
              <p className="text-[11px] text-neutral-500 mt-0.5">
                Email tổng kết số buổi học đã hoàn thành và nhận xét đánh giá vào mỗi sáng thứ Hai.
              </p>
            </div>
            <button
              type="button"
              onClick={() => toggle("emailWeeklyDigest")}
              className={`w-11 h-6 rounded-full transition-colors p-0.5 shrink-0 cursor-pointer ${
                settings.emailWeeklyDigest ? "bg-amber-500" : "bg-stone-200"
              }`}
            >
              <div
                className={`size-5 rounded-full bg-white shadow-xs transition-transform ${
                  settings.emailWeeklyDigest ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* 2. In-App Alerts */}
      <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/90 shadow-xs space-y-4">
        <div className="flex items-center gap-2 text-xs font-bold text-neutral-800 pb-2 border-b border-stone-100">
          <Bell className="size-4 text-amber-600" />
          <span>Thông báo trên trình duyệt (In-App)</span>
        </div>

        <div className="divide-y divide-stone-100">
          <div className="py-3.5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold text-neutral-800">
                Âm thanh thông báo
              </p>
              <p className="text-[11px] text-neutral-500 mt-0.5">
                Phát âm thanh nhẹ khi có tin nhắn hoặc thông báo mới được gửi đến.
              </p>
            </div>
            <button
              type="button"
              onClick={() => toggle("inAppSound")}
              className={`w-11 h-6 rounded-full transition-colors p-0.5 shrink-0 cursor-pointer ${
                settings.inAppSound ? "bg-amber-500" : "bg-stone-200"
              }`}
            >
              <div
                className={`size-5 rounded-full bg-white shadow-xs transition-transform ${
                  settings.inAppSound ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>

          <div className="py-3.5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold text-neutral-800">
                Trạng thái kiểm duyệt hồ sơ & KYC
              </p>
              <p className="text-[11px] text-neutral-500 mt-0.5">
                Cập nhật tức thì khi hồ sơ gia sư hoặc bằng cấp của bạn được phê duyệt.
              </p>
            </div>
            <button
              type="button"
              onClick={() => toggle("kycStatusAlerts")}
              className={`w-11 h-6 rounded-full transition-colors p-0.5 shrink-0 cursor-pointer ${
                settings.kycStatusAlerts ? "bg-amber-500" : "bg-stone-200"
              }`}
            >
              <div
                className={`size-5 rounded-full bg-white shadow-xs transition-transform ${
                  settings.kycStatusAlerts ? "translate-x-5" : "translate-x-0"
                }`}
              />
            </button>
          </div>
        </div>
      </div>

      <div className="pt-2">
        <button
          type="button"
          onClick={handleSave}
          disabled={saving}
          className="inline-flex items-center gap-2 px-6 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all cursor-pointer"
        >
          <Check className="size-3.5" />
          <span>{saving ? "Đang lưu..." : "Lưu cài đặt thông báo"}</span>
        </button>
      </div>
    </div>
  );
}
