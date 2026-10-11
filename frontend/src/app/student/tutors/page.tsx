"use client";

import React, { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Sparkles, Search, Compass, ChevronLeft, ChevronRight, GraduationCap } from "lucide-react";
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

export default function StudentTutorsPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { showToast } = useToast();

  const tabParam = searchParams.get("tab");
  const [activeTab, setActiveTab] = useState<"browse" | "matches">(
    tabParam === "matches" ? "matches" : "browse"
  );

  // Sync tab with URL
  useEffect(() => {
    if (tabParam === "matches") {
      setActiveTab("matches");
    } else {
      setActiveTab("browse");
    }
  }, [tabParam]);

  const handleTabChange = (tab: "browse" | "matches") => {
    setActiveTab(tab);
    if (tab === "matches") {
      router.push("/student/tutors?tab=matches");
    } else {
      router.push("/student/tutors");
    }
  };

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
  const [studentGoalId, setStudentGoalId] = useState<string>("00000000-0000-0000-0000-000000000001");
  const {
    matches,
    isMatching,
    error: matchError,
    runMatching,
  } = useTutorMatching({ defaultGoalId: studentGoalId });

  // Load student goal if available
  useEffect(() => {
    const savedGoal = localStorage.getItem("edufit_student_onboarding_goal");
    if (savedGoal) {
      try {
        const parsed = JSON.parse(savedGoal);
        if (parsed.id) setStudentGoalId(parsed.id);
      } catch {
        // ignore
      }
    }
  }, []);

  // Auto trigger AI match when entering matches tab if empty
  useEffect(() => {
    if (activeTab === "matches" && matches.length === 0 && !isMatching) {
      runMatching(studentGoalId);
    }
  }, [activeTab, matches.length, isMatching, runMatching, studentGoalId]);

  const handleRequestConnect = (tutorId: string) => {
    showToast("Đã lưu yêu cầu kết nối! Tính năng gửi lời mời học thử 1-1 sẽ đồng bộ với lịch học của bạn.", "success");
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* 1. Header Banner & Tab Switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-50 text-amber-900 rounded-full text-xs font-bold mb-2">
            <GraduationCap className="size-3.5 text-amber-600" />
            <span>Khám Phá & Kết Nối Gia Sư (Tutor Discovery)</span>
          </div>
          <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-neutral-900 tracking-tight">
            Tìm Gia Sư Dạy Kèm Phù Hợp
          </h2>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1 font-medium">
            Hơn 500+ gia sư tài năng đã xác minh bằng cấp KYC, sẵn sàng đồng hành nâng cao điểm số của bạn
          </p>
        </div>

        {/* Tab Buttons */}
        <div className="flex rounded-2xl bg-stone-100 p-1 border border-stone-200/80 shrink-0 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => handleTabChange("browse")}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === "browse"
                ? "bg-white text-neutral-900 shadow-2xs"
                : "text-neutral-500 hover:text-neutral-900"
            }`}
          >
            <Search className="size-3.5" />
            <span>Khám phá tất cả</span>
          </button>
          <button
            type="button"
            onClick={() => handleTabChange("matches")}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === "matches"
                ? "bg-amber-500 text-white shadow-xs"
                : "text-neutral-500 hover:text-neutral-900"
            }`}
          >
            <Sparkles className="size-3.5" />
            <span>Gợi ý AI Match</span>
          </button>
        </div>
      </div>

      {/* 2. TAB: AI MATCHES */}
      {activeTab === "matches" && (
        <AiMatchView
          matches={matches}
          isMatching={isMatching}
          error={matchError}
          onRefresh={() => runMatching(studentGoalId)}
          onViewDetail={openTutor}
          onRequestConnect={handleRequestConnect}
        />
      )}

      {/* 3. TAB: BROWSE ALL TUTORS */}
      {activeTab === "browse" && (
        <div className="space-y-6">
          {/* Filter Bar */}
          <TutorSearchFilters
            params={params}
            onUpdateFilters={updateFilters}
            onResetFilters={resetFilters}
            totalResults={pagination?.totalElements}
          />

          {/* Error notice (nếu có) */}
          {searchError && (
            <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium">
              {searchError}
            </div>
          )}

          {/* Tutors Grid */}
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
                Hãy thử nới lỏng các tiêu chí bộ lọc (môn học, mức học phí hoặc khu vực) để xem thêm nhiều lựa chọn gia sư khác.
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
