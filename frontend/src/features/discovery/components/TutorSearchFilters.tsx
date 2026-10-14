"use client";

import React, { useEffect, useState } from "react";
import { catalogService, SubjectItem, EducationLevel } from "@/services/catalog";
import type { TutorSearchParams } from "../types";
import { FilterHeader } from "./filters/FilterHeader";
import { QuickFilterPills } from "./filters/QuickFilterPills";
import { AdvancedFilterDrawer } from "./filters/AdvancedFilterDrawer";

export interface TutorSearchFiltersProps {
  params: TutorSearchParams;
  onUpdateFilters: (filters: Partial<TutorSearchParams>) => void;
  onResetFilters: () => void;
  totalResults?: number;
}

export function TutorSearchFilters({
  params,
  onUpdateFilters,
  onResetFilters,
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

  const selectedSubject = subjects.find((s) => s.subjectId === params.subjectId);
  const selectedLevel = levels.find((l) => l.levelId === params.levelId);

  return (
    <div className="space-y-3.5">
      <FilterHeader
        keywordInput={keywordInput}
        onKeywordChange={setKeywordInput}
        onClearKeyword={() => setKeywordInput("")}
        currentSortVal={currentSortVal}
        onSortChange={handleSortChange}
      />

      <QuickFilterPills
        params={params}
        subjects={subjects}
        levels={levels}
        selectedSubject={selectedSubject}
        selectedLevel={selectedLevel}
        showAdvanced={showAdvanced}
        hasActiveFilters={hasActiveFilters}
        onUpdateFilters={onUpdateFilters}
        onResetFilters={onResetFilters}
        onToggleAdvanced={() => setShowAdvanced(!showAdvanced)}
        onClearKeywordInput={() => setKeywordInput("")}
      />

      <AdvancedFilterDrawer
        showAdvanced={showAdvanced}
        params={params}
        subjects={subjects}
        levels={levels}
        onUpdateFilters={onUpdateFilters}
        onClose={() => setShowAdvanced(false)}
      />
    </div>
  );
}
