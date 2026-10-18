"use client";

import React, { useEffect, useState } from "react";
import { connectionService } from "@/services/connections";
import type { LearningGoalData } from "@/services/profile";
import { formatMatchingSlots } from "@/shared/lib/dateTime";

export interface ConnectionRequestDialogProps {
  tutorId: string;
  goal: LearningGoalData;
  goals?: LearningGoalData[];
  onSelectGoal?: (goal: LearningGoalData) => void;
  onClose: () => void;
  onSent: () => void;
}

export function ConnectionRequestDialog({
  tutorId,
  goal: initialGoal,
  goals,
  onSelectGoal,
  onClose,
  onSent,
}: ConnectionRequestDialogProps) {
  const [selectedGoal, setSelectedGoal] = useState<LearningGoalData>(initialGoal);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setSelectedGoal(initialGoal);
  }, [initialGoal]);

  const send = async (event: React.FormEvent) => {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await connectionService.send({
        goalId: selectedGoal.goalId,
        tutorId,
        message: message.trim() || undefined,
        desiredSlots: selectedGoal.availabilitySlots.map((slot) => formatMatchingSlots([slot])),
      });
      onSent();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể gửi yêu cầu.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4">
      <form
        onSubmit={send}
        role="dialog"
        aria-modal="true"
        aria-labelledby="connection-title"
        className="w-full max-w-lg space-y-4 rounded-3xl bg-white p-6 shadow-2xl"
      >
        <h3 id="connection-title" className="text-lg font-bold text-neutral-900">
          Gửi yêu cầu kết nối
        </h3>

        {goals && goals.length > 1 && (
          <div className="space-y-1">
            <label htmlFor="goal-picker" className="block text-xs font-semibold text-neutral-700">
              Chọn mục tiêu học tập ghép lớp
            </label>
            <select
              id="goal-picker"
              value={selectedGoal.goalId}
              onChange={(e) => {
                const found = goals.find((g) => g.goalId === e.target.value);
                if (found) {
                  setSelectedGoal(found);
                  onSelectGoal?.(found);
                }
              }}
              className="w-full rounded-xl border border-stone-200 bg-stone-50 p-2 text-xs font-medium focus:border-blue-600 focus:outline-hidden"
            >
              {goals.map((g) => (
                <option key={g.goalId} value={g.goalId}>
                  {g.subjectName} · {g.educationLevelName}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="rounded-2xl border border-stone-100 bg-stone-50/80 p-3.5 space-y-1">
          <p className="font-semibold text-neutral-900 text-sm">
            {selectedGoal.subjectName} · {selectedGoal.educationLevelName}
          </p>
          <p className="text-xs text-neutral-500">
            Khung giờ đề xuất: {formatMatchingSlots(selectedGoal.availabilitySlots) || "Chưa có khung giờ"}
          </p>
        </div>

        <label className="block text-xs font-semibold text-neutral-700">
          Lời nhắn (không bắt buộc)
          <textarea
            className="mt-1 w-full rounded-xl border border-stone-200 p-2.5 text-xs focus:border-blue-600 focus:outline-hidden"
            maxLength={2000}
            rows={3}
            placeholder="Ví dụ: Mong muốn học thử buổi đầu tiên, cải thiện kiến thức hình học..."
            value={message}
            onChange={(event) => setMessage(event.target.value)}
          />
        </label>
        {error && (
          <p role="alert" className="text-xs text-rose-700 font-medium">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            disabled={busy}
            onClick={onClose}
            className="rounded-full border border-stone-200 px-4 py-2 text-xs font-semibold text-neutral-700 hover:bg-stone-50 cursor-pointer"
          >
            Đóng
          </button>
          <button
            disabled={busy || !selectedGoal.availabilitySlots.length}
            className="rounded-full bg-blue-700 px-5 py-2 text-xs font-bold text-white hover:bg-blue-800 disabled:opacity-50 cursor-pointer"
          >
            {busy ? "Đang gửi…" : "Xác nhận gửi yêu cầu"}
          </button>
        </div>
      </form>
    </div>
  );
}
