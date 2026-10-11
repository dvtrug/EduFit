"use client";

import React, { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/shared/ui/Toast";
import {
  Users,
  Save,
  Loader2,
  MapPin,
  Phone,
  User,
  Mail,
  ShieldCheck,
  CheckCircle2,
  BookOpen,
} from "lucide-react";

export function ParentProfileForm() {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [fullName, setFullName] = useState(user?.fullName || "Trần Văn Hưng");
  const [phoneNumber, setPhoneNumber] = useState("0988 123 456");
  const [area, setArea] = useState("Cầu Giấy, Hà Nội");
  const [childGrade, setChildGrade] = useState("Lớp 9 (Ôn thi vào 10)");
  const [learningNote, setLearningNote] = useState(
    "Tìm gia sư Toán và Tiếng Anh có kinh nghiệm luyện thi vào các trường THPT Chuyên tại Hà Nội."
  );
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);

    try {
      // Giả lập lưu cấu hình hồ sơ phụ huynh và đồng bộ state local
      await new Promise((resolve) => setTimeout(resolve, 600));
      showToast("Cập nhật thông tin phụ huynh thành công!", "success");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi lưu thông tin";
      showToast(message, "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* 1. Header Card & Thông Tin Chung */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-100">
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-900 rounded-full text-xs font-bold mb-2">
              <Users className="size-3.5 text-emerald-600" />
              <span>Hồ Sơ Phụ Huynh (Parent Profile)</span>
            </div>
            <h3 className="font-heading text-xl font-bold text-neutral-900">
              Thông tin liên hệ & Yêu cầu gia sư cho con
            </h3>
            <p className="text-xs text-neutral-500 mt-0.5">
              Giúp gia sư nắm bắt nhu cầu học tập và liên hệ thuận tiện với gia đình
            </p>
          </div>

          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all disabled:opacity-50 shrink-0 cursor-pointer"
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
            <span>{saving ? "Đang lưu..." : "Lưu thay đổi"}</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Họ tên phụ huynh */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Họ và tên phụ huynh <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <User className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="VD: Trần Văn Hưng"
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 text-neutral-900 font-medium"
              />
            </div>
          </div>

          {/* Email tài khoản (read-only) */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Email đăng nhập hệ thống
            </label>
            <div className="relative">
              <Mail className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                disabled
                value={user?.email || "parent.demo@edufit.vn"}
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-100 border border-stone-200 rounded-xl text-neutral-500 font-medium cursor-not-allowed"
              />
            </div>
          </div>

          {/* Số điện thoại liên hệ */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Số điện thoại liên hệ <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <Phone className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="tel"
                required
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="VD: 0988 123 456"
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 text-neutral-900 font-medium"
              />
            </div>
          </div>

          {/* Khu vực sinh sống */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Khu vực sinh sống / Địa chỉ học tập <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <MapPin className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={area}
                onChange={(e) => setArea(e.target.value)}
                placeholder="VD: Cầu Giấy, Hà Nội"
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 text-neutral-900 font-medium"
              />
            </div>
          </div>
        </div>
      </div>

      {/* 2. Thông Tin Nhu Cầu Tìm Gia Sư Cho Con */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs space-y-5">
        <div className="flex items-center gap-2 pb-3 border-b border-stone-100">
          <BookOpen className="size-4 text-emerald-600" />
          <h4 className="text-sm font-bold text-neutral-900">
            Kế hoạch học tập của học sinh
          </h4>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Cấp lớp học của con
            </label>
            <input
              type="text"
              value={childGrade}
              onChange={(e) => setChildGrade(e.target.value)}
              placeholder="VD: Lớp 9 (Chuẩn bị thi vào 10)"
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 text-neutral-900 font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Mục tiêu & Ghi chú dành cho gia sư
            </label>
            <textarea
              rows={3}
              value={learningNote}
              onChange={(e) => setLearningNote(e.target.value)}
              placeholder="Mô tả tính cách học sinh, học lực hiện tại, thời gian mong muốn học trong tuần..."
              className="w-full p-3.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 text-neutral-900 font-medium resize-none"
            />
          </div>
        </div>
      </div>

      {/* 3. Liên Kết Tài Khoản Con Em (Parent - Student Link) */}
      <div className="bg-stone-50/80 p-6 rounded-3xl border border-stone-200 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShieldCheck className="size-4 text-emerald-600" />
            <h4 className="text-xs font-bold text-neutral-800">
              Trạng thái liên kết hồ sơ con em (Parent-Student Link)
            </h4>
          </div>
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-emerald-100/80 text-emerald-800 rounded-full text-[10px] font-bold">
            <CheckCircle2 className="size-3 text-emerald-600" />
            Đã đồng bộ
          </span>
        </div>
        <p className="text-[11px] text-neutral-500 leading-relaxed">
          Tài khoản phụ huynh được liên kết tự động với hồ sơ học viên <strong>Trần Minh Anh (student.demo@edufit.vn)</strong> theo mã định danh UC1.4. Phụ huynh có thể theo dõi tiến độ buổi học và lịch dạy của gia sư tại Bảng điều khiển.
        </p>
      </div>
    </form>
  );
}
