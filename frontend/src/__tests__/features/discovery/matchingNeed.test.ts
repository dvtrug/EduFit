import { describe, expect, it } from "vitest";
import {
  GOAL_TYPE_OPTIONS,
  isMiddleSchoolLevel,
  learningGoalToMatchingNeed,
} from "@/features/discovery/utils/matchingNeed";

describe("matching need", () => {
  it("chỉ nhận các lớp THCS từ lớp 6 đến lớp 9", () => {
    expect(["Lớp 5", "Lớp 6", "Lớp 7", "Lớp 8", "Lớp 9", "Lớp 10"].filter(isMiddleSchoolLevel))
      .toEqual(["Lớp 6", "Lớp 7", "Lớp 8", "Lớp 9"]);
  });

  it("dùng đúng các loại mục tiêu mà backend hỗ trợ", () => {
    expect(GOAL_TYPE_OPTIONS.map((option) => option.value)).toEqual([
      "CONSOLIDATE",
      "IMPROVE_GRADES",
      "ADVANCED_STUDY",
      "EXAM_PREPARATION",
      "OTHER",
    ]);
  });

  it("chuyển mục tiêu trong database thành nội dung nhu cầu hiển thị", () => {
    const need = learningGoalToMatchingNeed({
      goalId: "55555555-5555-5555-5555-000000000001",
      subjectId: 1,
      subjectName: "Toán",
      educationLevelId: 4,
      educationLevelName: "Lớp 9",
      currentLevel: 1,
      desiredLevel: 4,
      goalType: "EXAM_PREPARATION",
      deadline: "2030-06-01",
      mode: "BOTH",
      area: "Cầu Giấy, Hà Nội",
      budgetMin: 150000,
      budgetMax: 300000,
      status: "ACTIVE",
      availabilitySlots: [
        { dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" },
        { dayOfWeek: 5, startTime: "19:00:00", endTime: "21:00:00" },
      ],
    });

    expect(need.gradeName).toBe("Lớp 9");
    expect(need.targetDescription).toBe("Ôn thi chuyển cấp / thi tuyển");
    expect(need.timeSlotsDescription).toBe("Thứ 3, Thứ 6 · 19:00–21:00");
  });
});
