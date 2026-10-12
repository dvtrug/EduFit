"use client";

import React, { useState, useEffect } from "react";
import { X, Target, Sparkles, Check, BookOpen, GraduationCap, Clock, MapPin } from "lucide-react";
import type { MatchingNeed } from "../types";

export interface MatchingNeedModalProps {
  isOpen: boolean;
  currentNeed: MatchingNeed;
  onClose: () => void;
  onSave: (newNeed: MatchingNeed) => void;
}

const COMMON_SUBJECTS = ["Toán", "Tiếng Anh", "Ngữ văn", "Vật lý", "Hóa học", "Sinh học", "Tin học"];
const COMMON_GRADES = ["Lớp 6", "Lớp 7", "Lớp 8", "Lớp 9", "Lớp 10", "Lớp 11", "Lớp 12"];
const COMMON_TARGETS = [
  "Ôn thi vào lớp 10",
  "Ôn thi tốt nghiệp & ĐH",
  "Lấy lại kiến thức căn bản",
  "Nâng cao điểm số trên lớp (8.0+)",
  "Bồi dưỡng học sinh giỏi / Thi chuyên",
];
const COMMON_SLOTS = [
  "Tối T3, T5, T7 (18:00 - 20:00)",
  "Tối T2, T4, T6 (19:00 - 21:00)",
  "Sáng & Chiều Thứ 7, CN",
  "Linh hoạt theo thỏa thuận",
];

export function MatchingNeedModal({
  isOpen,
  currentNeed,
  onClose,
  onSave,
}: MatchingNeedModalProps) {
  const [subject, setSubject] = useState(currentNeed.subjectName);
  const [grade, setGrade] = useState(currentNeed.gradeName);
  const [target, setTarget] = useState(currentNeed.targetDescription);
  const [mode, setMode] = useState(currentNeed.teachingMode);
  const [area, setArea] = useState(currentNeed.area);
  const [slots, setSlots] = useState(currentNeed.timeSlotsDescription);

  useEffect(() => {
    if (isOpen) {
      setSubject(currentNeed.subjectName);
      setGrade(currentNeed.gradeName);
      setTarget(currentNeed.targetDescription);
      setMode(currentNeed.teachingMode);
      setArea(currentNeed.area);
      setSlots(currentNeed.timeSlotsDescription);
    }
  }, [isOpen, currentNeed]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      subjectName: subject,
      gradeName: grade,
      targetDescription: target,
      teachingMode: mode,
      area: area.trim() || "Toàn quốc",
      timeSlotsDescription: slots,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-950/50 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-lg w-full border border-stone-200 shadow-2xl overflow-hidden animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="px-6 py-5 border-b border-stone-100 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="size-9 rounded-2xl bg-sky-50 text-sky-600 flex items-center justify-center">
              <Target className="size-5 text-sky-600" />
            </div>
            <div>
              <h3 className="font-heading font-bold text-base text-neutral-900">
                Thay đổi nhu cầu Matching
              </h3>
              <p className="text-xs text-neutral-500">
                Hệ thống AI sẽ cập nhật trọng số và gợi ý lại danh sách gia sư tối ưu
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 text-neutral-400 hover:text-neutral-700 hover:bg-stone-100 rounded-full transition-colors"
          >
            <X className="size-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5 max-h-[75vh] overflow-y-auto">
          {/* 1. Môn học */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-neutral-700 flex items-center gap-1.5">
              <BookOpen className="size-3.5 text-neutral-400" />
              <span>Môn học cần tìm gia sư</span>
            </label>
            <div className="flex flex-wrap gap-1.5">
              {COMMON_SUBJECTS.map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setSubject(s)}
                  className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all cursor-pointer ${
                    subject === s
                      ? "bg-[#006DCC] text-white shadow-2xs"
                      : "bg-stone-100 text-neutral-700 hover:bg-stone-200"
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* 2. Khối lớp */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-neutral-700 flex items-center gap-1.5">
              <GraduationCap className="size-3.5 text-neutral-400" />
              <span>Khối lớp</span>
            </label>
            <div className="flex flex-wrap gap-1.5">
              {COMMON_GRADES.map((g) => (
                <button
                  key={g}
                  type="button"
                  onClick={() => setGrade(g)}
                  className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all cursor-pointer ${
                    grade === g
                      ? "bg-[#006DCC] text-white shadow-2xs"
                      : "bg-stone-100 text-neutral-700 hover:bg-stone-200"
                  }`}
                >
                  {g}
                </button>
              ))}
            </div>
          </div>

          {/* 3. Mục tiêu học tập */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-neutral-700 flex items-center gap-1.5">
              <Sparkles className="size-3.5 text-amber-500" />
              <span>Mục tiêu học tập ưu tiên</span>
            </label>
            <div className="space-y-1.5">
              {COMMON_TARGETS.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setTarget(t)}
                  className={`w-full text-left px-3.5 py-2 rounded-xl text-xs font-medium border transition-all cursor-pointer flex items-center justify-between ${
                    target === t
                      ? "border-[#006DCC] bg-sky-50/60 text-[#006DCC] font-semibold"
                      : "border-stone-200 bg-white text-neutral-700 hover:bg-stone-50"
                  }`}
                >
                  <span>{t}</span>
                  {target === t && <Check className="size-3.5 text-[#006DCC]" />}
                </button>
              ))}
            </div>
          </div>

          {/* 4. Hình thức & Khu vực */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-bold text-neutral-700 block mb-1.5">
                Hình thức học
              </label>
              <select
                value={mode}
                onChange={(e) => setMode(e.target.value as "ONLINE" | "OFFLINE" | "BOTH")}
                className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-xs text-neutral-800 focus:bg-white focus:outline-hidden focus:border-sky-500 font-medium"
              >
                <option value="BOTH">Online & Trực tiếp</option>
                <option value="ONLINE">Chỉ học Online</option>
                <option value="OFFLINE">Chỉ học Trực tiếp</option>
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-neutral-700 block mb-1.5 flex items-center gap-1">
                <MapPin className="size-3 text-neutral-400" />
                <span>Khu vực sinh sống</span>
              </label>
              <input
                type="text"
                value={area}
                onChange={(e) => setArea(e.target.value)}
                placeholder="Ví dụ: Quận 3, TP.HCM"
                className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-xs text-neutral-800 focus:bg-white focus:outline-hidden focus:border-sky-500 font-medium"
              />
            </div>
          </div>

          {/* 5. Khung giờ học */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-neutral-700 flex items-center gap-1.5">
              <Clock className="size-3.5 text-neutral-400" />
              <span>Khung giờ mong muốn học kèm</span>
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
              {COMMON_SLOTS.map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setSlots(s)}
                  className={`text-left px-3 py-2 rounded-xl text-[11px] font-medium border transition-all cursor-pointer ${
                    slots === s
                      ? "border-[#006DCC] bg-sky-50/60 text-[#006DCC] font-semibold"
                      : "border-stone-200 bg-white text-neutral-600 hover:bg-stone-50"
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* Buttons */}
          <div className="pt-3 border-t border-stone-100 flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-full text-xs font-semibold text-neutral-700 hover:bg-stone-100 transition-colors cursor-pointer"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              className="px-5 py-2.5 rounded-full text-xs font-bold text-white bg-[#006DCC] hover:bg-[#005bb5] shadow-xs transition-all cursor-pointer flex items-center gap-1.5"
            >
              <Sparkles className="size-3.5" />
              <span>Cập nhật & Ghép đôi lại</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
