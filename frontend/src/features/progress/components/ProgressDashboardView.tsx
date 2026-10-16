"use client";

import React, { useState, useEffect } from "react";
import {
  TrendingUp,
  CheckCircle2,
  Clock,
  CircleDashed,
  BookOpen,
  Calendar,
  User,
  GraduationCap,
  Award,
  ChevronRight,
  FileText,
  AlertCircle,
  Sparkles,
} from "lucide-react";
import { progressApi } from "../api/progressApi";
import type {
  StudentDashboardData,
  ParentDashboardData,
  ParentStudentSummary,
  MilestoneItem,
  SessionNoteItem,
} from "../types";

interface ProgressDashboardViewProps {
  role?: "STUDENT" | "PARENT";
  studentId?: string;
}

export function ProgressDashboardView({
  role = "PARENT",
  studentId: initialStudentId,
}: ProgressDashboardViewProps) {
  const [parentData, setParentData] = useState<ParentDashboardData | null>(null);
  const [studentData, setStudentData] = useState<StudentDashboardData | null>(null);
  const [selectedStudentId, setSelectedStudentId] = useState<string | undefined>(initialStudentId);
  const [selectedClassId, setSelectedClassId] = useState<string | undefined>(undefined);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Load parent or student data
  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      try {
        setIsLoading(true);
        setError(null);

        if (role === "PARENT") {
          const res = await progressApi.getParentDashboard();
          if (res.success && res.data && isMounted) {
            setParentData(res.data);
            const firstStudent = res.data.students?.[0];
            if (firstStudent) {
              const activeStudentId = selectedStudentId || firstStudent.studentId;
              setSelectedStudentId(activeStudentId);
              const target = res.data.students.find((s) => s.studentId === activeStudentId) || firstStudent;
              setStudentData(target.activeClassDashboard || null);
            }
          }
        } else if (initialStudentId) {
          const res = await progressApi.getStudentDashboard(initialStudentId, selectedClassId);
          if (res.success && res.data && isMounted) {
            setStudentData(res.data);
          }
        }
      } catch (err: unknown) {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : "Không thể tải dữ liệu tiến độ";
          setError(msg);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    loadData();
    return () => {
      isMounted = false;
    };
  }, [role, selectedStudentId, selectedClassId, initialStudentId]);

  // Handler for parent switching child
  const handleSelectStudent = (student: ParentStudentSummary) => {
    setSelectedStudentId(student.studentId);
    setSelectedClassId(undefined);
    setStudentData(student.activeClassDashboard || null);
  };

  // Handler for class switching
  const handleSelectClass = async (classId: string) => {
    if (!selectedStudentId) return;
    try {
      setIsLoading(true);
      setSelectedClassId(classId);
      const res = await progressApi.getStudentDashboard(selectedStudentId, classId);
      if (res.success && res.data) {
        setStudentData(res.data);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Lỗi khi chuyển lớp học";
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading && !studentData) {
    return (
      <div className="py-24 flex flex-col items-center justify-center gap-3">
        <div className="size-8 border-3 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
        <span className="text-xs text-neutral-500 font-medium">Đang tải tiến độ học tập...</span>
      </div>
    );
  }

  if (error && !studentData) {
    return (
      <div className="p-8 text-center bg-rose-50 border border-rose-200 rounded-2xl text-xs text-rose-700 space-y-2">
        <AlertCircle className="size-6 text-rose-500 mx-auto" />
        <p className="font-bold">Đã có lỗi xảy ra</p>
        <p>{error}</p>
      </div>
    );
  }

  const plan = studentData?.plan;
  const milestones = plan?.milestones || [];
  const stats = studentData?.sessionStats;
  const progressPercent = studentData?.progressPercentage ?? plan?.progressPercentage ?? 0;

  return (
    <div className="space-y-6">
      {/* 1. Header & Parent Child Switcher */}
      {role === "PARENT" && parentData && parentData.students.length > 0 && (
        <div className="p-4 rounded-2xl bg-white border border-stone-200/80 shadow-xs flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <GraduationCap className="size-5 text-amber-600" />
            <span className="text-xs font-bold text-neutral-700 uppercase tracking-wider">
              Chọn học sinh theo dõi:
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {parentData.students.map((student) => {
              const isSelected = student.studentId === selectedStudentId;
              return (
                <button
                  key={student.studentId}
                  type="button"
                  onClick={() => handleSelectStudent(student)}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                    isSelected
                      ? "bg-amber-500 text-white shadow-xs"
                      : "bg-stone-100 text-neutral-700 hover:bg-stone-200"
                  }`}
                >
                  {student.studentName}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* 2. Class Selector Filter if multiple classes */}
      {studentData?.classes && studentData.classes.length > 1 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          <span className="text-xs text-neutral-400 font-medium shrink-0">Lớp học:</span>
          {studentData.classes.map((cls) => {
            const isSelected = cls.classId === (selectedClassId || studentData.selectedClassId);
            return (
              <button
                key={cls.classId}
                type="button"
                onClick={() => handleSelectClass(cls.classId)}
                className={`px-3 py-1 rounded-lg text-xs font-medium whitespace-nowrap transition-colors cursor-pointer ${
                  isSelected
                    ? "bg-amber-100 text-amber-900 border border-amber-300 font-bold"
                    : "bg-white text-neutral-600 hover:bg-stone-100 border border-stone-200"
                }`}
              >
                {cls.title} ({cls.progressPercentage || 0}%)
              </button>
            );
          })}
        </div>
      )}

      {/* 3. Top Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Progress percentage */}
        <div className="p-5 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-neutral-500">
            <span className="text-xs font-bold uppercase tracking-wider">Tiến độ mục tiêu</span>
            <TrendingUp className="size-4 text-amber-600" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-neutral-900">{progressPercent}%</span>
            <span className="text-xs text-neutral-400 font-medium">hoàn thành</span>
          </div>
          <div className="w-full h-2 bg-stone-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-amber-500 rounded-full transition-all duration-500"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>

        {/* Card 2: Completed Sessions */}
        <div className="p-5 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-neutral-500">
            <span className="text-xs font-bold uppercase tracking-wider">Buổi học hoàn tất</span>
            <CheckCircle2 className="size-4 text-emerald-600" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-neutral-900">
              {stats?.completedSessions ?? 0}
            </span>
            <span className="text-xs text-neutral-400 font-medium">
              / {stats?.totalSessions ?? 0} buổi
            </span>
          </div>
          <p className="text-[11px] text-neutral-500">
            Sắp tới: {stats?.upcomingSessions ?? 0} buổi
          </p>
        </div>

        {/* Card 3: Milestones reached */}
        <div className="p-5 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-neutral-500">
            <span className="text-xs font-bold uppercase tracking-wider">Cột mốc kiến thức</span>
            <Award className="size-4 text-blue-600" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-neutral-900">
              {plan?.completedMilestones ?? 0}
            </span>
            <span className="text-xs text-neutral-400 font-medium">
              / {plan?.totalMilestones ?? 0} mốc
            </span>
          </div>
          <p className="text-[11px] text-neutral-500">
            Kế hoạch: {plan?.title || "Chưa thiết lập"}
          </p>
        </div>

        {/* Card 4: Tutor In Charge */}
        <div className="p-5 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-neutral-500">
            <span className="text-xs font-bold uppercase tracking-wider">Gia sư phụ trách</span>
            <User className="size-4 text-purple-600" />
          </div>
          <div className="truncate">
            <span className="text-base font-bold text-neutral-900 block truncate">
              {studentData?.tutorName || "Đang kết nối"}
            </span>
            <span className="text-xs text-neutral-400 font-medium block truncate">
              {studentData?.goalTitle || "Khóa học kèm 1:1"}
            </span>
          </div>
        </div>
      </div>

      {/* 4. Milestones & Learning Plan Detail */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Milestones Timeline */}
        <div className="lg:col-span-2 p-6 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-stone-100 pb-3">
            <div className="flex items-center gap-2">
              <Award className="size-4.5 text-amber-600" />
              <h3 className="font-heading font-bold text-sm text-neutral-900">
                Lộ trình các cột mốc học tập
              </h3>
            </div>
            <span className="text-xs text-neutral-400">
              {plan?.completedMilestones || 0}/{milestones.length} hoàn thành
            </span>
          </div>

          {milestones.length === 0 ? (
            <div className="py-12 text-center text-xs text-neutral-400">
              Gia sư đang lên kế hoạch lộ trình chi tiết cho lớp học này.
            </div>
          ) : (
            <div className="space-y-3">
              {milestones.map((m, idx) => {
                const isCompleted = m.status === "COMPLETED";
                const isInProgress = m.status === "IN_PROGRESS";

                return (
                  <div
                    key={m.milestoneId || idx}
                    className={`p-4 rounded-xl border flex items-start gap-3 transition-colors ${
                      isCompleted
                        ? "bg-emerald-50/40 border-emerald-200/60"
                        : isInProgress
                        ? "bg-amber-50/40 border-amber-200/80 ring-1 ring-amber-400/30"
                        : "bg-stone-50/40 border-stone-200/60 opacity-80"
                    }`}
                  >
                    <div className="mt-0.5 shrink-0">
                      {isCompleted ? (
                        <CheckCircle2 className="size-5 text-emerald-600" />
                      ) : isInProgress ? (
                        <CircleDashed className="size-5 text-amber-600 animate-spin" />
                      ) : (
                        <Clock className="size-5 text-stone-400" />
                      )}
                    </div>

                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-bold text-neutral-900">
                          Mốc {idx + 1}: {m.title}
                        </span>
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            isCompleted
                              ? "bg-emerald-100 text-emerald-800"
                              : isInProgress
                              ? "bg-amber-100 text-amber-800"
                              : "bg-stone-200 text-neutral-600"
                          }`}
                        >
                          {isCompleted
                            ? "Đã xong"
                            : isInProgress
                            ? "Đang học"
                            : "Chưa bắt đầu"}
                        </span>
                      </div>
                      {m.description && (
                        <p className="text-[11px] text-neutral-600 leading-relaxed">
                          {m.description}
                        </p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: Recent Session Notes */}
        <div className="p-6 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-stone-100 pb-3">
            <div className="flex items-center gap-2">
              <FileText className="size-4.5 text-amber-600" />
              <h3 className="font-heading font-bold text-sm text-neutral-900">
                Nhật ký buổi học gần nhất
              </h3>
            </div>
          </div>

          {(!studentData?.recentNotes || studentData.recentNotes.length === 0) ? (
            <div className="py-12 text-center text-xs text-neutral-400">
              Chưa có nhật ký buổi học nào được ghi nhận.
            </div>
          ) : (
            <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1">
              {studentData.recentNotes.map((note) => (
                <div
                  key={note.sessionNoteId}
                  className="p-3.5 rounded-xl bg-stone-50 border border-stone-200/70 text-xs space-y-2"
                >
                  <div className="flex items-center justify-between text-[11px] text-neutral-400">
                    <span className="font-bold text-neutral-700">
                      Buổi học {note.sessionDate || ""}
                    </span>
                    <span>{new Date(note.createdAt).toLocaleDateString("vi-VN")}</span>
                  </div>

                  <div>
                    <span className="text-[11px] font-bold text-neutral-800 block">
                      Tóm tắt nội dung:
                    </span>
                    <p className="text-neutral-600 text-[11px] leading-relaxed">
                      {note.summary}
                    </p>
                  </div>

                  {note.homeworkAssigned && (
                    <div className="bg-amber-50/60 p-2 rounded-lg border border-amber-200/60">
                      <span className="text-[10px] font-bold text-amber-800 block">
                        Bài tập về nhà:
                      </span>
                      <p className="text-neutral-700 text-[11px]">{note.homeworkAssigned}</p>
                    </div>
                  )}

                  {note.studentAttitude && (
                    <p className="text-[11px] text-neutral-500 italic">
                      Thái độ: {note.studentAttitude}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
