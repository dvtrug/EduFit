"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Plus, CreditCard, HeartHandshake, UserPlus } from "lucide-react";
import { ParentChildSwitcher, ChildInfo } from "./ParentChildSwitcher";
import { ParentKpiGrid } from "./ParentKpiGrid";
import { ParentActiveTutors } from "./ParentActiveTutors";
import { ParentQuickActions } from "./ParentQuickActions";
import { OnboardingGuideBanner } from "@/shared/ui/OnboardingGuideBanner";
import { EmptyState } from "@/shared/ui/EmptyState";

interface ParentDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

export function ParentDashboardView({ user }: ParentDashboardViewProps) {
  const parentName = user.fullName || "Quý phụ huynh";

  // Đọc danh sách con linh hoạt từ storage hoặc kết nối
  const [childrenList, setChildrenList] = useState<ChildInfo[]>(() => {
    if (typeof window !== "undefined") {
      try {
        const stored = localStorage.getItem("edufit_parent_linked_students");
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) {
            return parsed.map((item: any, idx: number) => ({
              id: idx + 1,
              name: item.name || `Học sinh ${idx + 1}`,
              grade: item.grade || "Lớp 9",
              tutorsCount: 0,
              completedLessons: "0/0",
              avgScore: "Chưa có",
              tuition: "0 đ",
              nextSession: "Chưa xếp lịch",
            }));
          }
        }
      } catch {
        // fallback
      }
    }
    return [];
  });
  const [selectedChild, setSelectedChild] = useState<number>(0);
  const currentChild = childrenList[selectedChild];

  const onboardingSteps = [
    {
      title: "Liên kết tài khoản con em",
      description: "Nhập mã tài khoản học sinh để đồng bộ lịch học và liên lạc gia sư.",
      href: "/dashboard/children",
      actionText: "Liên kết ngay",
      completed: childrenList.length > 0,
    },
    {
      title: "Khám phá & Chọn gia sư",
      description: "Lựa chọn gia sư có năng lực kiểm định hoặc để AI đề xuất hồ sơ tốt nhất.",
      href: "/dashboard/tutors",
      actionText: "Tìm gia sư",
      completed: false,
    },
    {
      title: "Theo dõi báo cáo tiến độ",
      description: "Nhận nhật ký buổi dạy, điểm bài kiểm tra và quản lý gói học phí minh bạch.",
      href: "/dashboard/progress",
      actionText: "Xem báo cáo",
      completed: false,
    },
  ];

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Greeting & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Chào {parentName}, tổng quan học tập con em 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            {childrenList.length > 0
              ? `Bạn đang theo dõi ${childrenList.length} học sinh trên nền tảng EduFit.`
              : "Chào mừng quý phụ huynh! Hãy liên kết tài khoản con em để bắt đầu theo dõi."}
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Link
            href="/dashboard/children"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 bg-white hover:bg-stone-50 border border-stone-200 text-neutral-800 rounded-xl text-xs font-bold shadow-2xs transition-colors shrink-0"
          >
            <Plus className="size-3.5 text-neutral-500" />
            <span>Liên kết thêm con</span>
          </Link>
          <Link
            href="/dashboard/billing"
            className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors shrink-0"
          >
            <CreditCard className="size-3.5" />
            <span>Thanh toán học phí</span>
          </Link>
        </div>
      </div>

      {/* 2. Banner Hướng dẫn thao tác cho Phụ Huynh mới */}
      <OnboardingGuideBanner
        role="PARENT"
        userName={parentName}
        steps={onboardingSteps}
      />

      {childrenList.length === 0 ? (
        /* Empty State toàn diện khi chưa liên kết con em */
        <EmptyState
          icon={HeartHandshake}
          theme="emerald"
          badge="CHƯA CÓ DỮ LIỆU CON EM"
          title="Chưa có tài khoản học sinh nào được liên kết"
          description="Để bắt đầu theo dõi lịch học, nhận báo cáo tiến độ và quản lý học phí, vui lòng kết nối tài khoản học sinh của con bạn vào hệ thống."
          action={{
            label: "Liên kết tài khoản con em ngay",
            href: "/dashboard/children",
            icon: UserPlus,
          }}
          secondaryAction={{
            label: "Khám phá danh sách gia sư trước",
            href: "/dashboard/tutors",
          }}
          steps={[
            {
              step: 1,
              title: "Lấy mã học sinh",
              description: "Học sinh đăng nhập và sao chép mã tài khoản trong mục Cài đặt.",
              href: "/dashboard/profile",
            },
            {
              step: 2,
              title: "Gửi yêu cầu liên kết",
              description: "Phụ huynh dán mã học sinh và gửi xác thực qua email/số điện thoại.",
              href: "/dashboard/children",
            },
            {
              step: 3,
              title: "Hoàn tất đồng bộ",
              description: "Mọi thông tin lịch học, gia sư sẽ ngay lập tức xuất hiện tại đây.",
            },
          ]}
        />
      ) : (
        <>
          {/* Child Switcher Tabs */}
          <ParentChildSwitcher
            childrenList={childrenList}
            selectedChild={selectedChild}
            onSelect={setSelectedChild}
          />

          {/* KPI Grid */}
          <ParentKpiGrid child={currentChild} />

          {/* 2-Column Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
            <div className="lg:col-span-8 space-y-6">
              <ParentActiveTutors tutors={[]} />
            </div>

            <div className="lg:col-span-4 space-y-6">
              <ParentQuickActions />
            </div>
          </div>
        </>
      )}
    </div>
  );
}
