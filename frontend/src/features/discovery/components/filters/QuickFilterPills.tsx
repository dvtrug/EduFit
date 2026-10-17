"use client";

import React from "react";
import { ChevronDown, SlidersHorizontal, X } from "lucide-react";
import type { SubjectItem, EducationLevel } from "@/services/catalog";
import type { TutorSearchParams, TeachingMode } from "../../types";
import { MAJOR_VIETNAM_CITIES } from "@/shared/constants/locations";

export interface QuickFilterPillsProps {
  params: TutorSearchParams;
  subjects: SubjectItem[];
  levels: EducationLevel[];
  selectedSubject?: SubjectItem;
  selectedLevel?: EducationLevel;
  showAdvanced: boolean;
  hasActiveFilters: boolean;
  onUpdateFilters: (filters: Partial<TutorSearchParams>) => void;
  onResetFilters: () => void;
  onToggleAdvanced: () => void;
  onClearKeywordInput: () => void;
}

const DAY_OPTIONS = [
  { value: 1, label: "Thứ 2" },
  { value: 2, label: "Thứ 3" },
  { value: 3, label: "Thứ 4" },
  { value: 4, label: "Thứ 5" },
  { value: 5, label: "Thứ 6" },
  { value: 6, label: "Thứ 7" },
  { value: 7, label: "Chủ Nhật" },
];

export function QuickFilterPills({
  params,
  subjects,
  levels,
  selectedSubject,
  selectedLevel,
  showAdvanced,
  hasActiveFilters,
  onUpdateFilters,
  onResetFilters,
  onToggleAdvanced,
  onClearKeywordInput,
}: QuickFilterPillsProps) {
  const handleModeChange = (mode?: TeachingMode) => {
    onUpdateFilters({ mode: params.mode === mode ? undefined : mode });
  };

  const handleSubjectToggle = (subjectId: number) => {
    onUpdateFilters({
      subjectId: params.subjectId === subjectId ? undefined : subjectId,
    });
  };

  const handleLevelToggle = (levelId: number) => {
    onUpdateFilters({
      levelId: params.levelId === levelId ? undefined : levelId,
    });
  };

  const handleRatingToggle = (rating: number) => {
    onUpdateFilters({
      minRating: params.minRating === rating ? undefined : rating,
    });
  };

  const handleAreaSelect = (area: string) => {
    onUpdateFilters({
      area: params.area === area ? undefined : area,
    });
  };

  return (
    <div className="flex flex-wrap items-center gap-2 pt-0.5">
      {/* Pill 1: Môn học */}
      {selectedSubject ? (
        <button
          type="button"
          onClick={() => onUpdateFilters({ subjectId: undefined })}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-[#006DCC] text-white shadow-2xs transition-all cursor-pointer"
        >
          <span>{selectedSubject.name}</span>
          <X className="size-3" />
        </button>
      ) : (
        <button
          type="button"
          onClick={() => {
            const math = subjects.find((s) => s.name.toLowerCase().includes("toán"));
            if (math) handleSubjectToggle(math.subjectId);
          }}
          className="px-3.5 py-1.5 rounded-full text-xs font-medium bg-card border border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800 transition-colors cursor-pointer shadow-2xs"
        >
          Toán
        </button>
      )}

      {/* Pill 2: Cấp lớp */}
      {selectedLevel ? (
        <button
          type="button"
          onClick={() => onUpdateFilters({ levelId: undefined })}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-[#006DCC] text-white shadow-2xs transition-all cursor-pointer"
        >
          <span>{selectedLevel.name}</span>
          <X className="size-3" />
        </button>
      ) : (
        <button
          type="button"
          onClick={() => {
            const grade9 = levels.find((l) => l.name.includes("9"));
            if (grade9) handleLevelToggle(grade9.levelId);
          }}
          className="px-3.5 py-1.5 rounded-full text-xs font-medium bg-card border border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800 transition-colors cursor-pointer shadow-2xs"
        >
          Lớp 9
        </button>
      )}

      {/* Pill 3: Online */}
      <button
        type="button"
        onClick={() => handleModeChange("ONLINE")}
        className={`px-3.5 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer shadow-2xs ${
          params.mode === "ONLINE"
            ? "bg-[#006DCC] text-white"
            : "bg-card border border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800"
        }`}
      >
        Online {params.mode === "ONLINE" && "✓"}
      </button>

      {/* Pill 4: Khu vực */}
      <div className="relative inline-block">
        <select
          value={params.area || ""}
          onChange={(e) => handleAreaSelect(e.target.value)}
          aria-label="Lọc theo khu vực"
          className={`appearance-none pl-3 pr-7 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer shadow-2xs ${
            params.area
              ? "bg-[#006DCC] text-white border-transparent"
              : "bg-card border border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800"
          }`}
        >
          <option value="" className="text-neutral-900 bg-card">Khu vực</option>
          {MAJOR_VIETNAM_CITIES.map((a) => (
            <option key={a} value={a} className="text-neutral-900 bg-card">
              {a}
            </option>
          ))}
        </select>
        <ChevronDown
          className={`size-3 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none ${
            params.area ? "text-white" : "text-neutral-400"
          }`}
        />
      </div>

      {/* Pill 5: Lịch rảnh */}
      <div className="relative inline-block">
        <select
          value={params.dayOfWeek || ""}
          onChange={(e) => {
            const val = e.target.value ? Number(e.target.value) : undefined;
            onUpdateFilters({
              dayOfWeek: val,
              availableFrom: val ? "00:00:00" : undefined,
              availableTo: val ? "23:59:59" : undefined,
            });
          }}
          aria-label="Lọc theo lịch rảnh trong tuần"
          className={`appearance-none pl-3 pr-7 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer shadow-2xs ${
            params.dayOfWeek
              ? "bg-[#006DCC] text-white border-transparent"
              : "bg-card border border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800"
          }`}
        >
          <option value="" className="text-neutral-900 bg-card">Lịch rảnh</option>
          {DAY_OPTIONS.map((d) => (
            <option key={d.value} value={d.value} className="text-neutral-900 bg-card">
              {d.label}
            </option>
          ))}
        </select>
        <ChevronDown
          className={`size-3 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none ${
            params.dayOfWeek ? "text-white" : "text-neutral-400"
          }`}
        />
      </div>

      {/* Pill 6: Rating */}
      <button
        type="button"
        onClick={() => handleRatingToggle(4.5)}
        className={`px-3.5 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer shadow-2xs ${
          params.minRating === 4.5
            ? "bg-[#006DCC] text-white"
            : "bg-card border border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800"
        }`}
      >
        Rating 4.5+ {params.minRating === 4.5 && "★"}
      </button>

      {/* Nút mở bộ lọc nâng cao */}
      <button
        type="button"
        onClick={onToggleAdvanced}
        className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors cursor-pointer shadow-2xs ${
          showAdvanced
            ? "bg-sky-50 dark:bg-sky-950 border-sky-300 dark:border-sky-800 text-[#006DCC]"
            : "bg-card border-stone-200 dark:border-stone-800 text-neutral-700 dark:text-neutral-300 hover:bg-stone-50 dark:hover:bg-stone-800"
        }`}
      >
        <SlidersHorizontal className="size-3" />
        <span>Bộ lọc chi tiết</span>
      </button>

      {/* Xóa bộ lọc */}
      {hasActiveFilters && (
        <button
          type="button"
          onClick={() => {
            onClearKeywordInput();
            onResetFilters();
          }}
          className="text-xs font-medium text-[#006DCC] hover:underline cursor-pointer ml-1"
        >
          Xóa bộ lọc
        </button>
      )}
    </div>
  );
}
