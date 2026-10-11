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
  const [activeClasses] = useState<any[]>([]);
  const [nextSession] = useState<any | null>(null);
  const [activeGoal, setActiveGoal] = useState<any | null>(null);

  useEffect(() => {
    async function loadData() {
      try {
        const studentProf = await profileService.getMyStudentProfile();
        setProfile(studentProf);

        const savedGoalStr = localStorage.getItem("edufit_student_onboarding_goal");
        if (savedGoalStr) {
          try {
            const savedGoal = JSON.parse(savedGoalStr);
            setActiveGoal({
              id: "goal-1",
              grade: savedGoal.grade || studentProf.educationLevelName || "Lớp 9",
              title: savedGoal.title || "Ôn luyện mục tiêu",
              roadmapDesc: `Môn: ${savedGoal.subjects?.join(", ") || "Toán"} · Khu vực: ${savedGoal.area || studentProf.area || "Hà Nội"}`,
              progressPercent: 33,
              completedMilestones: 1,
              totalMilestones: 3,
              milestones: [
                { label: "1. Thiết lập mục tiêu & hồ sơ", completed: true },
                { label: "2. Chọn & kết nối gia sư", completed: false },
                { label: "3. Đặt lịch học thử 1-1", completed: false },
              ],
              nextSessionText: "Sẵn sàng kết nối gia sư",
            });
          } catch {
            // ignore JSON parse error
          }
        } else if (studentProf.profileComplete) {
          setActiveGoal({
            id: "goal-1",
            grade: studentProf.educationLevelName || "Lớp 9",
            title: "Mục tiêu: Đạt điểm 8+ và luyện thi vào 10",
            roadmapDesc: `Lộ trình học tập cá nhân hóa tại ${studentProf.area || "Hà Nội"}`,
            progressPercent: 33,
            completedMilestones: 1,
            totalMilestones: 3,
            milestones: [
              { label: "1. Thiết lập mục tiêu & hồ sơ", completed: true },
              { label: "2. Chọn & kết nối gia sư", completed: false },
              { label: "3. Đặt lịch học thử 1-1", completed: false },
            ],
            nextSessionText: "Chưa xếp lịch học",
          });
        }
      } catch (err) {
        console.error("Lỗi khi tải dữ liệu học sinh:", err);
      }
    }

    loadData();
  }, []);

  const onboardingSteps = [
    {
      title: "Cài đặt hồ sơ học tập",
      description: "Chọn khối lớp và khu vực cư trú để hệ thống ghép cặp gia sư lân cận.",
      href: "/dashboard/profile",
      actionText: "Thiết lập ngay",
      completed: profile?.profileComplete === true,
    },
    {
      title: "Khám phá & Chọn gia sư",
      description: "Xem hồ sơ bằng cấp, đánh giá sao hoặc nhận danh sách AI Match chuẩn xác.",
      href: "/dashboard/tutors",
      actionText: "Tìm gia sư",
      completed: false,
    },
    {
      title: "Gửi yêu cầu học thử 1-1",
      description: "Đăng ký buổi trao đổi đầu tiên để thống nhất lịch học và mục tiêu điểm số.",
      href: "/dashboard/tutors",
      actionText: "Khám phá môn học",
      completed: false,
    },
  ];

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Greeting & Quick Search Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Chào {studentName} 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            {nextSession
              ? "Hôm nay bạn có 1 buổi học Toán lúc 19:00 · Lớp 9 Ôn thi vào 10"
              : "Chào mừng bạn đến với EduFit! Hãy bắt đầu bằng việc tìm gia sư phù hợp."}
          </p>
        </div>
        <Link
          href="/dashboard/tutors"
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
            <div className="p-4 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center shrink-0">
                <CheckCircle2 className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 font-semibold">Cột mốc hoàn thành</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900">
                  {activeClasses.length > 0 ? "8 / 12" : "0 / 0"}
                </div>
              </div>
            </div>

            <div className="p-4 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-sky-100 text-sky-800 flex items-center justify-center shrink-0">
                <TrendingUp className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 font-semibold">Điểm bài tập trung bình</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900">
                  {activeClasses.length > 0 ? "8.4" : "--"} <span className="text-xs font-normal text-neutral-400">/ 10</span>
                </div>
              </div>
            </div>

            <div className="p-4 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-3.5">
              <div className="size-11 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center shrink-0">
                <Calendar className="size-5" />
              </div>
              <div>
                <div className="text-[11px] text-neutral-500 font-semibold">Chuỗi ngày học tập</div>
                <div className="text-xl font-extrabold font-heading text-neutral-900">
                  {activeClasses.length > 0 ? "6 ngày" : "0 ngày"} <span className="text-xs font-bold text-amber-600">khởi đầu ✨</span>
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
