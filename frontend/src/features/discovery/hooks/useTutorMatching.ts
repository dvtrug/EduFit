"use client";

import { useCallback, useState } from "react";
import { profileService } from "@/services/profile";
import { discoveryApi } from "../api/discoveryApi";
import { learningGoalToMatchingNeed } from "../utils/matchingNeed";
import type { MatchingNeed, TutorMatchResult } from "../types";

export const DEFAULT_MATCHING_NEED: MatchingNeed = {
  subjectId: 1,
  subjectName: "Toán",
  educationLevelId: 4,
  gradeName: "Lớp 9",
  goalType: "EXAM_PREPARATION",
  targetDescription: "Ôn thi chuyển cấp / thi tuyển",
  teachingMode: "BOTH",
  area: "Cầu Giấy, Hà Nội",
  currentLevel: 1,
  desiredLevel: 4,
  deadline: "2030-06-01",
  budgetMin: 150000,
  budgetMax: 300000,
  availabilitySlots: [
    { dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" },
    { dayOfWeek: 5, startTime: "19:00:00", endTime: "21:00:00" },
  ],
  timeSlotsDescription: "Thứ 3, Thứ 6 · 19:00–21:00",
};

export interface UseTutorMatchingOptions {
  defaultGoalId?: string;
  defaultTopN?: number;
  initialNeed?: MatchingNeed;
}

export function useTutorMatching(options: UseTutorMatchingOptions = {}) {
  const { defaultGoalId, defaultTopN = 6, initialNeed } = options;
  const [need, setNeed] = useState<MatchingNeed>(initialNeed || DEFAULT_MATCHING_NEED);
  const [matches, setMatches] = useState<TutorMatchResult[]>([]);
  const [isMatching, setIsMatching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const requestMatches = useCallback(async (goalId: string, topN: number) => {
    const response = await discoveryApi.matchTutors({ goalId, topN });
    const results = response.data || [];
    setMatches(
      results.map((item) => ({
        ...item,
        reasons: item.reasons || [
          `Đúng môn và cấp lớp (${Math.round(item.scoreBreakdown.subjectFit)}%)`,
          `Trùng ${item.scoreBreakdown.overlappingSlots} khung giờ học`,
          "Học phí và hình thức học phù hợp",
        ],
      }))
    );
  }, []);

  const runMatching = useCallback(
    async (goalId?: string, _customNeed?: MatchingNeed, topN?: number) => {
      setIsMatching(true);
      setError(null);
      try {
        let activeGoalId = goalId || defaultGoalId || need.goalId;
        if (!activeGoalId) {
          const goals = await profileService.getMyLearningGoals();
          const activeGoal = goals.find((goal) => goal.status === "ACTIVE");
          if (!activeGoal) {
            setMatches([]);
            setError("Bạn chưa có mục tiêu học tập đang hoạt động. Hãy tạo nhu cầu để nhận gợi ý gia sư.");
            return;
          }
          const activeNeed = learningGoalToMatchingNeed(activeGoal);
          setNeed(activeNeed);
          activeGoalId = activeGoal.goalId;
        }
        await requestMatches(activeGoalId, topN ?? defaultTopN);
      } catch (matchingError) {
        setMatches([]);
        setError(
          matchingError instanceof Error
            ? matchingError.message
            : "Không thể tải danh sách gia sư phù hợp từ hệ thống."
        );
      } finally {
        setIsMatching(false);
      }
    },
    [defaultGoalId, defaultTopN, need.goalId, requestMatches]
  );

  const updateNeed = useCallback(
    async (newNeed: MatchingNeed) => {
      setIsMatching(true);
      setError(null);
      try {
        await profileService.updateStudentProfile({
          educationLevelId: newNeed.educationLevelId,
          area: newNeed.area,
        });
        const goal = await profileService.createLearningGoal({
          subjectId: newNeed.subjectId,
          currentLevel: newNeed.currentLevel,
          desiredLevel: newNeed.desiredLevel,
          goalType: newNeed.goalType,
          deadline: newNeed.deadline,
          mode: newNeed.teachingMode,
          area: newNeed.teachingMode === "ONLINE" ? undefined : newNeed.area,
          budgetMin: newNeed.budgetMin,
          budgetMax: newNeed.budgetMax,
          availabilitySlots: newNeed.availabilitySlots,
        });
        const savedNeed = learningGoalToMatchingNeed(goal);
        setNeed(savedNeed);
        await requestMatches(goal.goalId, defaultTopN);
      } catch (matchingError) {
        setError(
          matchingError instanceof Error
            ? matchingError.message
            : "Không thể lưu nhu cầu học tập."
        );
        throw matchingError;
      } finally {
        setIsMatching(false);
      }
    },
    [defaultTopN, requestMatches]
  );

  const clearMatches = useCallback(() => {
    setMatches([]);
    setError(null);
  }, []);

  return { need, matches, isMatching, error, updateNeed, runMatching, clearMatches };
}
