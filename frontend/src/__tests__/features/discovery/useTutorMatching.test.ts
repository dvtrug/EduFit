import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useTutorMatching, DEFAULT_MATCHING_NEED } from "@/features/discovery/hooks/useTutorMatching";
import { profileService, type LearningGoalData } from "@/services/profile";
import { connectionService } from "@/services/connections";
import { discoveryApi } from "@/features/discovery/api/discoveryApi";

const goal: LearningGoalData = {
  goalId: "goal-1", subjectId: 1, subjectName: "Toán", educationLevelId: 4, educationLevelName: "Lớp 9",
  currentLevel: 1, desiredLevel: 4, goalType: "EXAM_PREPARATION", deadline: "2030-06-01",
  mode: "ONLINE", budgetMax: 300000, status: "ACTIVE", readyForMatching: true,
  availabilitySlots: [{ dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" }],
};
beforeEach(() => {
  vi.spyOn(profileService, "getMyLearningGoals").mockResolvedValue([goal]);
});
afterEach(() => vi.restoreAllMocks());

describe("Matching workflow", () => {
  it("does not spend quota until the matching tab opens, and does not retry empty results", async () => {
    const match = vi.spyOn(discoveryApi, "matchTutors").mockResolvedValue({ success: true, data: [] });
    const { result, rerender } = renderHook(({ enabled }) => useTutorMatching({ enabled }), { initialProps: { enabled: false } });
    await waitFor(() => expect(result.current.goalId).toBe("goal-1"));
    expect(match).not.toHaveBeenCalled();
    rerender({ enabled: true });
    await waitFor(() => expect(result.current.isMatching).toBe(false));
    expect(match).toHaveBeenCalledTimes(1);
    rerender({ enabled: true });
    expect(match).toHaveBeenCalledTimes(1);
  });

  it("keeps API failures visible without automatic repeated calls", async () => {
    const match = vi.spyOn(discoveryApi, "matchTutors").mockRejectedValue(new Error("Quota exhausted"));
    const { result } = renderHook(() => useTutorMatching({ enabled: true }));
    await waitFor(() => expect(result.current.error).toBe("Quota exhausted"));
    expect(result.current.matches).toEqual([]);
    expect(match).toHaveBeenCalledTimes(1);
  });

  it("blocks an incomplete goal before calling matching", async () => {
    vi.mocked(profileService.getMyLearningGoals).mockResolvedValue([{ ...goal, readyForMatching: false, readinessReasons: ["GOAL_AVAILABILITY_MISSING"] }]);
    const match = vi.spyOn(discoveryApi, "matchTutors");
    const { result } = renderHook(() => useTutorMatching({ enabled: true }));
    await waitFor(() => expect(result.current.error).toContain("GOAL_AVAILABILITY_MISSING"));
    expect(match).not.toHaveBeenCalled();
  });

  it("loads the confirmed linked child's goals and prevents parent profile writes", async () => {
    vi.spyOn(connectionService, "links").mockResolvedValue([{ linkId: "link", studentId: "child", status: "CONFIRMED" }]);
    const linked = vi.spyOn(profileService, "getLinkedStudentGoals").mockResolvedValue([goal]);
    const update = vi.spyOn(profileService, "updateStudentProfile");
    const { result } = renderHook(() => useTutorMatching({ parent: true }));
    await waitFor(() => expect(result.current.goalId).toBe("goal-1"));
    expect(linked).toHaveBeenCalledWith("child");
    expect(profileService.getMyLearningGoals).not.toHaveBeenCalled();
    await act(async () => {
      await expect(result.current.updateNeed(DEFAULT_MATCHING_NEED)).rejects.toThrow("Chỉ học sinh");
    });
    expect(update).not.toHaveBeenCalled();
  });
});
