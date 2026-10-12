"use client";

import React from "react";
import { Star, CheckCircle2 } from "lucide-react";
import type { TutorDiscoveryCard } from "../types";

export interface TutorCardProps {
  tutor: TutorDiscoveryCard;
  onViewDetail?: (tutorId: string) => void;
  onRequestConnect?: (tutorId: string) => void;
}

export function TutorCard({
  tutor,
  onViewDetail,
  onRequestConnect,
}: TutorCardProps) {
  const formattedPrice = new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(tutor.pricePerSession);

  const subjectsText =
    tutor.subjects && tutor.subjects.length > 0
      ? tutor.subjects.map((s) => `${s.subjectName} · ${s.educationLevelName}`).join(", ")
      : "Chưa cập nhật môn";
  const modeText =
    tutor.teachingMode === "ONLINE"
      ? "Online"
      : tutor.teachingMode === "OFFLINE"
      ? "Offline"
      : "Online & Offline";
  const areaText = tutor.area
    ? tutor.area
    : tutor.teachingMode === "ONLINE"
    ? "Toàn quốc"
    : "Chưa cập nhật khu vực";

  return (
    <div className="bg-white rounded-2xl border border-stone-200/80 p-4 sm:p-5 hover:border-sky-300 hover:shadow-xs transition-all flex flex-col md:flex-row md:items-center justify-between gap-4">
      {/* 1. Left: Avatar + Basic Info */}
      <div className="flex items-start sm:items-center gap-3.5 flex-1 min-w-0">
        {/* Avatar */}
        <div className="size-12 rounded-full bg-sky-100 text-[#006DCC] flex items-center justify-center font-bold text-base shrink-0 shadow-2xs">
          {tutor.displayName.charAt(0).toUpperCase()}
        </div>

        {/* Col 1: Name, Subjects, Experience */}
        <div className="min-w-0 flex-1 sm:max-w-xs">
          <div className="flex items-center gap-1.5">
            <h4 className="font-heading font-bold text-[15px] text-neutral-900 truncate">
              {tutor.displayName}
            </h4>
            {tutor.verified && (
              <CheckCircle2 className="size-4 text-emerald-500 fill-emerald-50 shrink-0" />
            )}
          </div>
          <p className="text-xs text-neutral-600 font-medium truncate mt-0.5">
            {subjectsText}
          </p>
          <p className="text-[11px] text-neutral-400 mt-0.5 truncate">
            {tutor.headline || tutor.shortBio || (tutor.experienceYears ? `${tutor.experienceYears} năm kinh nghiệm` : "Gia sư tận tâm")}
          </p>
        </div>

        {/* Col 2: Rating, Mode/Location, Overlap Schedule */}
        <div className="hidden lg:flex flex-col gap-1 text-xs shrink-0 min-w-[210px]">
          <div className="flex items-center gap-1.5 font-semibold text-neutral-800 text-xs">
            <Star className="size-3.5 fill-amber-400 text-amber-400" />
            <span>
              {tutor.ratingAvg != null ? tutor.ratingAvg.toFixed(1) : "Chưa có đánh giá"} ({tutor.reviewCount ?? 0})
            </span>
          </div>
          <p className="text-neutral-500 text-[11px] truncate">
            {modeText} · {areaText}
          </p>
          <div className="inline-flex items-center gap-1 text-sky-700 text-[11px] font-medium">
            <span>{tutor.experienceYears ? `${tutor.experienceYears} năm kinh nghiệm` : "Đã xác minh chuyên môn"}</span>
          </div>
        </div>
      </div>

      {/* 2. Right: Price & Action Buttons */}
      <div className="flex items-center justify-between md:justify-end gap-4 w-full md:w-auto pt-3 md:pt-0 border-t md:border-t-0 border-stone-100 shrink-0">
        <div className="text-left md:text-right">
          <span className="font-heading font-bold text-sm sm:text-base text-neutral-900">
            {formattedPrice}
          </span>
          <span className="text-[11px] text-neutral-400"> / buổi</span>
        </div>

        <div className="flex items-center gap-2">
          {onViewDetail && (
            <button
              type="button"
              onClick={() => onViewDetail(tutor.tutorId)}
              className="px-4 py-2 rounded-full text-xs font-semibold text-neutral-700 bg-stone-100 hover:bg-stone-200 transition-colors cursor-pointer"
            >
              Xem hồ sơ
            </button>
          )}

          {onRequestConnect && (
            <button
              type="button"
              onClick={() => onRequestConnect(tutor.tutorId)}
              className="px-4 py-2 rounded-full text-xs font-semibold text-white bg-[#006DCC] hover:bg-[#005bb5] transition-colors cursor-pointer"
            >
              Gửi yêu cầu
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
