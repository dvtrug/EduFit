"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import { discoveryApi } from "../api/discoveryApi";
import type { TutorDetail } from "../types";

export function useTutorDetail() {
  const [selectedTutorId, setSelectedTutorId] = useState<string | null>(null);
  const [tutor, setTutor] = useState<TutorDetail | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const pending = useRef<AbortController | null>(null);
  useEffect(() => () => pending.current?.abort(), []);

  const openTutor = useCallback(async (tutorId: string) => {
    pending.current?.abort();
    const controller = new AbortController();
    pending.current = controller;
    setSelectedTutorId(tutorId);
    setTutor(null);
    setIsLoading(true);
    setError(null);

    try {
      const res = await discoveryApi.getTutorDetail(tutorId, controller.signal);
      if (controller.signal.aborted) return;
      if (res.success && res.data) {
        setTutor(res.data);
      } else {
        setError(res.message || "Không thể tải chi tiết hồ sơ gia sư");
      }
    } catch (err: unknown) {
      if (controller.signal.aborted) return;
      const msg = err instanceof Error ? err.message : "Lỗi kết nối khi tải hồ sơ gia sư";
      setError(msg);
    } finally {
      if (!controller.signal.aborted) setIsLoading(false);
    }
  }, []);

  const closeTutor = useCallback(() => {
    pending.current?.abort();
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
