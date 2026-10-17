"use client";

import React, { useState, useEffect } from "react";
import { AlertTriangle, X, Loader2, StopCircle } from "lucide-react";
import { connectionApi } from "../api/connectionApi";
import type { TutoringClass } from "../types";

export interface EndClassModalProps {
  classItem: TutoringClass | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function EndClassModal({
  classItem,
  isOpen,
  onClose,
  onSuccess,
}: EndClassModalProps) {
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setReason("");
      setError(null);
      setLoading(false);
    }
  }, [isOpen]);

  if (!isOpen || !classItem) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (reason.trim().length > 300) {
      setError("Lý do kết thúc lớp học không được vượt quá 300 ký tự.");
      return;
    }

    try {
      setLoading(true);
      await connectionApi.endClass(classItem.classId, reason.trim());
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Không thể kết thúc lớp học. Vui lòng thử lại sau."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="end-class-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-xs animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-md overflow-hidden rounded-3xl bg-white shadow-2xl border border-stone-200 dark:bg-zinc-900 dark:border-zinc-800">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-100 dark:border-zinc-800 px-6 py-4">
          <div className="flex items-center gap-2.5">
            <div className="grid size-10 place-items-center rounded-xl bg-rose-100 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400">
              <StopCircle className="size-5" />
            </div>
            <div>
              <h2 id="end-class-modal-title" className="text-base font-bold text-neutral-900 dark:text-zinc-100">
                Kết thúc lớp học
              </h2>
              <p className="text-xs text-neutral-500 dark:text-zinc-400">
                {classItem.subjectName || "Lớp học"} • {classItem.studentDisplayName ? `Học sinh: ${classItem.studentDisplayName}` : ""}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="grid size-8 place-items-center rounded-full text-neutral-400 hover:bg-stone-100 hover:text-neutral-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-200 transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="size-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Cảnh báo cascade cancellation */}
          <div
            role="alert"
            className="flex items-start gap-3 rounded-2xl bg-amber-50 p-4 border border-amber-200 text-amber-900 text-xs dark:bg-amber-950/30 dark:border-amber-900/50 dark:text-amber-300"
          >
            <AlertTriangle className="size-4 shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
            <div>
              <p className="font-bold">Lưu ý quan trọng khi kết thúc lớp:</p>
              <p className="mt-1 leading-relaxed">
                Tất cả các buổi học trong tương lai (ở trạng thái Đã xếp lịch hoặc Đang đề xuất) của lớp học này sẽ <strong>tự động bị hủy</strong> để giải phóng lịch dạy cho cả hai bên.
              </p>
            </div>
          </div>

          {error && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800 dark:border-rose-900/50 dark:bg-rose-950/30 dark:text-rose-300">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="end-class-reason" className="block text-xs font-bold text-neutral-700 dark:text-zinc-300 mb-1">
              Lý do kết thúc lớp học (Tùy chọn)
            </label>
            <textarea
              id="end-class-reason"
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Ví dụ: Đã hoàn thành chương trình học / Cả hai bên thống nhất kết thúc..."
              maxLength={300}
              disabled={loading}
              className="w-full rounded-xl border border-stone-200 bg-white p-3 text-xs text-neutral-800 focus:border-rose-500 focus:outline-hidden dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-100"
            />
            <div className="flex justify-end mt-1 text-[11px] text-neutral-400 dark:text-zinc-500">
              <span>{reason.length}/300</span>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-2 pt-3 border-t border-stone-100 dark:border-zinc-800">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="h-9 px-4 rounded-xl border border-stone-200 bg-white text-xs font-bold text-neutral-700 hover:bg-stone-50 disabled:opacity-50 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading}
              className="h-9 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-xs flex items-center gap-1.5 disabled:opacity-50 cursor-pointer"
            >
              {loading ? (
                <>
                  <Loader2 className="size-3.5 animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                "Xác nhận kết thúc lớp"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
