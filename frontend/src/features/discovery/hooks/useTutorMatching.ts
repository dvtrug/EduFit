"use client";

import { useState, useCallback } from "react";
import { discoveryApi } from "../api/discoveryApi";
import type { TutorMatchResult } from "../types";

export interface UseTutorMatchingOptions {
  defaultGoalId?: string;
  defaultTopN?: number;
}

export function useTutorMatching(options: UseTutorMatchingOptions = {}) {
  const { defaultGoalId, defaultTopN = 5 } = options;

  const [matches, setMatches] = useState<TutorMatchResult[]>([]);
  const [isMatching, setIsMatching] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const runMatching = useCallback(
    async (goalId?: string, topN?: number) => {
      const targetGoalId = goalId || defaultGoalId;
      if (!targetGoalId) {
        setError("Vui lòng chọn hoặc thiết lập mục tiêu học tập trước khi chạy AI Match");
        return;
      }

      setIsMatching(true);
      setError(null);

      try {
        const res = await discoveryApi.matchTutors({
          goalId: targetGoalId,
          topN: topN ?? defaultTopN,
        });

        if (res.success && res.data) {
          setMatches(res.data);
        } else {
          setError(res.message || "Không thể thực hiện ghép cặp gia sư");
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "Có lỗi xảy ra khi thực hiện ghép cặp AI";
        setError(msg);
      } finally {
        setIsMatching(false);
      }
    },
    [defaultGoalId, defaultTopN]
  );

  const clearMatches = useCallback(() => {
    setMatches([]);
    setError(null);
  }, []);

  return {
    matches,
    isMatching,
    error,
    runMatching,
    clearMatches,
  };
}
