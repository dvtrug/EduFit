"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Clock, Plus } from "lucide-react";
import { TutorVerificationBanner } from "@/features/verification/components/TutorVerificationBanner";
import { UploadCredentialModal } from "@/features/verification/components/UploadCredentialModal";
import { VerificationStatus } from "@/services/verification";
import { profileService, TutorProfileData } from "@/services/profile";
import { TutorKpiGrid } from "./TutorKpiGrid";
import { TutorUpcomingClasses, UpcomingClassItem } from "./TutorUpcomingClasses";
import { TutorPendingRequests, PendingRequestItem } from "./TutorPendingRequests";
import { TutorQuickActions } from "./TutorQuickActions";
import { OnboardingGuideBanner } from "@/shared/ui/OnboardingGuideBanner";

interface TutorDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

export function TutorDashboardView({ user }: TutorDashboardViewProps) {
  const tutorName = user.fullName || "Gia sư";
  const [verificationStatus, setVerificationStatus] = useState<VerificationStatus>("DRAFT");
  const [profile, setProfile] = useState<TutorProfileData | null>(null);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

  // Scheduling và Connection chưa có API ứng dụng trong source hiện tại.
  // Giữ trạng thái rỗng thật thay vì bơm dữ liệu demo vào dashboard production.
  const upcomingClasses: UpcomingClassItem[] = [];
  const pendingRequests: PendingRequestItem[] = [];

  React.useEffect(() => {
    import("@/services/verification").then(({ verificationService }) => {
      Promise.all([
        verificationService.getMyRequest(),
        profileService.getMyTutorProfile().catch(() => null),
      ]).then(([request, tutorProfile]) => {
        setVerificationStatus(request?.status || "DRAFT");
        setProfile(tutorProfile);
      }).catch(() => {
        setVerificationStatus("DRAFT");
      });
    });
  }, []);

  const hasConfiguredTeachingProfile = Boolean(
    profile && profile.subjects.length > 0 && profile.availabilitySlots.length > 0
  );

  const onboardingSteps = [
    {
      title: "Thiết lập môn dạy & Lịch rảnh",
      description: "Khai báo mức học phí và các khung giờ bạn sẵn sàng nhận lớp trong tuần.",
      href: "/dashboard/profile",
      actionText: "Cài đặt ngay",
      completed: hasConfiguredTeachingProfile,
    },
    {
      title: "Xác minh bằng cấp năng lực (KYC)",
      description: "Tải chứng chỉ sư phạm, bằng đại học để nhận huy hiệu Gia Sư Uy Tín.",
      href: "/dashboard/verification",
      actionText: "Nộp bằng cấp",
      completed: verificationStatus === "VERIFIED" || verificationStatus === "APPROVED",
    },
    {
      title: "Nhận yêu cầu & Bắt đầu dạy",
      description: "Chấp nhận lời mời học thử từ học sinh và tạo lớp học 1-1 trực tiếp.",
      href: "/dashboard/requests",
      actionText: "Xem hàng đợi",
      completed: false,
    },
  ];

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Header Greeting & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Chào Thầy/Cô {tutorName} 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            {upcomingClasses.length > 0 || pendingRequests.length > 0
              ? `Hôm nay bạn có ${upcomingClasses.length} buổi dạy kèm và ${pendingRequests.length} yêu cầu đang chờ phản hồi.`
              : "Chào mừng bạn đến với EduFit! Hoàn thiện các bước dưới đây để nhận học viên đầu tiên."}
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Link
            href="/dashboard/profile"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 bg-white hover:bg-stone-50 border border-stone-200 text-neutral-800 rounded-xl text-xs font-bold shadow-2xs transition-colors shrink-0"
          >
            <Clock className="size-3.5 text-neutral-500" />
            <span>Cập nhật lịch rảnh</span>
          </Link>
          <Link
            href="/dashboard/classes"
            className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors shrink-0"
          >
            <Plus className="size-3.5" />
            <span>Tạo buổi dạy mới</span>
          </Link>
        </div>
      </div>

      {/* 2. Banner Hướng dẫn thao tác cho Gia Sư mới */}
      <OnboardingGuideBanner
        role="TUTOR"
        userName={tutorName}
        steps={onboardingSteps}
      />

      {/* 3. Banner Xác minh Gia sư */}
      <TutorVerificationBanner
        status={verificationStatus}
        onOpenUploadModal={() => setIsUploadModalOpen(true)}
      />

      {/* 4. Thẻ KPI */}
      <TutorKpiGrid
        completedSessions={0}
        plannedSessions={0}
        activeStudents={0}
        estimatedIncome={0}
        ratingAverage={profile?.ratingAvg || 0}
        reviewCount={profile?.reviewCount || 0}
      />

      {/* 5. 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        <div className="lg:col-span-8 space-y-6">
          <TutorUpcomingClasses classes={upcomingClasses} />
          <TutorPendingRequests requests={pendingRequests} />
        </div>

        <div className="lg:col-span-4 space-y-6">
          <TutorQuickActions />
        </div>
      </div>

      {/* Modal Upload KYC Credential */}
      <UploadCredentialModal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        onSuccess={() => setVerificationStatus("PENDING")}
      />
    </div>
  );
}
