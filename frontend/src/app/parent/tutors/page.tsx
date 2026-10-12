"use client";

import React, { useState } from "react";
import { Compass, ChevronLeft, ChevronRight, ShieldCheck } from "lucide-react";
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

export default function ParentTutorsPage() {
  const { showToast } = useToast();
  const [activeTab, setActiveTab] = useState<"browse" | "matches">("browse");

  // 1. Search Hook
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
    openTutor,
    closeTutor,
  } = useTutorDetail();

  // 3. AI Matching Hook
  const {
    need,
    matches,
    isMatching,
    error: matchError,
    updateNeed,
    runMatching,
  } = useTutorMatching();

  const handleRequestConnect = (_tutorId: string) => {
    showToast(
      "Phụ huynh đã gửi yêu cầu kết nối! Gia sư sẽ liên hệ lại với gia đình sớm nhất.",
      "success"
    );
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* 1. Header (Figma Heading) */}
      <div className="space-y-1">
        <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
          Tìm gia sư phù hợp cho con
        </h2>
        <p className="text-xs sm:text-sm text-neutral-500 font-medium">
          Chọn lọc gia sư uy tín có chứng chỉ sư phạm, kinh nghiệm luyện thi và biểu phí minh bạch.
        </p>
      </div>

      {/* 2. Tabs Navigator */}
      <div className="flex items-center gap-6 border-b border-stone-200/90 text-xs font-semibold">
        <button
          type="button"
          onClick={() => setActiveTab("browse")}
          className={`pb-3 border-b-2 transition-all cursor-pointer flex items-center gap-1.5 ${
            activeTab === "browse"
              ? "border-[#006DCC] text-[#006DCC] font-bold"
              : "border-transparent text-neutral-500 hover:text-neutral-900"
          }`}
        >
          <span>Khám phá</span>
        </button>

        <button
          type="button"
          onClick={() => {
            setActiveTab("matches");
            if (matches.length === 0) runMatching();
          }}
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
      </div>

      {/* 3. TAB: KHÁM PHÁ (Discover) */}
      {activeTab === "browse" && (
        <div className="space-y-4">
          <TutorSearchFilters
            params={params}
            onUpdateFilters={updateFilters}
            onResetFilters={resetFilters}
            totalResults={pagination?.totalElements}
          />

          {/* Results count & note */}
          <div className="flex items-center justify-between text-xs pt-2">
            <span className="font-heading font-bold text-sm text-neutral-900">
              {pagination?.totalElements ?? tutors.length} gia sư phù hợp
            </span>
            <span className="text-[11px] text-neutral-400 flex items-center gap-1 font-medium">
              <ShieldCheck className="size-3.5 text-emerald-500" />
              <span>Chỉ hiển thị gia sư đã xác minh</span>
            </span>
          </div>

          {searchError && (
            <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium">
              {searchError}
            </div>
          )}

          {isSearchLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
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

      {/* 4. TAB: GỢI Ý PHÙ HỢP (AI Matches) */}
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

      {/* 5. Detail Modal */}
      <TutorDetailModal
        isOpen={isDetailOpen}
        tutor={selectedTutor}
        isLoading={isDetailLoading}
        onClose={closeTutor}
        onRequestConnect={handleRequestConnect}
      />
    </div>
  );
}
