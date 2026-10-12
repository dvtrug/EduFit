"use client";

import React from "react";
import type { SubjectItem, EducationLevel } from "@/services/catalog";
import type { TutorSearchParams } from "../../types";

export interface AdvancedFilterDrawerProps {
  showAdvanced: boolean;
  params: TutorSearchParams;
  subjects: SubjectItem[];
  levels: EducationLevel[];
  onUpdateFilters: (filters: Partial<TutorSearchParams>) => void;
  onClose: () => void;
}

export function AdvancedFilterDrawer({
  showAdvanced,
  params,
  subjects,
  levels,
  onUpdateFilters,
  onClose,
}: AdvancedFilterDrawerProps) {
  if (!showAdvanced) return null;

  return (
    <div className="p-5 rounded-2xl bg-stone-50 dark:bg-stone-900 border border-stone-200 dark:border-stone-800 space-y-4 animate-in fade-in duration-150">
      <div className="flex items-center justify-between pb-2 border-b border-stone-200/70 dark:border-stone-800">
        <h5 className="text-xs font-bold text-neutral-900 dark:text-neutral-100 uppercase tracking-wider">
          Bộ lọc nâng cao đầy đủ
        </h5>
        <button
          type="button"
          onClick={onClose}
          className="text-xs text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 cursor-pointer"
        >
          Đóng
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Tất cả môn học từ Catalogs */}
        <div>
          <label className="text-xs font-semibold text-neutral-700 dark:text-neutral-300 block mb-1.5">
            Môn học đào tạo
          </label>
          <select
            value={params.subjectId || ""}
            onChange={(e) =>
              onUpdateFilters({
                subjectId: e.target.value ? Number(e.target.value) : undefined,
              })
            }
            className="w-full px-3 py-2 bg-card border border-stone-200 dark:border-stone-800 rounded-xl text-xs text-card-foreground focus:outline-hidden focus:border-sky-500 font-medium"
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
          <label className="text-xs font-semibold text-neutral-700 dark:text-neutral-300 block mb-1.5">
            Cấp lớp giảng dạy
          </label>
          <select
            value={params.levelId || ""}
            onChange={(e) =>
              onUpdateFilters({
                levelId: e.target.value ? Number(e.target.value) : undefined,
              })
            }
            className="w-full px-3 py-2 bg-card border border-stone-200 dark:border-stone-800 rounded-xl text-xs text-card-foreground focus:outline-hidden focus:border-sky-500 font-medium"
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
          <label className="text-xs font-semibold text-neutral-700 dark:text-neutral-300 block mb-1.5">
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
              className="w-full px-2.5 py-2 bg-card border border-stone-200 dark:border-stone-800 rounded-xl text-xs text-card-foreground"
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
              className="w-full px-2.5 py-2 bg-card border border-stone-200 dark:border-stone-800 rounded-xl text-xs text-card-foreground"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
