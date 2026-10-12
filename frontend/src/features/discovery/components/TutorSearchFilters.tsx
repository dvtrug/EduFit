"use client";

import React, { useEffect, useState } from "react";
import {
  Search,
  SlidersHorizontal,
  ChevronDown,
  RotateCcw,
  Check,
  X,
} from "lucide-react";
import { catalogService, SubjectItem, EducationLevel } from "@/services/catalog";
import type { TutorSearchParams, TeachingMode } from "../types";

export interface TutorSearchFiltersProps {
  params: TutorSearchParams;
  onUpdateFilters: (filters: Partial<TutorSearchParams>) => void;
  onResetFilters: () => void;
  totalResults?: number;
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

const COMMON_AREAS = [
  "Quận 1",
  "Quận 3",
  "Quận 5",
  "Quận 7",
  "Quận 10",
  "Bình Thạnh",
  "Tân Bình",
  "Thủ Đức",
];

export function TutorSearchFilters({
  params,
  onUpdateFilters,
  onResetFilters,
  totalResults,
}: TutorSearchFiltersProps) {
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [levels, setLevels] = useState<EducationLevel[]>([]);
  const [keywordInput, setKeywordInput] = useState(params.keyword || "");
  const [showAdvanced, setShowAdvanced] = useState(false);

  useEffect(() => {
    async function loadCatalogs() {
      try {
        const [subList, lvlList] = await Promise.all([
          catalogService.getSubjects(),
          catalogService.getEducationLevels(),
        ]);
        setSubjects(subList);
        setLevels(lvlList);
      } catch (err) {
        console.error("Lỗi khi tải danh mục tìm kiếm:", err);
      }
    }
    loadCatalogs();
  }, []);

  // Debounced keyword search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (keywordInput !== (params.keyword || "")) {
        onUpdateFilters({ keyword: keywordInput || undefined });
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [keywordInput, params.keyword, onUpdateFilters]);

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

  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    if (val === "relevance") onUpdateFilters({ sortBy: "relevance", sortDirection: "desc" });
    else if (val === "rating") onUpdateFilters({ sortBy: "rating", sortDirection: "desc" });
    else if (val === "price_asc") onUpdateFilters({ sortBy: "price", sortDirection: "asc" });
    else if (val === "price_desc") onUpdateFilters({ sortBy: "price", sortDirection: "desc" });
    else if (val === "experience") onUpdateFilters({ sortBy: "experience", sortDirection: "desc" });
  };

  const currentSortVal =
    params.sortBy === "price"
      ? params.sortDirection === "asc"
        ? "price_asc"
        : "price_desc"
      : params.sortBy === "experience"
      ? "experience"
      : params.sortBy === "relevance"
      ? "relevance"
      : "rating";

  const hasActiveFilters = Boolean(
    params.keyword ||
    params.subjectId ||
    params.levelId ||
    params.mode ||
    params.minPrice ||
    params.maxPrice ||
    params.minRating ||
    params.dayOfWeek ||
    params.area
  );

  // Tìm tên môn học và lớp đang chọn để hiển thị pill
  const selectedSubject = subjects.find((s) => s.subjectId === params.subjectId);
  const selectedLevel = levels.find((l) => l.levelId === params.levelId);

  return (
    <div className="space-y-3.5">
      {/* 1. HÀNG 1: SEARCH INPUT & SORT DROPDOWN (Figma 36:3538) */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        {/* Search input field */}
        <div className="relative flex-1">
          <Search className="size-4 text-neutral-400 absolute left-4 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            placeholder="Tìm theo tên, môn học hoặc khu vực..."
            className="w-full pl-11 pr-4 py-2.5 bg-white border border-stone-200/90 rounded-2xl text-xs text-neutral-900 placeholder:text-neutral-400 focus:bg-white focus:outline-hidden focus:border-[#006DCC] focus:ring-2 focus:ring-sky-500/20 transition-all font-medium shadow-2xs"
          />
          {keywordInput && (
            <button
              type="button"
              onClick={() => setKeywordInput("")}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
            >
              <X className="size-3.5" />
            </button>
          )}
        </div>

        {/* Sort Select */}
        <div className="relative shrink-0">
          <select
            value={currentSortVal}
            onChange={handleSortChange}
            aria-label="Sắp xếp kết quả tìm kiếm"
            className="w-full sm:w-auto appearance-none pl-4 pr-9 py-2.5 bg-white border border-stone-200/90 rounded-2xl text-xs font-semibold text-neutral-800 focus:outline-hidden focus:border-[#006DCC] shadow-2xs transition-colors cursor-pointer"
          >
            <option value="relevance">Liên quan nhất</option>
            <option value="rating">Đánh giá cao nhất</option>
            <option value="price_asc">Học phí: Thấp đến cao</option>
            <option value="price_desc">Học phí: Cao đến thấp</option>
            <option value="experience">Nhiều kinh nghiệm nhất</option>
          </select>
          <ChevronDown className="size-3.5 text-neutral-400 absolute right-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
        </div>
      </div>

      {/* 2. HÀNG 2: FILTER PILLS (Figma Quick Filter Pills) */}
      <div className="flex flex-wrap items-center gap-2 pt-0.5">
        {/* Pill 1: Toán (hoặc môn đang chọn) */}
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
            className="px-3.5 py-1.5 rounded-full text-xs font-medium bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50 transition-colors cursor-pointer shadow-2xs"
          >
            Toán
          </button>
        )}

        {/* Pill 2: Lớp 9 (hoặc cấp lớp đang chọn) */}
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
            className="px-3.5 py-1.5 rounded-full text-xs font-medium bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50 transition-colors cursor-pointer shadow-2xs"
          >
            Lớp 9
          </button>
        )}

        {/* Pill 3: Hình thức Online */}
        <button
          type="button"
          onClick={() => handleModeChange("ONLINE")}
          className={`px-3.5 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer shadow-2xs ${
            params.mode === "ONLINE"
              ? "bg-[#006DCC] text-white"
              : "bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50"
          }`}
        >
          Online {params.mode === "ONLINE" && "✓"}
        </button>

        {/* Pill 4: Khu vực dropdown */}
        <div className="relative inline-block">
          <select
            value={params.area || ""}
            onChange={(e) => handleAreaSelect(e.target.value)}
            aria-label="Lọc theo khu vực"
            className={`appearance-none pl-3 pr-7 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer shadow-2xs ${
              params.area
                ? "bg-[#006DCC] text-white border-transparent"
                : "bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50"
            }`}
          >
            <option value="" className="text-neutral-900 bg-white">Khu vực</option>
            {COMMON_AREAS.map((a) => (
              <option key={a} value={a} className="text-neutral-900 bg-white">
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

        {/* Pill 5: Lịch rảnh dropdown */}
        <div className="relative inline-block">
          <select
            value={params.dayOfWeek || ""}
            onChange={(e) =>
              onUpdateFilters({
                dayOfWeek: e.target.value ? Number(e.target.value) : undefined,
              })
            }
            aria-label="Lọc theo lịch rảnh trong tuần"
            className={`appearance-none pl-3 pr-7 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer shadow-2xs ${
              params.dayOfWeek
                ? "bg-[#006DCC] text-white border-transparent"
                : "bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50"
            }`}
          >
            <option value="" className="text-neutral-900 bg-white">Lịch rảnh</option>
            {DAY_OPTIONS.map((d) => (
              <option key={d.value} value={d.value} className="text-neutral-900 bg-white">
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

        {/* Pill 6: Rating 4.5+ */}
        <button
          type="button"
          onClick={() => handleRatingToggle(4.5)}
          className={`px-3.5 py-1.5 rounded-full text-xs font-medium transition-all cursor-pointer shadow-2xs ${
            params.minRating === 4.5
              ? "bg-[#006DCC] text-white"
              : "bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50"
          }`}
        >
          Rating 4.5+ {params.minRating === 4.5 && "★"}
        </button>

        {/* Nút mở toàn bộ bộ lọc nâng cao (đảm bảo giữ 100% chức năng bộ lọc hiện tại) */}
        <button
          type="button"
          onClick={() => setShowAdvanced(!showAdvanced)}
          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors cursor-pointer shadow-2xs ${
            showAdvanced
              ? "bg-sky-50 border-sky-300 text-[#006DCC]"
              : "bg-white border-stone-200 text-neutral-700 hover:bg-stone-50"
          }`}
        >
          <SlidersHorizontal className="size-3" />
          <span>Bộ lọc chi tiết</span>
        </button>

        {/* Link text: Xóa bộ lọc */}
        {hasActiveFilters && (
          <button
            type="button"
            onClick={() => {
              setKeywordInput("");
              onResetFilters();
            }}
            className="text-xs font-medium text-[#006DCC] hover:underline cursor-pointer ml-1"
          >
            Xóa bộ lọc
          </button>
        )}
      </div>

      {/* 3. BẢNG BỘ LỌC CHI TIẾT (EXPANDABLE DRAWER - GIỮ NGUYÊN 100% TẤT CẢ TÍNH NĂNG ĐÃ CÓ) */}
      {showAdvanced && (
        <div className="p-5 rounded-2xl bg-stone-50 border border-stone-200 space-y-4 animate-in fade-in duration-150">
          <div className="flex items-center justify-between pb-2 border-b border-stone-200/70">
            <h5 className="text-xs font-bold text-neutral-900 uppercase tracking-wider">
              Bộ lọc nâng cao đầy đủ
            </h5>
            <button
              type="button"
              onClick={() => setShowAdvanced(false)}
              className="text-xs text-neutral-400 hover:text-neutral-700"
            >
              Đóng
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {/* Tất cả môn học từ Catalogs */}
            <div>
              <label className="text-xs font-semibold text-neutral-700 block mb-1.5">
                Môn học đào tạo
              </label>
              <select
                value={params.subjectId || ""}
                onChange={(e) =>
                  onUpdateFilters({
                    subjectId: e.target.value ? Number(e.target.value) : undefined,
                  })
                }
                className="w-full px-3 py-2 bg-white border border-stone-200 rounded-xl text-xs text-neutral-800 focus:outline-hidden focus:border-sky-500 font-medium"
              >
                <option value="">Tất cả môn học</option>
                {subjects.map((sub) => (
                  <option key={sub.subjectId} value={sub.subjectId}>
                    {sub.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Tất cả cấp lớp từ Catalogs */}
            <div>
              <label className="text-xs font-semibold text-neutral-700 block mb-1.5">
                Cấp lớp giảng dạy
              </label>
              <select
                value={params.levelId || ""}
                onChange={(e) =>
                  onUpdateFilters({
                    levelId: e.target.value ? Number(e.target.value) : undefined,
                  })
                }
                className="w-full px-3 py-2 bg-white border border-stone-200 rounded-xl text-xs text-neutral-800 focus:outline-hidden focus:border-sky-500 font-medium"
              >
                <option value="">Tất cả khối lớp</option>
                {levels.map((lvl) => (
                  <option key={lvl.levelId} value={lvl.levelId}>
                    {lvl.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Khoảng giá học phí */}
            <div>
              <label className="text-xs font-semibold text-neutral-700 block mb-1.5">
                Khoảng học phí (VNĐ / buổi)
              </label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  placeholder="Từ (VNĐ)"
                  value={params.minPrice || ""}
                  onChange={(e) =>
                    onUpdateFilters({
                      minPrice: e.target.value ? Number(e.target.value) : undefined,
                    })
                  }
                  className="w-full px-2.5 py-2 bg-white border border-stone-200 rounded-xl text-xs text-neutral-800"
                />
                <input
                  type="number"
                  placeholder="Đến (VNĐ)"
                  value={params.maxPrice || ""}
                  onChange={(e) =>
                    onUpdateFilters({
                      maxPrice: e.target.value ? Number(e.target.value) : undefined,
                    })
                  }
                  className="w-full px-2.5 py-2 bg-white border border-stone-200 rounded-xl text-xs text-neutral-800"
                />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
