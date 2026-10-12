"use client";

import React, { Suspense, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import {
  Compass,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
} from "lucide-react";
import {
  TutorSearchFilters,
  TutorCard,
  TutorDetailModal,
  AiMatchView,
  useTutorSearch,
  useTutorDetail,
  useTutorMatching,
} from "@/features/discovery";
import { useToast } from "@/shared/ui/Toast";
import { GoalSelector } from "@/features/discovery/components/GoalSelector";
import { ConnectionRequestDialog } from "@/features/discovery/components/ConnectionRequestDialog";
import { SentRequests } from "@/features/discovery/components/SentRequests";


export default function StudentTutorsPage() {
  return <Suspense fallback={<p>Đang tải tìm kiếm gia sư…</p>}><StudentTutorsContent /></Suspense>;
}

function StudentTutorsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { showToast } = useToast();

  const tabParam = searchParams.get("tab");
  const activeTab =
    tabParam === "matches"
      ? "matches"
      : tabParam === "requests"
      ? "requests"
      : "browse"
  ;

  const handleTabChange = (tab: "browse" | "matches" | "requests") => {
    if (tab === "matches") {
      router.push("/student/tutors?tab=matches");
    } else if (tab === "requests") {
      router.push("/student/tutors?tab=requests");
    } else {
      router.push("/student/tutors");
    }
  };

  // 1. Search Hook (Khám phá)
  const {
    params,
    tutors,
    pagination,
    isLoading: isSearchLoading,
    error: searchError,
    updateFilters,
    setPage,
    resetFilters,
  } = useTutorSearch();

  // 2. Detail Modal Hook
  const {
    isOpen: isDetailOpen,
    tutor: selectedTutor,
    isLoading: isDetailLoading,
    error: detailError,
    openTutor,
    closeTutor,
  } = useTutorDetail();

  const matching = useTutorMatching({ parent: false, enabled: activeTab === "matches" });
  const { need, matches, isMatching, error: matchError, updateNeed } = matching;
  const [connectTutorId, setConnectTutorId] = useState<string | null>(null);
  const handleRequestConnect = (tutorId: string) => {
    if (!matching.goal || !matching.goal.availabilitySlots.length || matching.goal.readyForMatching === false) {
      showToast("Vui lòng chọn mục tiêu đã hoàn thiện hồ sơ và khung giờ học trước khi gửi yêu cầu.", "error");
      return;
    }
    setConnectTutorId(tutorId);
    closeTutor();
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* 1. HEADER (Figma 36:3538 / 36:3764 Heading) */}
      <div className="space-y-1">
        <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
          Tìm gia sư phù hợp
        </h2>
        <p className="text-xs sm:text-sm text-neutral-500 font-medium">
          Khám phá hồ sơ đã xác minh hoặc xem gợi ý dựa trên nhu cầu học của bạn.
        </p>
      </div>

      <GoalSelector matching={matching} />

      {/* 2. TABS NAVIGATOR (Figma Tabs: Khám phá, Gợi ý phù hợp, Yêu cầu đã gửi) */}
      <div className="flex items-center gap-6 border-b border-stone-200/90 text-xs font-semibold">
        {/* Tab 1: Khám phá */}
        <button
          type="button"
          onClick={() => handleTabChange("browse")}
          className={`pb-3 border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
            activeTab === "browse"
              ? "border-[#006DCC] text-[#006DCC] font-bold"
              : "border-transparent text-neutral-500 hover:text-neutral-900"
          }`}
        >
          <span>Khám phá</span>
        </button>

        {/* Tab 2: Gợi ý phù hợp */}
        <button
          type="button"
          onClick={() => handleTabChange("matches")}
          className={`pb-3 border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
            activeTab === "matches"
              ? "border-[#006DCC] text-[#006DCC] font-bold"
              : "border-transparent text-neutral-500 hover:text-neutral-900"
          }`}
        >
          <span>Gợi ý phù hợp</span>
          <span
            className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
              activeTab === "matches"
                ? "bg-[#006DCC] text-white"
                : "bg-stone-200 text-neutral-600"
            }`}
          >
            {matches.length}
          </span>
        </button>

        {/* Tab 3: Yêu cầu đã gửi */}
        <button
          type="button"
          onClick={() => handleTabChange("requests")}
          className={`pb-3 border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
            activeTab === "requests"
              ? "border-[#006DCC] text-[#006DCC] font-bold"
              : "border-transparent text-neutral-500 hover:text-neutral-900"
          }`}
        >
          <span>Yêu cầu đã gửi</span>
          <span
            className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
              activeTab === "requests"
                ? "bg-[#006DCC] text-white"
                : "bg-stone-200 text-neutral-600"
            }`}
          >
          </span>
        </button>
      </div>

      {/* =========================================================================
          3. TAB 1: KHÁM PHÁ (Figma 36:3538 Discover Tab)
         ========================================================================= */}
      {activeTab === "browse" && (
        <div className="space-y-4">
          {/* Search Bar & Quick Filters (Giữ nguyên 100% chức năng bộ lọc) */}
          <TutorSearchFilters
            params={params}
            onUpdateFilters={updateFilters}
            onResetFilters={resetFilters}
            totalResults={pagination?.totalElements}
          />

          {/* Results Counter & Verified Note (Figma Header) */}
          <div className="flex items-center justify-between text-xs pt-2">
            <span className="font-heading font-bold text-sm text-neutral-900">
              {pagination?.totalElements ?? tutors.length} gia sư phù hợp
            </span>
            <span className="text-[11px] text-neutral-400 flex items-center gap-1 font-medium">
              <ShieldCheck className="size-3.5 text-emerald-500" />
              <span>Chỉ hiển thị gia sư đã xác minh</span>
            </span>
          </div>

          {/* Error notice (nếu có) */}
          {searchError && (
            <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium">
              {searchError}
            </div>
          )}

          {/* Horizontal List of Tutors (Figma 36:3538 Layout) */}
          {isSearchLoading ? (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  className="h-24 rounded-2xl bg-white border border-stone-200/60 p-5 animate-pulse flex items-center justify-between"
                >
                  <div className="flex items-center gap-4">
                    <div className="size-12 rounded-full bg-stone-100" />
                    <div className="space-y-2">
                      <div className="h-4 w-36 bg-stone-100 rounded-md" />
                      <div className="h-3 w-48 bg-stone-100 rounded-md" />
                    </div>
                  </div>
                  <div className="h-8 w-28 bg-stone-100 rounded-full" />
                </div>
              ))}
            </div>
          ) : tutors.length === 0 ? (
            <div className="bg-white p-12 rounded-3xl border border-stone-200 text-center space-y-3">
              <Compass className="size-8 text-neutral-400 mx-auto" />
              <h3 className="font-heading font-bold text-base text-neutral-900">
                Không tìm thấy gia sư phù hợp
              </h3>
              <p className="text-xs text-neutral-500 max-w-md mx-auto">
                Hãy thử nới lỏng các tiêu chí bộ lọc (môn học, mức học phí hoặc khu vực) để xem thêm nhiều lựa chọn gia sư khác.
              </p>
              <button
                type="button"
                onClick={resetFilters}
                className="px-4 py-2 rounded-full bg-stone-100 hover:bg-stone-200 text-neutral-800 text-xs font-bold transition-colors cursor-pointer"
              >
                Đặt lại bộ lọc
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {tutors.map((tutor) => (
                <TutorCard
                  key={tutor.tutorId}
                  tutor={tutor}
                  onViewDetail={openTutor}
                  onRequestConnect={handleRequestConnect}
                />
              ))}
            </div>
          )}

          {/* Pagination Controls */}
          {pagination && pagination.totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t border-stone-200 text-xs">
              <span className="text-neutral-500 font-medium">
                Trang <strong className="text-neutral-900">{pagination.page + 1}</strong> / {pagination.totalPages}
              </span>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={pagination.first || isSearchLoading}
                  onClick={() => setPage(pagination.page - 1)}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full bg-white border border-stone-200 text-neutral-700 font-semibold hover:bg-stone-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                >
                  <ChevronLeft className="size-4" />
                  <span>Trang trước</span>
                </button>

                <button
                  type="button"
                  disabled={pagination.last || isSearchLoading}
                  onClick={() => setPage(pagination.page + 1)}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full bg-white border border-stone-200 text-neutral-700 font-semibold hover:bg-stone-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                >
                  <span>Trang sau</span>
                  <ChevronRight className="size-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* =========================================================================
          4. TAB 2: GỢI Ý PHÙ HỢP (Figma 36:3764 Matches Tab)
         ========================================================================= */}
      {activeTab === "matches" && (
        <AiMatchView
          matches={matches}
          isMatching={isMatching}
          error={matchError}
          need={need}
          onUpdateNeed={updateNeed}
          onViewDetail={openTutor}
          onRequestConnect={handleRequestConnect}
        />
      )}

      {/* =========================================================================
          5. TAB 3: YÊU CẦU ĐÃ GỬI (Figma 36:3936 Requests Tab)
         ========================================================================= */}
      {activeTab === "requests" && <SentRequests />}

      {/* 6. DETAIL MODAL */}
      {connectTutorId && matching.goal && <ConnectionRequestDialog
        tutorId={connectTutorId} goal={matching.goal} onClose={() => setConnectTutorId(null)}
        onSent={() => {
          setConnectTutorId(null);
          showToast("Đã gửi yêu cầu kết nối thành công.", "success");
          handleTabChange("requests");
        }}
      />}
      <TutorDetailModal
        isOpen={isDetailOpen}
        tutor={selectedTutor}
        isLoading={isDetailLoading}
        error={detailError}
        onClose={closeTutor}
        onRequestConnect={handleRequestConnect}
      />
    </div>
  );
}
