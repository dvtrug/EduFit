import { describe, it, expect, vi, beforeEach } from "vitest";
import { progressApi } from "@/features/progress/api/progressApi";
import * as apiModule from "@/lib/api";

describe("Progress API Client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("gọi GET /api/v1/progress/dashboard/student/{id} với các query params", async () => {
    const fetchSpy = vi.spyOn(apiModule, "fetchApi").mockResolvedValue({
      success: true,
      data: {
        studentId: "student-1",
        studentName: "Học sinh A",
        classes: [],
      },
    });

    const res = await progressApi.getStudentDashboard("student-1", "class-1", "60D");

    expect(fetchSpy).toHaveBeenCalledWith(
      "/api/v1/progress/dashboard/student/student-1?range=60D&classId=class-1",
      { method: "GET" }
    );
    expect(res.data?.studentName).toBe("Học sinh A");
  });

  it("gọi GET /api/v1/progress/dashboard/parent để lấy dữ liệu con em", async () => {
    const fetchSpy = vi.spyOn(apiModule, "fetchApi").mockResolvedValue({
      success: true,
      data: { students: [] },
    });

    const res = await progressApi.getParentDashboard();

    expect(fetchSpy).toHaveBeenCalledWith(
      "/api/v1/progress/dashboard/parent",
      { method: "GET" }
    );
    expect(res.data?.students).toEqual([]);
  });

  it("gọi GET /api/v1/progress/plans/{classId} để lấy kế hoạch học tập", async () => {
    const fetchSpy = vi.spyOn(apiModule, "fetchApi").mockResolvedValue({
      success: true,
      data: {
        planId: "plan-1",
        classId: "class-1",
        title: "Kế hoạch Toán 9",
        milestones: [],
        totalMilestones: 0,
        completedMilestones: 0,
        progressPercentage: 0,
      },
    });

    const res = await progressApi.getPlanByClassId("class-1");

    expect(fetchSpy).toHaveBeenCalledWith(
      "/api/v1/progress/plans/class-1",
      { method: "GET" }
    );
    expect(res.data?.title).toBe("Kế hoạch Toán 9");
  });
});
