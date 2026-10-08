"use client";

import React, { useState } from "react";
import { Clock, Plus } from "lucide-react";
import { TutorVerificationBanner } from "@/features/verification/components/TutorVerificationBanner";
import { UploadCredentialModal } from "@/features/verification/components/UploadCredentialModal";
import { VerificationStatus } from "@/services/verification";
import { TutorKpiGrid } from "./TutorKpiGrid";
import { TutorUpcomingClasses } from "./TutorUpcomingClasses";
import { TutorPendingRequests } from "./TutorPendingRequests";
import { TutorQuickActions } from "./TutorQuickActions";

interface TutorDashboardViewProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
}

export function TutorDashboardView({ user }: TutorDashboardViewProps) {
  const tutorName = user.fullName || "Hoàng Nam";
  const [verificationStatus, setVerificationStatus] = useState<VerificationStatus>("DRAFT");
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Header Greeting & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Chào Thầy {tutorName} 👋
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            Hôm nay bạn có 2 buổi dạy kèm và 2 yêu cầu kết nối học sinh mới đang chờ phản hồi.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 bg-white hover:bg-stone-50 border border-stone-200 text-neutral-800 rounded-xl text-xs font-bold shadow-2xs transition-colors shrink-0"
          >
            <Clock className="size-3.5 text-neutral-500" />
            <span>Cập nhật lịch rảnh</span>
          </button>
          <button
            type="button"
            className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors shrink-0"
          >
            <Plus className="size-3.5" />
            <span>Tạo buổi dạy mới</span>
          </button>
        </div>
      </div>

      {/* 2. Banner Xác minh Gia sư */}
      <TutorVerificationBanner
        status={verificationStatus}
        onOpenUploadModal={() => setIsUploadModalOpen(true)}
      />

      {/* 3. Thẻ KPI */}
      <TutorKpiGrid />

      {/* 4. 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
        <div className="lg:col-span-8 space-y-6">
          <TutorUpcomingClasses />
          <TutorPendingRequests />
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
