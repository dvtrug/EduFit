import type { LearningGoalData, LearningGoalType } from "@/services/profile";
import type { MatchingNeed, TutorAvailabilitySlot } from "../types";

export const GOAL_TYPE_OPTIONS: Array<{ value: LearningGoalType; label: string }> = [
  { value: "CONSOLIDATE", label: "Củng cố kiến thức nền tảng" },
  { value: "IMPROVE_GRADES", label: "Cải thiện điểm số trên lớp" },
  { value: "ADVANCED_STUDY", label: "Bồi dưỡng nâng cao / học sinh giỏi" },
  { value: "EXAM_PREPARATION", label: "Ôn thi chuyển cấp / thi tuyển" },
  { value: "OTHER", label: "Mục tiêu khác" },
];

const DAY_LABELS: Record<number, string> = {
  1: "Thứ 2",
  2: "Thứ 3",
  3: "Thứ 4",
  4: "Thứ 5",
  5: "Thứ 6",
  6: "Thứ 7",
  7: "Chủ Nhật",
};

export function isMiddleSchoolLevel(name: string): boolean {
  const match = name.match(/Lớp\s*(\d+)/i);
  if (!match) return false;
  const grade = Number(match[1]);
  return grade >= 6 && grade <= 9;
}

export function goalTypeLabel(goalType: LearningGoalType): string {
  return GOAL_TYPE_OPTIONS.find((option) => option.value === goalType)?.label || "Mục tiêu học tập";
}

export function formatMatchingSlots(slots: TutorAvailabilitySlot[]): string {
  if (slots.length === 0) return "Chưa chọn khung giờ";
  const sorted = [...slots].sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime));
  const first = sorted[0];
  const sameTime = sorted.every(
    (slot) => slot.startTime === first.startTime && slot.endTime === first.endTime
  );
  const days = sorted.map((slot) => DAY_LABELS[slot.dayOfWeek] || `Ngày ${slot.dayOfWeek}`).join(", ");
  if (!sameTime) {
    return sorted
      .map((slot) => `${DAY_LABELS[slot.dayOfWeek]} ${slot.startTime.slice(0, 5)}–${slot.endTime.slice(0, 5)}`)
      .join(" · ");
  }
  return `${days} · ${first.startTime.slice(0, 5)}–${first.endTime.slice(0, 5)}`;
}

export function learningGoalToMatchingNeed(goal: LearningGoalData): MatchingNeed {
  return {
    goalId: goal.goalId,
    subjectId: goal.subjectId,
    subjectName: goal.subjectName,
    educationLevelId: goal.educationLevelId,
    gradeName: goal.educationLevelName,
    goalType: goal.goalType,
    targetDescription: goalTypeLabel(goal.goalType),
    teachingMode: goal.mode,
    area: goal.area || "Toàn quốc",
    currentLevel: goal.currentLevel,
    desiredLevel: goal.desiredLevel,
    deadline: goal.deadline,
    budgetMin: goal.budgetMin,
    budgetMax: goal.budgetMax,
    availabilitySlots: goal.availabilitySlots,
    timeSlotsDescription: formatMatchingSlots(goal.availabilitySlots),
  };
}
