import type { LearningGoalData, LearningGoalType } from "@/services/profile";
import type { MatchingNeed } from "../types";
import { DAY_LABELS, formatMatchingSlots } from "@/shared/lib/dateTime";

export { DAY_LABELS, formatMatchingSlots };

export const GOAL_TYPE_OPTIONS: Array<{ value: LearningGoalType; label: string }> = [
  { value: "CONSOLIDATE", label: "Củng cố kiến thức nền tảng" },
  { value: "IMPROVE_GRADES", label: "Cải thiện điểm số trên lớp" },
  { value: "ADVANCED_STUDY", label: "Bồi dưỡng nâng cao / học sinh giỏi" },
  { value: "EXAM_PREPARATION", label: "Ôn thi chuyển cấp / thi tuyển" },
  { value: "OTHER", label: "Mục tiêu khác" },
];

export function isMiddleSchoolLevel(name: string): boolean {
  const match = name.match(/Lớp\s*(\d+)/i);
  if (!match) return false;
  const grade = Number(match[1]);
  return grade >= 6 && grade <= 9;
}

export function goalTypeLabel(goalType: LearningGoalType): string {
  return GOAL_TYPE_OPTIONS.find((option) => option.value === goalType)?.label || "Mục tiêu học tập";
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
