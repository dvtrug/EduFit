"use client";

import React, { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import {
  Compass,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  Send,
  Clock,
  CheckCircle2,
  Calendar,
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

// Dữ liệu mẫu danh sách yêu cầu đã gửi (Figma 36:3936 Requests Tab)
const SAMPLE_SENT_REQUESTS = [
  {
    id: "req-1",
    tutorName: "Trần Thu Hà",
    subject: "Toán · Lớp 9",
    sentAt: "10:30 Hôm qua",
    status: "PENDING",
    statusText: "Đang chờ gia sư phản hồi",
    preferredTime: "Tối Thứ 3, 5 (19:30 - 21:00)",
  },
  {
    id: "req-2",
    tutorName: "Lê Minh Quân",
    subject: "Toán, Vật lý · Lớp 9",
    sentAt: "08:15 Ngày 10/10",
    status: "ACCEPTED",
    statusText: "Gia sư đã đồng ý kết nối",
    preferredTime: "Tối Thứ 4, 6 (18:00 - 19:30)",
  },
];

export default function StudentTutorsPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { showToast } = useToast();

  const tabParam = searchParams.get("tab");
  const [activeTab, setActiveTab] = useState<"browse" | "matches" | "requests">(
    tabParam === "matches"
      ? "matches"
      : tabParam === "requests"
      ? "requests"
      : "browse"
  );

  // Sync tab with URL
  useEffect(() => {
    if (tabParam === "matches") {
      setActiveTab("matches");
    } else if (tabParam === "requests") {
      setActiveTab("requests");
    } else {
      setActiveTab("browse");
    }
  }, [tabParam]);

  const handleTabChange = (tab: "browse" | "matches" | "requests") => {
    setActiveTab(tab);
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

  // Tự động chạy Matching khi chuyển qua tab matches nếu danh sách chưa có
  useEffect(() => {
    if (activeTab === "matches" && matches.length === 0 && !isMatching) {
      runMatching();
    }
  }, [activeTab, matches.length, isMatching, runMatching]);

  const handleRequestConnect = (tutorId: string) => {
    showToast(
      "Đã gửi yêu cầu kết nối học thử 1-1 tới gia sư thành công!",
      "success"
    );
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
            {matches.length || 6}
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
            {SAMPLE_SENT_REQUESTS.length}
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
          onRefresh={() => runMatching()}
          onViewDetail={openTutor}
          onRequestConnect={handleRequestConnect}
        />
      )}

      {/* =========================================================================
          5. TAB 3: YÊU CẦU ĐÃ GỬI (Figma 36:3936 Requests Tab)
         ========================================================================= */}
      {activeTab === "requests" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs">
            <h4 className="font-heading font-bold text-sm text-neutral-900">
              Danh sách kết nối đang chờ phản hồi
            </h4>
            <span className="text-neutral-400 text-xs">
              Tự động cập nhật mỗi khi gia sư xác nhận
            </span>
          </div>

          <div className="space-y-3">
            {SAMPLE_SENT_REQUESTS.map((req) => (
              <div
                key={req.id}
                className="bg-white rounded-2xl border border-stone-200/80 p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div className="flex items-start sm:items-center gap-3.5">
                  <div className="size-11 rounded-full bg-sky-100 text-[#006DCC] font-bold text-sm flex items-center justify-center shrink-0">
                    {req.tutorName.charAt(0)}
                  </div>
                  <div>
                    <h5 className="font-heading font-bold text-sm text-neutral-900">
                      Gia sư {req.tutorName}
                    </h5>
                    <p className="text-xs text-neutral-600 font-medium mt-0.5">
                      {req.subject}
                    </p>
                    <p className="text-[11px] text-neutral-400 mt-0.5 flex items-center gap-1.5">
                      <Clock className="size-3 text-neutral-400" />
                      <span>Đã gửi {req.sentAt} · Khung giờ: {req.preferredTime}</span>
                    </p>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-3 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-stone-100">
                  <span
                    className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold ${
                      req.status === "ACCEPTED"
                        ? "bg-emerald-50 text-emerald-700"
                        : "bg-amber-50 text-amber-800"
                    }`}
                  >
                    {req.status === "ACCEPTED" ? (
                      <CheckCircle2 className="size-3.5 text-emerald-600" />
                    ) : (
                      <Clock className="size-3.5 text-amber-600" />
                    )}
                    <span>{req.statusText}</span>
                  </span>

                  {req.status === "ACCEPTED" && (
                    <button
                      type="button"
                      onClick={() => router.push("/student/schedule")}
                      className="px-3.5 py-1.5 rounded-full text-xs font-semibold bg-[#006DCC] text-white hover:bg-[#005bb5] transition-colors"
                    >
                      Vào xếp lịch học
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 6. DETAIL MODAL */}
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
