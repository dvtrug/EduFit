import { describe, it, expect, vi, beforeEach } from "vitest";
import { connectionApi } from "@/features/connection/api/connectionApi";
import { apiClient } from "@/lib/api";

describe("connectionApi client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("getTutorInbox gọi GET /api/v1/connections/requests khi không truyền phân trang", async () => {
    const mockInbox = [
      {
        requestId: "req-1",
        goalType: "EXAM",
        subjectId: 1,
        educationLevelId: 4,
        desiredSlots: ["Mon 19:00"],
        senderRole: "STUDENT",
        status: "PENDING",
        expiresAt: "2026-10-25T00:00:00Z",
        studentDisplayName: "Minh",
        hasSlotConflict: false,
      },
    ];

    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({
      success: true,
      data: mockInbox,
    });

    const res = await connectionApi.getTutorInbox();
    expect(getSpy).toHaveBeenCalledWith("/api/v1/connections/requests");
    expect(res).toEqual(mockInbox);
  });

  it("getTutorInbox gọi GET /api/v1/connections/requests/page khi có phân trang", async () => {
    const mockPage = {
      items: [
        {
          requestId: "req-2",
          goalType: "IMPROVE",
          subjectId: 2,
          educationLevelId: 5,
          desiredSlots: ["Tue 18:00"],
          senderRole: "PARENT",
          status: "PENDING",
          expiresAt: "2026-10-25T00:00:00Z",
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
    };

    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({
      success: true,
      data: mockPage,
    });

    const res = await connectionApi.getTutorInbox(0, 10);
    expect(getSpy).toHaveBeenCalledWith("/api/v1/connections/requests/page?page=0&size=10");
    expect(res).toEqual(mockPage.items);
  });

  it("respondRequest gọi POST /api/v1/connections/requests/{id}/respond để chấp thuận và tạo lớp", async () => {
    const mockClassResult = {
      classId: "cls-100",
      studentId: "stu-1",
      tutorId: "tut-1",
      goalId: "goal-1",
    };

    const postSpy = vi.spyOn(apiClient, "post").mockResolvedValue({
      success: true,
      data: mockClassResult,
    });

    const res = await connectionApi.respondRequest("req-1", {
      accept: true,
      reason: "Đồng ý nhận lớp",
    });

    expect(postSpy).toHaveBeenCalledWith("/api/v1/connections/requests/req-1/respond", {
      accept: true,
      reason: "Đồng ý nhận lớp",
    });
    expect(res).toEqual(mockClassResult);
  });

  it("respondRequest gọi POST /api/v1/connections/requests/{id}/respond để từ chối", async () => {
    const postSpy = vi.spyOn(apiClient, "post").mockResolvedValue({
      success: true,
      data: null,
    });

    const res = await connectionApi.respondRequest("req-1", {
      accept: false,
      reason: "Lịch bận không thể nhận thêm",
    });

    expect(postSpy).toHaveBeenCalledWith("/api/v1/connections/requests/req-1/respond", {
      accept: false,
      reason: "Lịch bận không thể nhận thêm",
    });
    expect(res).toBeNull();
  });

  it("getTutorClasses gọi GET /api/v1/connections/classes", async () => {
    const mockClasses = [
      {
        classId: "cls-1",
        studentId: "stu-1",
        tutorId: "tut-1",
        goalId: "goal-1",
        status: "ACTIVE",
        studentDisplayName: "Minh",
        scheduleStatus: "PROPOSED",
        sessionCount: 0,
        startedAt: "2026-10-18T00:00:00Z",
      },
    ];

    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({
      success: true,
      data: mockClasses,
    });

    const res = await connectionApi.getTutorClasses();
    expect(getSpy).toHaveBeenCalledWith("/api/v1/connections/classes");
    expect(res).toEqual(mockClasses);
  });

  it("getStudentClasses gọi GET /api/v1/connections/classes", async () => {
    const mockClasses = [
      {
        classId: "cls-2",
        studentId: "stu-2",
        tutorId: "tut-2",
        goalId: "goal-2",
        status: "ACTIVE",
        tutorDisplayName: "Thầy Hưng",
        scheduleStatus: "SCHEDULED",
        sessionCount: 4,
        startedAt: "2026-10-10T00:00:00Z",
      },
    ];

    const getSpy = vi.spyOn(apiClient, "get").mockResolvedValue({
      success: true,
      data: mockClasses,
    });

    const res = await connectionApi.getStudentClasses();
    expect(getSpy).toHaveBeenCalledWith("/api/v1/connections/classes");
    expect(res).toEqual(mockClasses);
  });

  it("cancelRequest gọi POST /api/v1/connections/requests/{id}/cancel", async () => {
    const postSpy = vi.spyOn(apiClient, "post").mockResolvedValue({
      success: true,
      data: undefined,
    });

    await connectionApi.cancelRequest("req-999");
    expect(postSpy).toHaveBeenCalledWith("/api/v1/connections/requests/req-999/cancel");
  });
});
