"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Search, CheckCircle2, TrendingUp, Calendar } from "lucide-react";
import { StudentGoalProgressCard } from "./StudentGoalProgressCard";
import { StudentNextSessionCard } from "./StudentNextSessionCard";
import { StudentActiveClasses } from "./StudentActiveClasses";
import { StudentQuickActions } from "./StudentQuickActions";
import { OnboardingGuideBanner } from "@/shared/ui/OnboardingGuideBanner";
import { profileService, StudentProfileData } from "@/services/profile";
import { connectionApi } from "@/features/connection/api/connectionApi";
import { schedulingApi } from "@/features/scheduling";
import type { TutoringClass } from "@/features/connection/types";
import type { CalendarItem } from "@/features/scheduling/types";

interface StudentDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

export function StudentDashboardView({ user }: StudentDashboardViewProps) {
  const studentName = user.fullName || "Minh Anh";

  const [profile, setProfile] = useState<StudentProfileData | null>(null);
  const [rawClasses, setRawClasses] = useState<TutoringClass[]>([]);
  const [activeClasses, setActiveClasses] = useState<any[]>([]);
  const [nextSession, setNextSession] = useState<any | null>(null);
  const [activeGoal, setActiveGoal] = useState<any | null>(null);

  useEffect(() => {
    let ignore = false;

    async function loadData() {
      try {
        const studentProf = await profileService.getMyStudentProfile();
        if (!ignore) setProfile(studentProf);

        // Mục tiêu học tập: Để trống khi học sinh chưa thiết lập mục tiêu thật
        if (!ignore) {
          setActiveGoal(null);
        }

        // Fetch real classes and calendar
        const now = new Date();
        const from = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0).toISOString();
        const to = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 7, 23, 59, 59).toISOString();

        const [classesRes, calendarRes] = await Promise.allSettled([
          connectionApi.getStudentClasses(),
          schedulingApi.getCalendar({ from, to }),
        ]);

        if (ignore) return;

        const classList = classesRes.status === "fulfilled" ? classesRes.value || [] : [];
        setRawClasses(classList);

        const activeList = classList.filter((c) => c.status === "ACTIVE");
        const badgeColors = [
          "bg-sky-50 text-sky-800",
          "bg-amber-50 text-amber-800",
          "bg-emerald-50 text-emerald-800",
          "bg-purple-50 text-purple-800",
        ];
        const borderColors = [
          "hover:border-sky-300",
          "hover:border-amber-300",
          "hover:border-emerald-300",
          "hover:border-purple-300",
        ];

        const mappedActive = activeList.map((c, idx) => ({
          subject: c.subjectName || "Môn học kèm",
          badgeColor: badgeColors[idx % badgeColors.length],
          borderColor: borderColors[idx % borderColors.length],
          frequency: c.scheduleStatus === "SCHEDULED" ? "Đã chốt lịch" : "Chờ thống nhất lịch",
          title: `${c.subjectName || "Môn học kèm"}${c.educationLevelName ? ` - ${c.educationLevelName}` : ""}`,
          tutor: c.tutorDisplayName || "Gia sư EduFit",
          progress: `Đã học ${c.completedCount ?? 0} / ${c.totalCount ?? c.sessionCount} buổi`,
        }));
        setActiveClasses(mappedActive);

        // Next session: active/pending sessions that haven't ended yet
        let sessions: CalendarItem[] = [];
        if (calendarRes.status === "fulfilled" && calendarRes.value && Array.isArray(calendarRes.value.data)) {
          sessions = calendarRes.value.data;
        }

        const validUpcoming = sessions
          .filter(
            (s) =>
              (s.status === "SCHEDULED" || s.status === "PROPOSED") &&
              new Date(s.endAt).getTime() >= Date.now()
          )
          .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());

        if (validUpcoming.length > 0) {
          const earliest = validUpcoming[0];
          const classItem = classList.find((c) => c.classId === earliest.classId);
          const start = new Date(earliest.startAt);
          const end = new Date(earliest.endAt);
          const days = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
          const dayShort = days[start.getDay()];
          const dateNum = String(start.getDate());
          const formatHour = (d: Date) =>
            d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit", hour12: false });
          const timeSlot = `${formatHour(start)} - ${formatHour(end)}`;

          setNextSession({
            id: earliest.sessionId,
            dayShort,
            dateNum,
            lessonTitle: classItem?.subjectName
              ? `${classItem.subjectName} · ${classItem.educationLevelName || "Kèm 1-1"}`
              : "Buổi học kèm 1-1",
            timeSlot,
            tutorName: classItem?.tutorDisplayName || "Gia sư",
            status: earliest.status === "SCHEDULED" ? "Đã xác nhận" : "Đề xuất",
          });
        } else {
          setNextSession(null);
        }
      } catch (err) {
        console.error("Lỗi khi tải dữ liệu học sinh:", err);
      }
    }

    void loadData();

    return () => {
      ignore = true;
    };
  }, []);

  const totalCompleted = rawClasses.reduce(
    (acc, c) => acc + (c.completedCount ?? c.sessionCount ?? 0),
    0
  );
  const totalPlanned = rawClasses.reduce(
    (acc, c) => acc + (c.totalCount ?? c.sessionCount ?? 0),
    0
  );

  const onboardingSteps = [
    {
      title: "Cài đặt hồ sơ học tập",
      description: "Chọn khối lớp và khu vực cư trú để hệ thống ghép cặp gia sư lân cận.",
      href: "/student/profile",
      actionText: "Thiết lập ngay",
      completed: profile?.profileComplete === true,
    },
    {
      title: "Khám phá & Chọn gia sư",
      description: "Xem hồ sơ bằng cấp, đánh giá sao hoặc nhận danh sách AI Match chuẩn xác.",
      href: "/student/tutors",
      actionText: "Tìm gia sư",
      completed: rawClasses.length > 0,
    },
    {
      title: "Gửi yêu cầu học thử 1-1",
      description: "Đăng ký buổi trao đổi đầu tiên để thống nhất lịch học và mục tiêu điểm số.",
      href: "/student/tutors",
      actionText: "Khám phá môn học",
      completed: rawClasses.length > 0,
    },
  ];

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Greeting & Quick Search Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 dark:text-stone-100 tracking-tight">
            Chào {studentName} 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 dark:text-stone-300 mt-1 font-medium">
            {nextSession
              ? `Bạn có buổi học ${nextSession.lessonTitle} lúc ${nextSession.timeSlot}`
              : "Chào mừng bạn đến với EduFit! Hãy bắt đầu bằng việc tìm gia sư phù hợp."}
          </p>
        </div>
        <Link
          href="/student/tutors"
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-amber-500 hover:bg-amber-600 text-neutral-900 font-bold text-xs rounded-xl shadow-xs transition-all hover:scale-[1.02] shrink-0"
        >
          <Search className="size-4" />
          <span>Tìm gia sư</span>
        </Link>
      </div>

      {/* 2. Banner Hướng dẫn thao tác cho người mới (Onboarding Guide) */}
      <OnboardingGuideBanner
        role="STUDENT"
        userName={studentName}
        steps={onboardingSteps}
      />

      {/* 3. Main 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        {/* Main Column (8 Cols) */}
        <div className="lg:col-span-8 space-y-6">
          <StudentGoalProgressCard goal={activeGoal} />
          <StudentNextSessionCard session={nextSession} />
          <StudentActiveClasses classes={activeClasses} />
        </div>

        {/* Side Column (4 Cols) */}
        <div className="lg:col-span-4 space-y-6">
          {/* Micro Metrics */}
          <div className="grid grid-cols-1 gap-3.5">
            <div className="p-4 bg-white dark:bg-stone-900 rounded-2xl border border-stone-200/80 dark:border-stone-800 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-emerald-100 dark:bg-emerald-950/80 text-emerald-800 dark:text-emerald-300 flex items-center justify-center shrink-0">
                <CheckCircle2 className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 dark:text-stone-300 font-semibold">Cột mốc hoàn thành</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900 dark:text-stone-100">
                  {totalPlanned > 0 ? `${totalCompleted} / ${totalPlanned}` : "0 / 0"}
                </div>
              </div>
            </div>

            <div className="p-4 bg-white dark:bg-stone-900 rounded-2xl border border-stone-200/80 dark:border-stone-800 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-sky-100 dark:bg-sky-950/80 text-sky-800 dark:text-sky-300 flex items-center justify-center shrink-0">
                <TrendingUp className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 dark:text-stone-300 font-semibold">Điểm bài tập trung bình</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900 dark:text-stone-100">
                  {activeClasses.length > 0 ? "8.4" : "--"} <span className="text-xs font-normal text-neutral-400 dark:text-stone-400">/ 10</span>
                </div>
              </div>
            </div>

            <div className="p-4 bg-white dark:bg-stone-900 rounded-2xl border border-stone-200/80 dark:border-stone-800 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-amber-100 dark:bg-amber-950/80 text-amber-800 dark:text-amber-300 flex items-center justify-center shrink-0">
                <Calendar className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 dark:text-stone-300 font-semibold">Chuỗi ngày học tập</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900 dark:text-stone-100">
                  {activeClasses.length > 0 ? "6 ngày" : "0 ngày"} <span className="text-xs font-bold text-amber-600 dark:text-amber-400">khởi đầu ✨</span>
                </div>
              </div>
            </div>
          </div>

          <StudentQuickActions />
        </div>
      </div>
    </div>
  );
}
