"use client";

import React, { useState } from "react";
import { Search, Compass, ChevronLeft, ChevronRight, Users, Sparkles } from "lucide-react";
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
    matches,
    isMatching,
    error: matchError,
    runMatching,
  } = useTutorMatching();

  const handleRequestConnect = (tutorId: string) => {
    showToast("Phụ huynh đã gửi yêu cầu kết nối! Gia sư sẽ liên hệ lại với gia đình sớm nhất.", "success");
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-900 rounded-full text-xs font-bold mb-2">
            <Users className="size-3.5 text-emerald-600" />
            <span>Gia Sư Cho Con Em (Parent Discovery)</span>
          </div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Tìm Gia Sư Dạy Kèm Cho Con
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            Chọn lọc gia sư uy tín có chứng chỉ sư phạm, kinh nghiệm luyện thi và biểu phí minh bạch
          </p>
        </div>

        {/* Tab Buttons */}
        <div className="flex rounded-2xl bg-stone-100 p-1 border border-stone-200/80 shrink-0 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => setActiveTab("browse")}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === "browse"
                ? "bg-white text-neutral-900 shadow-2xs"
                : "text-neutral-500 hover:text-neutral-900"
            }`}
          >
            <Search className="size-3.5" />
            <span>Tất cả gia sư</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab("matches");
              if (matches.length === 0) {
                runMatching("00000000-0000-0000-0000-000000000001");
              }
            }}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === "matches"
                ? "bg-emerald-600 text-white shadow-xs"
                : "text-neutral-500 hover:text-neutral-900"
            }`}
          >
            <Sparkles className="size-3.5" />
            <span>AI Match cho con</span>
          </button>
        </div>
      </div>

      {/* 2. TAB: AI MATCHES */}
      {activeTab === "matches" && (
        <AiMatchView
          matches={matches}
          isMatching={isMatching}
          error={matchError}
          onRefresh={() => runMatching("00000000-0000-0000-0000-000000000001")}
          onViewDetail={openTutor}
          onRequestConnect={handleRequestConnect}
        />
      )}

      {/* 3. TAB: BROWSE ALL */}
      {activeTab === "browse" && (
        <div className="space-y-6">
          <TutorSearchFilters
            params={params}
            onUpdateFilters={updateFilters}
            onResetFilters={resetFilters}
            totalResults={pagination?.totalElements}
          />

          {searchError && (
            <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium">
              {searchError}
            </div>
          )}

          {isSearchLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div
                  key={i}
                  className="h-64 rounded-3xl bg-white border border-stone-200/60 p-6 animate-pulse flex flex-col justify-between"
                >
                  <div className="flex gap-3">
                    <div className="size-12 rounded-2xl bg-stone-100" />
                    <div className="space-y-2 flex-1">
                      <div className="h-4 w-32 bg-stone-100 rounded-md" />
                      <div className="h-3 w-48 bg-stone-100 rounded-md" />
                    </div>
                  </div>
                  <div className="h-10 w-full bg-stone-100 rounded-xl" />
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
                Quý phụ huynh hãy thử chọn mở rộng phạm vi khu vực hoặc nới lỏng các yêu cầu môn học.
              </p>
              <button
                type="button"
                onClick={resetFilters}
                className="px-4 py-2 rounded-xl bg-stone-100 hover:bg-stone-200 text-neutral-800 text-xs font-bold transition-colors cursor-pointer"
              >
                Đặt lại bộ lọc
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
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
                  className="inline-flex items-center gap-1 px-3 py-2 rounded-xl bg-white border border-stone-200 text-neutral-700 font-bold hover:bg-stone-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                >
                  <ChevronLeft className="size-4" />
                  <span>Trang trước</span>
                </button>

                <button
                  type="button"
                  disabled={pagination.last || isSearchLoading}
                  onClick={() => setPage(pagination.page + 1)}
                  className="inline-flex items-center gap-1 px-3 py-2 rounded-xl bg-white border border-stone-200 text-neutral-700 font-bold hover:bg-stone-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                >
                  <span>Trang sau</span>
                  <ChevronRight className="size-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 4. DETAIL MODAL */}
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
