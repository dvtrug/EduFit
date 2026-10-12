"use client";

import { useState, useCallback } from "react";
import { discoveryApi } from "../api/discoveryApi";
import type { TutorDetail } from "../types";

export function useTutorDetail() {
  const [selectedTutorId, setSelectedTutorId] = useState<string | null>(null);
  const [tutor, setTutor] = useState<TutorDetail | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const openTutor = useCallback(async (tutorId: string) => {
    setSelectedTutorId(tutorId);
    setIsLoading(true);
    setError(null);

    try {
      const res = await discoveryApi.getTutorDetail(tutorId);
      if (res.success && res.data) {
        setTutor(res.data);
      } else {
        setError(res.message || "Không thể tải chi tiết hồ sơ gia sư");
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Lỗi kết nối khi tải hồ sơ gia sư";
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const closeTutor = useCallback(() => {
    setSelectedTutorId(null);
    setTutor(null);
    setError(null);
  }, []);

  return {
    isOpen: Boolean(selectedTutorId),
    selectedTutorId,
    tutor,
    isLoading,
    error,
    openTutor,
    closeTutor,
  };
}
