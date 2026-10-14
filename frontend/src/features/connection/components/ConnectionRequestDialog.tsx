"use client";

import React, { useState } from "react";
import { connectionService } from "@/services/connections";
import type { LearningGoalData } from "@/services/profile";
import { formatMatchingSlots } from "@/shared/lib/dateTime";

export interface ConnectionRequestDialogProps {
  tutorId: string;
  goal: LearningGoalData;
  onClose: () => void;
  onSent: () => void;
}

export function ConnectionRequestDialog({
  tutorId,
  goal,
  onClose,
  onSent,
}: ConnectionRequestDialogProps) {
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = async (event: React.FormEvent) => {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await connectionService.send({
        goalId: goal.goalId,
        tutorId,
        message: message.trim() || undefined,
        desiredSlots: goal.availabilitySlots.map((slot) => formatMatchingSlots([slot])),
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
        className="w-full max-w-lg space-y-4 rounded-3xl bg-white p-6"
      >
        <h3 id="connection-title" className="text-lg font-bold">
          Gửi yêu cầu kết nối
        </h3>
        <p>
          {goal.subjectName} · {goal.educationLevelName}
        </p>
        <p className="text-sm">
          Khung giờ đề xuất: {formatMatchingSlots(goal.availabilitySlots)}
        </p>
        <label className="block text-sm">
          Lời nhắn (không bắt buộc)
          <textarea
            className="mt-1 w-full rounded-lg border p-2"
            maxLength={2000}
            value={message}
            onChange={(event) => setMessage(event.target.value)}
          />
        </label>
        {error && (
          <p role="alert" className="text-sm text-rose-700">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-3">
          <button
            type="button"
            disabled={busy}
            onClick={onClose}
            className="rounded-full border px-4 py-2"
          >
            Đóng
          </button>
          <button
            disabled={busy || !goal.availabilitySlots.length}
            className="rounded-full bg-blue-700 px-4 py-2 text-white disabled:opacity-50"
          >
            {busy ? "Đang gửi…" : "Xác nhận gửi yêu cầu"}
          </button>
        </div>
      </form>
    </div>
  );
}
