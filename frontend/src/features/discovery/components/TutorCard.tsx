"use client";

import React from "react";
import { Star, ShieldCheck, MapPin, Video, Home, BookOpen, ChevronRight, Sparkles } from "lucide-react";
import type { TutorDiscoveryCard } from "../types";

export interface TutorCardProps {
  tutor: TutorDiscoveryCard;
  onViewDetail?: (tutorId: string) => void;
  onRequestConnect?: (tutorId: string) => void;
  matchScore?: number;
  highlightMatch?: boolean;
}

export function TutorCard({
  tutor,
  onViewDetail,
  onRequestConnect,
  matchScore,
  highlightMatch = false,
}: TutorCardProps) {
  const formattedPrice = new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(tutor.pricePerSession);

  return (
    <div
      className={`group relative bg-white rounded-3xl border transition-all duration-200 flex flex-col justify-between overflow-hidden shadow-2xs hover:shadow-md ${
        highlightMatch
          ? "border-amber-300 ring-2 ring-amber-400/20 bg-gradient-to-b from-amber-50/30 to-white"
          : "border-stone-200/90 hover:border-amber-300"
      }`}
    >
      {/* Header Match Badge (nếu có) */}
      {matchScore !== undefined && (
        <div className="bg-gradient-to-r from-amber-500 to-orange-500 text-white px-4 py-1.5 flex items-center justify-between text-xs font-bold">
          <span className="flex items-center gap-1.5">
            <Sparkles className="size-3.5" />
            AI Match Tương thích
          </span>
          <span className="text-sm font-black tracking-tight">{Math.round(matchScore)}%</span>
        </div>
      )}

      <div className="p-5 sm:p-6 space-y-4">
        {/* Top: Avatar & Info */}
        <div className="flex items-start gap-3.5">
          {/* Avatar Placeholder */}
          <div className="size-13 rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-400 text-white font-heading font-black text-xl flex items-center justify-center shrink-0 shadow-2xs">
            {tutor.displayName.charAt(0).toUpperCase()}
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5">
              <h3 className="font-heading font-bold text-base text-neutral-900 truncate">
                {tutor.displayName}
              </h3>
              {tutor.verified && (
                <span title="Gia sư đã xác minh bằng cấp KYC">
                  <ShieldCheck className="size-4 text-emerald-500 shrink-0" />
                </span>
              )}
            </div>

            {tutor.headline ? (
              <p className="text-xs text-neutral-500 truncate mt-0.5">{tutor.headline}</p>
            ) : (
              <p className="text-xs text-neutral-400 italic mt-0.5">Gia sư EduFit đã kiểm duyệt</p>
            )}

            {/* Rating & Mode Badge */}
            <div className="flex flex-wrap items-center gap-2 mt-2">
              <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-50 text-amber-900 text-[11px] font-bold">
                <Star className="size-3 fill-amber-400 text-amber-400" />
                <span>{tutor.ratingAvg ? tutor.ratingAvg.toFixed(1) : "5.0"}</span>
                <span className="text-neutral-400 font-normal">({tutor.reviewCount})</span>
              </div>

              <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-stone-100 text-neutral-600 text-[11px] font-medium">
                {tutor.teachingMode === "ONLINE" && (
                  <>
                    <Video className="size-3 text-sky-500" />
                    <span>Dạy Online</span>
                  </>
                )}
                {tutor.teachingMode === "OFFLINE" && (
                  <>
                    <Home className="size-3 text-emerald-500" />
                    <span>Dạy Offline</span>
                  </>
                )}
                {tutor.teachingMode === "BOTH" && (
                  <>
                    <Video className="size-3 text-sky-500" />
                    <span>Online / Offline</span>
                  </>
                )}
              </div>

              {tutor.area && (
                <div className="inline-flex items-center gap-1 text-[11px] text-neutral-400 truncate max-w-[120px]">
                  <MapPin className="size-3 shrink-0" />
                  <span className="truncate">{tutor.area}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Short Bio */}
        {tutor.shortBio && (
          <p className="text-xs text-neutral-600 line-clamp-2 leading-relaxed">
            {tutor.shortBio}
          </p>
        )}

        {/* Subjects list */}
        {tutor.subjects && tutor.subjects.length > 0 && (
          <div className="pt-2 border-t border-stone-100">
            <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block mb-1.5">
              Môn dạy & Cấp lớp
            </span>
            <div className="flex flex-wrap gap-1.5">
              {tutor.subjects.slice(0, 3).map((sub, idx) => (
                <span
                  key={`${sub.subjectId}-${sub.educationLevelId}-${idx}`}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-stone-50 border border-stone-200/80 text-[11px] font-semibold text-neutral-700"
                >
                  <BookOpen className="size-3 text-amber-600" />
                  <span>
                    {sub.subjectName} · {sub.educationLevelName}
                  </span>
                </span>
              ))}
              {tutor.subjects.length > 3 && (
                <span className="px-2 py-1 rounded-xl bg-stone-50 text-[10px] font-bold text-neutral-400">
                  +{tutor.subjects.length - 3} môn khác
                </span>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Footer: Price & Actions */}
      <div className="p-4 sm:p-5 bg-stone-50/70 border-t border-stone-100 flex items-center justify-between gap-3">
        <div>
          <span className="text-[10px] text-neutral-400 block font-medium">Học phí đề xuất</span>
          <span className="text-sm font-bold text-neutral-900">{formattedPrice}</span>
          <span className="text-[10px] text-neutral-400"> /buổi</span>
        </div>

        <div className="flex items-center gap-2">
          {onViewDetail && (
            <button
              type="button"
              onClick={() => onViewDetail(tutor.tutorId)}
              className="px-3.5 py-2 rounded-xl text-xs font-bold text-neutral-700 bg-white hover:bg-stone-100 border border-stone-200 transition-colors cursor-pointer"
            >
              Chi tiết
            </button>
          )}

          {onRequestConnect && (
            <button
              type="button"
              onClick={() => onRequestConnect(tutor.tutorId)}
              className="inline-flex items-center gap-1 px-4 py-2 rounded-xl text-xs font-bold text-white bg-amber-500 hover:bg-amber-600 shadow-2xs transition-colors cursor-pointer"
            >
              <span>Kết nối</span>
              <ChevronRight className="size-3" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
