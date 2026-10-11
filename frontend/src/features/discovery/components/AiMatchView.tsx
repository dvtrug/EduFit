"use client";

import React from "react";
import { Sparkles, Trophy, CheckCircle2, ChevronRight, RotateCcw, AlertCircle } from "lucide-react";
import { TutorCard } from "./TutorCard";
import { AiExplanationBadge } from "./AiExplanationBadge";
import type { TutorMatchResult } from "../types";

export interface AiMatchViewProps {
  matches: TutorMatchResult[];
  isMatching: boolean;
  error: string | null;
  onRefresh?: () => void;
  onViewDetail?: (tutorId: string) => void;
  onRequestConnect?: (tutorId: string) => void;
}

export function AiMatchView({
  matches,
  isMatching,
  error,
  onRefresh,
  onViewDetail,
  onRequestConnect,
}: AiMatchViewProps) {
  if (isMatching) {
    return (
      <div className="p-12 sm:p-16 rounded-3xl bg-white border border-stone-200/90 shadow-2xs flex flex-col items-center justify-center text-center space-y-4">
        <div className="relative">
          <div className="size-16 rounded-3xl bg-amber-100 flex items-center justify-center text-amber-600 animate-pulse">
            <Sparkles className="size-8 text-amber-500 animate-spin" />
          </div>
        </div>
        <div className="space-y-1">
          <h3 className="font-heading font-bold text-lg text-neutral-900">
            AI đang phân tích và tìm kiếm gia sư tương thích...
          </h3>
          <p className="text-xs text-neutral-500 max-w-md">
            Hệ thống đang so khớp 5 tiêu chí: Môn học, Cấp lớp, Khung giờ rảnh, Khoảng cách địa lý và Học phí kỳ vọng.
          </p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 rounded-3xl bg-white border border-rose-200 shadow-2xs space-y-3 text-center">
        <AlertCircle className="size-8 text-rose-500 mx-auto" />
        <h3 className="font-heading font-bold text-base text-neutral-900">Không thể thực hiện ghép cặp AI</h3>
        <p className="text-xs text-neutral-500 max-w-md mx-auto">{error}</p>
        {onRefresh && (
          <button
            type="button"
            onClick={onRefresh}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-stone-100 hover:bg-stone-200 text-neutral-800 rounded-xl text-xs font-bold transition-colors cursor-pointer mt-2"
          >
            <RotateCcw className="size-3.5" />
            <span>Thử lại</span>
          </button>
        )}
      </div>
    );
  }

  if (matches.length === 0) {
    return (
      <div className="p-10 rounded-3xl bg-white border border-stone-200/90 shadow-2xs text-center space-y-3">
        <Sparkles className="size-8 text-amber-500 mx-auto" />
        <h3 className="font-heading font-bold text-base text-neutral-900">Chưa có kết quả gợi ý AI</h3>
        <p className="text-xs text-neutral-500 max-w-md mx-auto">
          Nhấn nút bên dưới để hệ thống tự động quét và gợi ý Top 5 gia sư phù hợp nhất theo mục tiêu của bạn.
        </p>
        {onRefresh && (
          <button
            type="button"
            onClick={onRefresh}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-bold shadow-xs transition-colors cursor-pointer mt-2"
          >
            <Sparkles className="size-4" />
            <span>Chạy EduFit AI Match</span>
          </button>
        )}
      </div>
    );
  }

  const topMatch = matches[0];
  const remainingMatches = matches.slice(1);

  return (
    <div className="space-y-6">
      {/* 1. TOP 1 MATCH CARD (HERO SPOTLIGHT) */}
      <div className="bg-white rounded-3xl border-2 border-amber-300 ring-4 ring-amber-400/10 shadow-md p-6 sm:p-8 space-y-6 overflow-hidden relative">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-100">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-100 text-amber-800">
              <Trophy className="size-5 text-amber-600" />
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-900 text-[10px] font-extrabold uppercase tracking-wider">
                Lựa Chọn Tối Ưu Nhất
              </div>
              <h3 className="font-heading font-black text-xl text-neutral-900 mt-0.5">
                Gia sư phù hợp nhất: {topMatch.tutorInfo.displayName}
              </h3>
            </div>
          </div>

          <div className="flex items-baseline gap-1 text-right">
            <span className="text-3xl font-black text-amber-600 tracking-tight">
              {Math.round(topMatch.matchScore)}%
            </span>
            <span className="text-xs text-neutral-400 font-bold uppercase">Tương thích</span>
          </div>
        </div>

        {/* AI Explanation Box */}
        {topMatch.explanation && (
          <AiExplanationBadge explanation={topMatch.explanation} aiGenerated={topMatch.aiGenerated} />
        )}

        {/* 5-Factor Score Breakdown Progress */}
        <div className="space-y-3 p-4 rounded-2xl bg-stone-50 border border-stone-200/80">
          <span className="text-[11px] font-bold text-neutral-700 block">
            Chi tiết chỉ số tương thích 5 yếu tố:
          </span>

          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-neutral-500">Môn học</span>
                <span className="font-bold text-neutral-800">{Math.round(topMatch.scoreBreakdown.subjectFit)}%</span>
              </div>
              <div className="h-1.5 w-full bg-stone-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-emerald-500 rounded-full"
                  style={{ width: `${topMatch.scoreBreakdown.subjectFit}%` }}
                />
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-neutral-500">Cấp lớp</span>
                <span className="font-bold text-neutral-800">{Math.round(topMatch.scoreBreakdown.levelFit)}%</span>
              </div>
              <div className="h-1.5 w-full bg-stone-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-emerald-500 rounded-full"
                  style={{ width: `${topMatch.scoreBreakdown.levelFit}%` }}
                />
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-neutral-500">Lịch rảnh ({topMatch.scoreBreakdown.overlappingSlots} ca)</span>
                <span className="font-bold text-neutral-800">{Math.round(topMatch.scoreBreakdown.scheduleFit)}%</span>
              </div>
              <div className="h-1.5 w-full bg-stone-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-sky-500 rounded-full"
                  style={{ width: `${topMatch.scoreBreakdown.scheduleFit}%` }}
                />
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-neutral-500">Ngân sách</span>
                <span className="font-bold text-neutral-800">{Math.round(topMatch.scoreBreakdown.budgetFit)}%</span>
              </div>
              <div className="h-1.5 w-full bg-stone-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-amber-500 rounded-full"
                  style={{ width: `${topMatch.scoreBreakdown.budgetFit}%` }}
                />
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-[11px]">
                <span className="text-neutral-500">Đánh giá</span>
                <span className="font-bold text-neutral-800">{Math.round(topMatch.scoreBreakdown.ratingFit)}%</span>
              </div>
              <div className="h-1.5 w-full bg-stone-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-purple-500 rounded-full"
                  style={{ width: `${topMatch.scoreBreakdown.ratingFit}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Action Button */}
        <div className="flex flex-wrap items-center justify-between gap-4 pt-2">
          <div className="text-xs text-neutral-500">
            Đề xuất học phí: <strong className="text-neutral-900 text-sm">{new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(topMatch.tutorInfo.pricePerSession)}</strong> /buổi
          </div>

          <div className="flex items-center gap-2">
            {onViewDetail && (
              <button
                type="button"
                onClick={() => onViewDetail(topMatch.tutorId)}
                className="px-4 py-2.5 rounded-xl text-xs font-bold text-neutral-700 bg-stone-100 hover:bg-stone-200 transition-colors cursor-pointer"
              >
                Xem chi tiết hồ sơ
              </button>
            )}
            {onRequestConnect && (
              <button
                type="button"
                onClick={() => onRequestConnect(topMatch.tutorId)}
                className="inline-flex items-center gap-1.5 px-6 py-2.5 rounded-xl text-xs font-bold text-white bg-amber-500 hover:bg-amber-600 shadow-xs transition-colors cursor-pointer"
              >
                <span>Gửi lời mời học thử ngay</span>
                <ChevronRight className="size-4" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 2. REMAINING MATCHES (Top 2..5) */}
      {remainingMatches.length > 0 && (
        <div className="space-y-4 pt-4">
          <div className="flex items-center justify-between">
            <h4 className="font-heading font-bold text-base text-neutral-900">
              Các gia sư đề xuất tiếp theo ({remainingMatches.length})
            </h4>
            {onRefresh && (
              <button
                type="button"
                onClick={onRefresh}
                className="inline-flex items-center gap-1.5 text-xs text-neutral-500 hover:text-neutral-800 font-bold transition-colors cursor-pointer"
              >
                <RotateCcw className="size-3.5" />
                <span>Làm mới gợi ý</span>
              </button>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {remainingMatches.map((m) => (
              <TutorCard
                key={m.tutorId}
                tutor={m.tutorInfo}
                matchScore={m.matchScore}
                onViewDetail={onViewDetail}
                onRequestConnect={onRequestConnect}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
