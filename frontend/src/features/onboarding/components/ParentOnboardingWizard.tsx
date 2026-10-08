"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { EduFitLogo } from "@/shared/ui/EduFitLogo";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/shared/ui/Toast";
import {
  Users,
  QrCode,
  CheckCircle2,
  Lightbulb,
  ArrowRight,
  Plus,
  BookOpen,
  School,
  Sparkles,
  Loader2,
  Trash2,
} from "lucide-react";

interface LinkedStudent {
  code: string;
  name: string;
  grade: string;
  school: string;
  subjects: string;
  goal: string;
}

export function ParentOnboardingWizard() {
  const router = useRouter();
  const { user, refreshUser } = useAuth();
  const { showToast } = useToast();

  const [inputCode, setInputCode] = useState<string>("EDU-849");
  const [showQrMock, setShowQrMock] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // List of connected children
  const [linkedStudents, setLinkedStudents] = useState<LinkedStudent[]>([
    {
      code: "EDU-849",
      name: "Trần Minh Đức",
      grade: "Lớp 9",
      school: "Trường THCS Nguyễn Du",
      subjects: "Toán, Tiếng Anh",
      goal: "Ôn thi vào lớp 10",
    },
  ]);

  // Additional child input dialog
  const [showAddModal, setShowAddModal] = useState(false);
  const [newCode, setNewCode] = useState("");
  const [newName, setNewName] = useState("");
  const [newGrade, setNewGrade] = useState("Lớp 8");

  const handleLinkNewChild = () => {
    if (!newCode.trim() || !newName.trim()) {
      showToast("Vui lòng nhập đầy đủ mã và tên con!", "error");
      return;
    }

    const newChild: LinkedStudent = {
      code: newCode.trim().toUpperCase(),
      name: newName.trim(),
      grade: newGrade,
      school: "Trường THCS Thực Nghiệm",
      subjects: "Toán, Ngữ văn",
      goal: "Củng cố kiến thức nền tảng",
    };

    setLinkedStudents((prev) => [...prev, newChild]);
    setNewCode("");
    setNewName("");
    setShowAddModal(false);
    showToast(`Đã ghép nối hồ sơ của ${newChild.name} thành công!`, "success");
  };

  const handleRemoveChild = (code: string) => {
    if (linkedStudents.length <= 1) {
      showToast("Bạn cần duy trì ít nhất 1 học sinh được ghép nối!", "error");
      return;
    }
    setLinkedStudents((prev) => prev.filter((c) => c.code !== code));
  };

  const handleComplete = async () => {
    if (linkedStudents.length === 0) {
      showToast("Vui lòng ghép nối ít nhất 1 hồ sơ học sinh!", "error");
      return;
    }

    setSubmitting(true);
    try {
      // 1. Lưu danh sách con em đã ghép nối vào localStorage để Parent Dashboard đọc linh hoạt
      localStorage.setItem("edufit_parent_linked_students", JSON.stringify(linkedStudents));
      localStorage.setItem("edufit_parent_onboarding_completed", "true");

      showToast("Thiết lập tài khoản phụ huynh thành công! Chào mừng bạn đến với EduFit.", "success");
      await refreshUser?.();
      router.push("/dashboard");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi hoàn tất thiết lập";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 font-sans flex flex-col justify-between">
      {/* 1. Header Bar */}
      <header className="bg-white border-b border-stone-200 sticky top-0 z-30 px-4 sm:px-8 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <EduFitLogo href="/" />
          <div className="hidden sm:flex items-center gap-2 pl-6 border-l border-stone-200 text-xs text-neutral-500">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 bg-emerald-50 text-emerald-800 font-semibold rounded-full">
              THIẾT LẬP NHANH
            </span>
            <span>Liên kết con để bắt đầu đồng hành</span>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <Link
            href="/dashboard"
            className="text-xs text-neutral-500 hover:text-neutral-900 transition-colors"
          >
            Bỏ qua để vào sau
          </Link>
        </div>
      </header>

      {/* 2. Main Container (Figma 142:8392) */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 sm:p-8 space-y-6">
        {/* Left/Top Tip Box */}
        <div className="bg-gradient-to-r from-emerald-50 via-teal-50 to-white rounded-3xl p-5 sm:p-6 border border-emerald-100 flex items-start gap-4">
          <div className="size-10 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
            <Lightbulb className="size-5" />
          </div>
          <div className="space-y-1">
            <h3 className="text-xs font-bold text-emerald-950 uppercase tracking-wider">
              Lấy mã liên kết ở đâu?
            </h3>
            <p className="text-xs text-emerald-900 leading-relaxed">
              Học sinh có thể mở <strong>EduFit &gt; Hồ sơ cá nhân &gt; chọn 'Liên kết phụ huynh'</strong> để lấy mã 6 ký tự hoặc mở mã QR để phụ huynh quét nhanh.
            </p>
          </div>
        </div>

        {/* Main Linking Card */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-stone-200/90 shadow-xs space-y-6">
          <div className="space-y-1">
            <span className="text-[11px] font-bold text-emerald-600 uppercase tracking-wider">
              BƯỚC 1: NHẬP MÃ GHÉP NỐI
            </span>
            <h2 className="font-heading text-2xl font-bold text-neutral-900">
              Nhập mã liên kết từ tài khoản học sinh của con
            </h2>
            <p className="text-xs text-neutral-500">
              Mã này giúp đồng bộ dữ liệu học tập, tiến độ bài tập và thông báo lịch học trực tiếp đến tài khoản của bạn.
            </p>
          </div>

          {/* Input Box for 6 characters */}
          <div className="p-5 bg-stone-50 border border-stone-200 rounded-3xl space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-neutral-700">
                MÃ GHÉP NỐI (6 KÝ TỰ HOẶC ĐỊNH DẠNG EDU-XXX)
              </label>
              <button
                type="button"
                onClick={() => setShowQrMock(!showQrMock)}
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700 hover:text-emerald-800 cursor-pointer"
              >
                <QrCode className="size-4" />
                <span>Quét mã QR</span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="text"
                value={inputCode}
                onChange={(e) => setInputCode(e.target.value.toUpperCase())}
                placeholder="VD: EDU-849"
                className="w-full sm:w-64 px-4 py-3 bg-white border border-stone-300 rounded-2xl text-center text-lg font-mono font-bold tracking-widest text-neutral-900 uppercase focus:outline-hidden focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
              />
              <span className="text-xs text-emerald-600 font-semibold px-2">
                ✓ Đã tìm thấy
              </span>
            </div>

            {showQrMock && (
              <div className="p-4 bg-white border border-emerald-200 rounded-2xl flex items-center justify-between animate-in fade-in">
                <div className="flex items-center gap-3">
                  <div className="size-10 bg-emerald-50 rounded-xl flex items-center justify-center text-emerald-600 font-bold">
                    <QrCode className="size-5" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-neutral-800">Mã QR Scanner Simulator</p>
                    <p className="text-[11px] text-neutral-400">Camera đã quét tự động mã EDU-849</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setShowQrMock(false)}
                  className="px-3 py-1 bg-stone-100 hover:bg-stone-200 text-neutral-700 text-xs font-semibold rounded-lg"
                >
                  Xong
                </button>
              </div>
            )}
          </div>

          {/* List of Matched Children Cards */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-neutral-700">
                Hồ sơ học sinh đã ghép nối ({linkedStudents.length})
              </label>
              <button
                type="button"
                onClick={() => setShowAddModal(true)}
                className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-full text-xs font-bold transition-colors cursor-pointer"
              >
                <Plus className="size-3.5" />
                <span>Thêm một con khác</span>
              </button>
            </div>

            <div className="space-y-2">
              {linkedStudents.map((child) => (
                <div
                  key={child.code}
                  className="p-5 bg-gradient-to-br from-stone-50 to-white border border-stone-200 rounded-3xl flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="flex items-start gap-4">
                    <div className="size-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white font-bold text-lg flex items-center justify-center shadow-xs shrink-0">
                      {child.name[0]}
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h4 className="font-bold text-sm text-neutral-900">{child.name}</h4>
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 bg-emerald-100 text-emerald-800 font-bold rounded-full text-[10px]">
                          <CheckCircle2 className="size-3" />
                          Mã hợp lệ ✓
                        </span>
                      </div>
                      <p className="text-xs text-neutral-500 flex items-center gap-1.5">
                        <School className="size-3.5 text-neutral-400" />
                        <span>
                          Học sinh {child.grade} • {child.school}
                        </span>
                      </p>
                      <p className="text-xs text-neutral-600 flex items-center gap-1.5">
                        <BookOpen className="size-3.5 text-neutral-400" />
                        <span>
                          Môn quan tâm: <strong>{child.subjects}</strong> • Mục tiêu:{" "}
                          <strong>{child.goal}</strong>
                        </span>
                      </p>
                    </div>
                  </div>

                  {linkedStudents.length > 1 && (
                    <button
                      type="button"
                      onClick={() => handleRemoveChild(child.code)}
                      className="p-2 text-stone-400 hover:text-rose-500 rounded-xl hover:bg-rose-50 transition-colors self-end sm:self-center"
                    >
                      <Trash2 className="size-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Modal to add another child */}
          {showAddModal && (
            <div className="p-5 bg-stone-50 border border-stone-200 rounded-3xl space-y-4 animate-in fade-in">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-neutral-900">
                  Thêm hồ sơ con thứ {linkedStudents.length + 1}
                </h4>
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="text-xs text-neutral-400 hover:text-neutral-700"
                >
                  Đóng
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-neutral-600 mb-1">
                    Mã liên kết
                  </label>
                  <input
                    type="text"
                    value={newCode}
                    onChange={(e) => setNewCode(e.target.value)}
                    placeholder="VD: EDU-721"
                    className="w-full px-3 py-2 text-xs bg-white border border-stone-200 rounded-xl uppercase font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-neutral-600 mb-1">
                    Tên học sinh
                  </label>
                  <input
                    type="text"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    placeholder="VD: Trần Phương Thảo"
                    className="w-full px-3 py-2 text-xs bg-white border border-stone-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-neutral-600 mb-1">
                    Khối lớp
                  </label>
                  <select
                    value={newGrade}
                    onChange={(e) => setNewGrade(e.target.value)}
                    className="w-full px-3 py-2 text-xs bg-white border border-stone-200 rounded-xl"
                  >
                    <option value="Lớp 6">Lớp 6</option>
                    <option value="Lớp 7">Lớp 7</option>
                    <option value="Lớp 8">Lớp 8</option>
                    <option value="Lớp 9">Lớp 9</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3 py-1.5 text-xs text-neutral-600 hover:bg-stone-100 rounded-xl"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  onClick={handleLinkNewChild}
                  className="px-4 py-1.5 bg-emerald-600 text-white text-xs font-bold rounded-xl hover:bg-emerald-700 shadow-xs"
                >
                  Xác nhận liên kết
                </button>
              </div>
            </div>
          )}

          {/* Action Footer */}
          <div className="flex items-center justify-end pt-4 border-t border-stone-100">
            <button
              type="button"
              disabled={submitting}
              onClick={handleComplete}
              className="inline-flex items-center gap-2 px-7 py-3 bg-emerald-600 hover:bg-emerald-700 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all disabled:opacity-50 cursor-pointer"
            >
              {submitting ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <ArrowRight className="size-4" />
              )}
              <span>{submitting ? "Đang hoàn tất..." : "Hoàn tất & vào Dashboard →"}</span>
            </button>
          </div>
        </div>
      </main>

      {/* 3. Footer */}
      <footer className="border-t border-stone-200 py-4 px-6 text-center text-[11px] text-neutral-400">
        © 2026 EduFit Platform. Đồng hành cùng phụ huynh và học sinh trên con đường tri thức.
      </footer>
    </div>
  );
}
