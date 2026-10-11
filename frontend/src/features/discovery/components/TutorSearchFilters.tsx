"use client";

import React, { useEffect, useState } from "react";
import { Search, Filter, RotateCcw, SlidersHorizontal, ChevronDown, Check } from "lucide-react";
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
    }, 400);
    return () => clearTimeout(timer);
  }, [keywordInput, params.keyword, onUpdateFilters]);

  const handleModeChange = (mode?: TeachingMode) => {
    onUpdateFilters({ mode });
  };

  const handleSubjectChange = (subjectId?: number) => {
    onUpdateFilters({ subjectId: subjectId || undefined });
  };

  const handleLevelChange = (levelId?: number) => {
    onUpdateFilters({ levelId: levelId || undefined });
  };

  const handleSortChange = (sortBy: "rating" | "price" | "experience", sortDirection: "asc" | "desc") => {
    onUpdateFilters({ sortBy, sortDirection });
  };

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

  return (
    <div className="bg-white rounded-3xl border border-stone-200/90 shadow-2xs p-5 sm:p-6 space-y-4">
      {/* 1. Thanh tìm kiếm chính & Nút lọc nâng cao */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            placeholder="Tìm theo tên gia sư, môn học, kỹ năng hoặc trường đào tạo..."
            className="w-full pl-10 pr-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-xs text-neutral-900 placeholder:text-neutral-400 focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all font-medium"
          />
        </div>

        <div className="flex items-center gap-2">
          {/* Nút bật/tắt Lọc nâng cao */}
          <button
            type="button"
            onClick={() => setShowAdvanced(!showAdvanced)}
            className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold border transition-colors cursor-pointer shrink-0 ${
              showAdvanced || hasActiveFilters
                ? "bg-amber-50 border-amber-300 text-amber-900"
                : "bg-white border-stone-200 text-neutral-700 hover:bg-stone-50"
            }`}
          >
            <SlidersHorizontal className="size-3.5" />
            <span>Bộ lọc {hasActiveFilters && "(Đang áp dụng)"}</span>
          </button>

          {/* Nút reset */}
          {hasActiveFilters && (
            <button
              type="button"
              onClick={() => {
                setKeywordInput("");
                onResetFilters();
              }}
              className="p-2.5 rounded-xl border border-stone-200 text-neutral-500 hover:text-neutral-800 hover:bg-stone-50 transition-colors cursor-pointer"
              title="Đặt lại bộ lọc"
            >
              <RotateCcw className="size-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* 2. Bộ lọc nhanh: Môn học, Cấp lớp, Hình thức dạy */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
        {/* Chọn Môn học */}
        <div>
          <label className="block text-[11px] font-bold text-neutral-600 mb-1">Môn học</label>
          <select
            value={params.subjectId || ""}
            onChange={(e) => handleSubjectChange(e.target.value ? Number(e.target.value) : undefined)}
            className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-xs text-neutral-900 font-medium focus:bg-white focus:outline-hidden focus:border-amber-500 transition-all"
          >
            <option value="">-- Tất cả môn học --</option>
            {subjects.map((sub) => (
              <option key={sub.subjectId} value={sub.subjectId}>
                {sub.name}
              </option>
            ))}
          </select>
        </div>

        {/* Chọn Cấp lớp */}
        <div>
          <label className="block text-[11px] font-bold text-neutral-600 mb-1">Khối lớp</label>
          <select
            value={params.levelId || ""}
            onChange={(e) => handleLevelChange(e.target.value ? Number(e.target.value) : undefined)}
            className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-xs text-neutral-900 font-medium focus:bg-white focus:outline-hidden focus:border-amber-500 transition-all"
          >
            <option value="">-- Tất cả khối lớp --</option>
            {levels.map((lvl) => (
              <option key={lvl.levelId} value={lvl.levelId}>
                {lvl.name}
              </option>
            ))}
          </select>
        </div>

        {/* Hình thức dạy */}
        <div>
          <label className="block text-[11px] font-bold text-neutral-600 mb-1">Hình thức dạy</label>
          <div className="flex rounded-xl bg-stone-100 p-0.5 border border-stone-200">
            <button
              type="button"
              onClick={() => handleModeChange(undefined)}
              className={`flex-1 py-1.5 text-[11px] font-bold rounded-lg transition-all cursor-pointer ${
                !params.mode ? "bg-white text-neutral-900 shadow-2xs" : "text-neutral-500 hover:text-neutral-800"
              }`}
            >
              Tất cả
            </button>
            <button
              type="button"
              onClick={() => handleModeChange("ONLINE")}
              className={`flex-1 py-1.5 text-[11px] font-bold rounded-lg transition-all cursor-pointer ${
                params.mode === "ONLINE" ? "bg-white text-sky-700 shadow-2xs" : "text-neutral-500 hover:text-neutral-800"
              }`}
            >
              Online
            </button>
            <button
              type="button"
              onClick={() => handleModeChange("OFFLINE")}
              className={`flex-1 py-1.5 text-[11px] font-bold rounded-lg transition-all cursor-pointer ${
                params.mode === "OFFLINE" ? "bg-white text-emerald-700 shadow-2xs" : "text-neutral-500 hover:text-neutral-800"
              }`}
            >
              Offline
            </button>
          </div>
        </div>
      </div>

      {/* 3. Khung Bộ lọc nâng cao (Khoảng giá, Đánh giá, Lịch rảnh, Khu vực) */}
      {showAdvanced && (
        <div className="pt-4 border-t border-stone-100 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-in fade-in duration-150">
          {/* Khoảng học phí */}
          <div>
            <label className="block text-[11px] font-bold text-neutral-600 mb-1">
              Khoảng học phí (VNĐ/buổi)
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                placeholder="Từ"
                value={params.minPrice || ""}
                onChange={(e) => onUpdateFilters({ minPrice: e.target.value ? Number(e.target.value) : undefined })}
                className="w-full px-2.5 py-1.5 bg-stone-50 border border-stone-200 rounded-lg text-xs"
              />
              <span className="text-neutral-400">-</span>
              <input
                type="number"
                placeholder="Đến"
                value={params.maxPrice || ""}
                onChange={(e) => onUpdateFilters({ maxPrice: e.target.value ? Number(e.target.value) : undefined })}
                className="w-full px-2.5 py-1.5 bg-stone-50 border border-stone-200 rounded-lg text-xs"
              />
            </div>
          </div>

          {/* Đánh giá sao */}
          <div>
            <label className="block text-[11px] font-bold text-neutral-600 mb-1">Đánh giá tối thiểu</label>
            <select
              value={params.minRating || ""}
              onChange={(e) => onUpdateFilters({ minRating: e.target.value ? Number(e.target.value) : undefined })}
              className="w-full px-2.5 py-1.5 bg-stone-50 border border-stone-200 rounded-lg text-xs text-neutral-900 font-medium"
            >
              <option value="">-- Bất kỳ đánh giá --</option>
              <option value="4.5">⭐ 4.5 sao trở lên</option>
              <option value="4.0">⭐ 4.0 sao trở lên</option>
              <option value="3.5">⭐ 3.5 sao trở lên</option>
            </select>
          </div>

          {/* Lọc theo thứ trong tuần */}
          <div>
            <label className="block text-[11px] font-bold text-neutral-600 mb-1">Thứ rảnh trong tuần</label>
            <select
              value={params.dayOfWeek || ""}
              onChange={(e) => onUpdateFilters({ dayOfWeek: e.target.value ? Number(e.target.value) : undefined })}
              className="w-full px-2.5 py-1.5 bg-stone-50 border border-stone-200 rounded-lg text-xs text-neutral-900 font-medium"
            >
              <option value="">-- Mọi ngày --</option>
              {DAY_OPTIONS.map((d) => (
                <option key={d.value} value={d.value}>
                  {d.label}
                </option>
              ))}
            </select>
          </div>

          {/* Khu vực học kèm */}
          <div>
            <label className="block text-[11px] font-bold text-neutral-600 mb-1">Khu vực sinh sống</label>
            <input
              type="text"
              placeholder="VD: Cầu Giấy, Hà Nội..."
              value={params.area || ""}
              onChange={(e) => onUpdateFilters({ area: e.target.value || undefined })}
              className="w-full px-2.5 py-1.5 bg-stone-50 border border-stone-200 rounded-lg text-xs text-neutral-900"
            />
          </div>
        </div>
      )}

      {/* 4. Thanh Sort & Số lượng kết quả */}
      <div className="pt-2 border-t border-stone-100 flex flex-wrap items-center justify-between gap-3 text-xs">
        <span className="text-neutral-500 font-medium">
          {totalResults !== undefined ? (
            <>
              Tìm thấy <strong className="text-neutral-900">{totalResults}</strong> gia sư phù hợp
            </>
          ) : (
            "Đang tìm kiếm gia sư..."
          )}
        </span>

        <div className="flex items-center gap-2">
          <span className="text-neutral-400 text-[11px]">Sắp xếp theo:</span>
          <select
            value={`${params.sortBy || "rating"}-${params.sortDirection || "desc"}`}
            onChange={(e) => {
              const [sb, sd] = e.target.value.split("-") as [any, any];
              handleSortChange(sb, sd);
            }}
            className="px-2.5 py-1 bg-stone-50 border border-stone-200 rounded-lg text-[11px] font-bold text-neutral-800"
          >
            <option value="rating-desc">Đánh giá cao nhất</option>
            <option value="price-asc">Học phí: Thấp đến cao</option>
            <option value="price-desc">Học phí: Cao đến thấp</option>
            <option value="experience-desc">Kinh nghiệm nhiều nhất</option>
          </select>
        </div>
      </div>
    </div>
  );
}
