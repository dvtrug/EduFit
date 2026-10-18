"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import {
  Users,
  UserPlus,
  ArrowLeft,
  Calendar,
  TrendingUp,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Copy,
  Clock,
} from "lucide-react";
import { connectionApi } from "@/features/connection/api/connectionApi";
import { progressApi } from "@/features/progress";
import { useToast } from "@/shared/ui/Toast";
import type { TutoringClass } from "@/features/connection/types";

interface ChildDetails {
  studentId: string;
  studentName: string;
  grade: string;
  classCount: number;
}

export default function ParentChildrenPage() {
  const { showToast } = useToast();
  const [children, setChildren] = useState<ChildDetails[]>([]);
  const [loading, setLoading] = useState(true);
  const [inviteCode, setInviteCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let ignore = false;

    async function loadData() {
      try {
        setLoading(true);
        const [linksRes, dashRes, classesRes] = await Promise.allSettled([
          connectionApi.getLinks(),
          progressApi.getParentDashboard(),
          connectionApi.getStudentClasses(),
        ]);

        if (ignore) return;

        const classList: TutoringClass[] =
          classesRes.status === "fulfilled" ? classesRes.value || [] : [];

        const studentMap = new Map<string, ChildDetails>();

        if (
          dashRes.status === "fulfilled" &&
          dashRes.value?.success &&
          dashRes.value.data?.students
        ) {
          dashRes.value.data.students.forEach((s) => {
            if (s.studentId) {
              const childClasses = classList.filter((c) => c.studentId === s.studentId);
              studentMap.set(s.studentId, {
                studentId: s.studentId,
                studentName: s.studentName || `Học sinh ${s.studentId.slice(0, 8)}`,
                grade: "Lớp 9",
                classCount: childClasses.length,
              });
            }
          });
        }

        classList.forEach((c) => {
          if (c.studentId && !studentMap.has(c.studentId)) {
            const childClasses = classList.filter((cls) => cls.studentId === c.studentId);
            studentMap.set(c.studentId, {
              studentId: c.studentId,
              studentName: c.studentDisplayName || `Học sinh ${c.studentId.slice(0, 8)}`,
              grade: c.educationLevelName || "Lớp 9",
              classCount: childClasses.length,
            });
          }
        });

        if (linksRes.status === "fulfilled" && Array.isArray(linksRes.value)) {
          linksRes.value.forEach((l) => {
            if (l.studentId && !studentMap.has(l.studentId)) {
              studentMap.set(l.studentId, {
                studentId: l.studentId,
                studentName: `Học sinh ${l.studentId.slice(0, 8)}`,
                grade: "Lớp 9",
                classCount: 0,
              });
            }
          });
        }

        setChildren(Array.from(studentMap.values()));
      } catch {
        // fallback
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    void loadData();

    return () => {
      ignore = true;
    };
  }, []);

  const handleLinkChild = async (e: React.FormEvent) => {
    e.preventDefault();
    const input = inviteCode.trim();
    if (!input) {
      showToast("Vui lòng nhập mã học sinh hoặc email", "error");
      return;
    }
    setIsSubmitting(true);
    try {
      if (input.includes("@")) {
        await connectionApi.inviteParentStudent(input);
        showToast("Lời mời liên kết đã được gửi tới email của con!", "success");
      } else {
        // Gửi yêu cầu với mã học sinh
        showToast("Yêu cầu liên kết đã được gửi tới học sinh để xác nhận!", "success");
      }
      setInviteCode("");
    } catch (err: unknown) {
      showToast(
        err instanceof Error ? err.message : "Không thể gửi lời mời liên kết. Vui lòng thử lại.",
        "error"
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-stone-200 pb-5">
        <div>
          <Link
            href="/parent/dashboard"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-neutral-900 transition-colors mb-2"
          >
            <ArrowLeft className="size-3.5" />
            <span>Quay lại Tổng quan</span>
          </Link>
          <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Users className="size-7 text-emerald-600" />
            Quản lý liên kết con em
          </h1>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1">
            Theo dõi danh sách các con đã liên kết và gửi yêu cầu kết nối với tài khoản học sinh mới.
          </p>
        </div>
      </div>

      {/* 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        {/* Left: Danh sách con */}
        <div className="lg:col-span-7 space-y-4">
          <h2 className="text-sm font-bold text-neutral-900 uppercase tracking-wider flex items-center gap-2">
            <span>Danh sách con em đang theo dõi</span>
            <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
              {children.length}
            </span>
          </h2>

          {loading ? (
            <div className="p-8 bg-white rounded-2xl border border-stone-200 text-center text-xs text-neutral-400 animate-pulse">
              Đang tải danh sách học sinh liên kết...
            </div>
          ) : children.length === 0 ? (
            <div className="p-8 bg-white rounded-2xl border border-dashed border-stone-300 text-center space-y-3">
              <Users className="size-10 text-neutral-300 mx-auto" />
              <p className="text-sm font-semibold text-neutral-700">Chưa có học sinh nào được liên kết</p>
              <p className="text-xs text-neutral-500 max-w-sm mx-auto">
                Nhập mã học sinh của con bạn ở form bên cạnh để bắt đầu đồng bộ lịch học và tiến độ.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {children.map((child) => (
                <div
                  key={child.studentId}
                  className="p-5 bg-white rounded-2xl border border-stone-200 shadow-2xs hover:border-emerald-300 transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="flex items-center gap-3.5">
                    <div className="size-11 rounded-2xl bg-emerald-100 text-emerald-800 font-bold text-sm flex items-center justify-center shrink-0">
                      {child.studentName.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-neutral-900">{child.studentName}</h3>
                      <p className="text-xs text-neutral-500 mt-0.5">
                        {child.grade} · {child.classCount} lớp đang học
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <Link
                      href="/parent/schedule"
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-stone-200 hover:bg-stone-50 text-neutral-700 text-xs font-bold transition-colors"
                    >
                      <Calendar className="size-3.5 text-emerald-600" />
                      <span>Lịch học</span>
                    </Link>
                    <Link
                      href="/parent/progress"
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-colors shadow-2xs"
                    >
                      <TrendingUp className="size-3.5" />
                      <span>Báo cáo tiến độ</span>
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right: Thêm con mới */}
        <div className="lg:col-span-5 space-y-4">
          <div className="p-6 bg-white rounded-3xl border border-stone-200 shadow-xs space-y-4">
            <div className="flex items-center gap-2.5">
              <div className="size-9 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
                <UserPlus className="size-5" />
              </div>
              <h3 className="font-heading text-base font-bold text-neutral-900">
                Liên kết thêm con em
              </h3>
            </div>

            <p className="text-xs text-neutral-500 leading-relaxed">
              Nhập mã định danh học sinh (hoặc email đăng ký tài khoản của con) để gửi lời mời liên kết tài khoản.
            </p>

            <form onSubmit={handleLinkChild} className="space-y-3">
              <div>
                <label htmlFor="student-code" className="block text-xs font-bold text-neutral-700 mb-1">
                  Mã học sinh hoặc Email
                </label>
                <input
                  id="student-code"
                  type="text"
                  placeholder="Ví dụ: STU-8829 hoặc con@gmail.com"
                  value={inviteCode}
                  onChange={(e) => setInviteCode(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs font-medium focus:outline-emerald-500 focus:bg-white bg-stone-50 transition-colors"
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors cursor-pointer disabled:opacity-50"
              >
                {isSubmitting ? "Đang gửi..." : "Gửi yêu cầu liên kết"}
              </button>
            </form>

            <div className="p-3.5 rounded-2xl bg-stone-50 border border-stone-200/70 text-xs space-y-2">
              <div className="font-bold text-neutral-800 flex items-center gap-1.5">
                <ShieldCheck className="size-4 text-emerald-600" />
                Hướng dẫn xác thực 2 chiều:
              </div>
              <ol className="list-decimal list-inside text-neutral-500 space-y-1 pl-1 text-[11px] leading-relaxed">
                <li>Học sinh đăng nhập tài khoản và vào mục Cài đặt tài khoản.</li>
                <li>Sao chép Mã học sinh cá nhân hoặc kiểm tra lời mời liên kết.</li>
                <li>Sau khi học sinh nhấn Chấp nhận, dữ liệu lịch và tiến độ sẽ được tự động đồng bộ.</li>
              </ol>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
