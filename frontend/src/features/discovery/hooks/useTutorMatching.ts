"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { profileService, type LearningGoalData } from "@/services/profile";
import { connectionService, type StudentLink } from "@/services/connections";
import { discoveryApi } from "../api/discoveryApi";
import { learningGoalToMatchingNeed } from "../utils/matchingNeed";
import type { MatchingNeed, TutorMatchResult } from "../types";

// Form defaults only; matching always uses a goal loaded from the backend.
export const DEFAULT_MATCHING_NEED: MatchingNeed = {
  subjectId: 1, subjectName: "Toán", educationLevelId: 4, gradeName: "Lớp 9",
  goalType: "EXAM_PREPARATION", targetDescription: "Ôn thi chuyển cấp / thi tuyển",
  teachingMode: "BOTH", area: "", currentLevel: 1, desiredLevel: 4,
  deadline: "2030-06-01", budgetMin: 150000, budgetMax: 300000,
  availabilitySlots: [], timeSlotsDescription: "Chưa chọn khung giờ",
};

export function useTutorMatching({ parent = false, enabled = false } = {}) {
  const [goals, setGoals] = useState<LearningGoalData[]>([]);
  const [links, setLinks] = useState<StudentLink[]>([]);
  const [studentId, setStudentId] = useState("");
  const [goalId, setGoalId] = useState("");
  const [matches, setMatches] = useState<TutorMatchResult[]>([]);
  const [isMatching, setIsMatching] = useState(false);
  const [isLoadingGoals, setIsLoadingGoals] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const goal = goals.find(item => item.goalId === goalId);
  const need = goal ? learningGoalToMatchingNeed(goal) : DEFAULT_MATCHING_NEED;

  useEffect(() => {
    if (!parent) return;
    let cancelled = false;
    connectionService.links().then(result => {
      if (cancelled) return;
      setLinks(result);
      setStudentId(result[0]?.studentId || "");
      if (!result.length) {
        setError("Chưa có liên kết phụ huynh–học sinh được xác nhận. Cần xác nhận liên kết trước khi ghép gia sư.");
        setIsLoadingGoals(false);
      }
    }).catch(err => {
      if (!cancelled) { setError(err.message); setIsLoadingGoals(false); }
    });
    return () => { cancelled = true; };
  }, [parent]);

  useEffect(() => {
    if (parent && !studentId) return;
    let cancelled = false;
    generation.current++;
    setGoals([]); setGoalId(""); setMatches([]); setError(null); setIsLoadingGoals(true);
    const load = parent ? profileService.getLinkedStudentGoals(studentId) : profileService.getMyLearningGoals();
    load.then(result => {
      if (cancelled) return;
      const active = result.filter(item => item.status === "ACTIVE");
      setGoals(active);
      setGoalId((active.find(item => item.readyForMatching) || active[0])?.goalId || "");
      if (!active.length) setError(parent ? "Học sinh chưa có mục tiêu đang hoạt động." : "Bạn chưa có mục tiêu học tập. Hãy chọn Thay đổi nhu cầu để tạo mục tiêu.");
    }).catch(err => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setIsLoadingGoals(false); });
    return () => { cancelled = true; };
  }, [parent, studentId]);

  // One request per selected goal. Empty results and errors never trigger retries.
  useEffect(() => {
    const version = ++generation.current;
    setMatches([]); setIsMatching(false);
    if (!enabled || !goal) return;
    if (goal.readyForMatching === false) {
      setError(`Mục tiêu chưa sẵn sàng: ${(goal.readinessReasons || []).join(", ")}`);
      return;
    }
    setIsMatching(true); setError(null);
    discoveryApi.matchTutors({ goalId: goal.goalId, topN: 6 }).then(response => {
      if (generation.current === version) setMatches(response.data || []);
    }).catch(err => { if (generation.current === version) setError(err.message); })
      .finally(() => { if (generation.current === version) setIsMatching(false); });
    return () => { generation.current++; };
  }, [enabled, goal]);

  const updateNeed = useCallback(async (newNeed: MatchingNeed) => {
    if (parent) throw new Error("Chỉ học sinh được chỉnh sửa mục tiêu học tập.");
    await profileService.updateStudentProfile({ educationLevelId: newNeed.educationLevelId, area: newNeed.area });
    const payload = {
      subjectId: newNeed.subjectId, currentLevel: newNeed.currentLevel, desiredLevel: newNeed.desiredLevel,
      goalType: newNeed.goalType, deadline: newNeed.deadline, mode: newNeed.teachingMode,
      area: newNeed.teachingMode === "ONLINE" ? undefined : newNeed.area,
      budgetMin: newNeed.budgetMin, budgetMax: newNeed.budgetMax, availabilitySlots: newNeed.availabilitySlots,
    };
    const existingGoalForSubject = goals.find(item => item.subjectId === newNeed.subjectId);
    const saved = existingGoalForSubject
      ? await profileService.updateLearningGoal(existingGoalForSubject.goalId, payload)
      : await profileService.createLearningGoal(payload);
    const refreshed = await profileService.getMyLearningGoals();
    setGoals(refreshed.filter(item => item.status === "ACTIVE"));
    setGoalId(saved.goalId);
  }, [parent, goals]);

  const refreshGoals = useCallback(async () => {
    if (parent && !studentId) return [];
    const load = parent ? profileService.getLinkedStudentGoals(studentId) : profileService.getMyLearningGoals();
    try {
      const result = await load;
      const active = result.filter(item => item.status === "ACTIVE");
      setGoals(active);
      return active;
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      return [];
    }
  }, [parent, studentId]);

  return { need, goal, goals, goalId, setGoalId, links, studentId, setStudentId,
    matches, isMatching: isMatching || isLoadingGoals, error, updateNeed, refreshGoals };
}

