import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { TutorDashboardView } from "@/features/dashboard/components/tutor/TutorDashboardView";
import { connectionApi } from "@/features/connection/api/connectionApi";
import { schedulingApi } from "@/features/scheduling";
import { verificationService } from "@/services/verification";
import { profileService } from "@/services/profile";
import type { TutoringClass, TutorInboxItem } from "@/features/connection/types";
import type { CalendarItem } from "@/features/scheduling/types";

describe("TutorDashboardView Component", () => {
  const mockUser = {
    fullName: "Thầy Hưng",
    email: "tutor@edufit.vn",
    role: "TUTOR",
  };

  const mockClasses: TutoringClass[] = [
    {
      classId: "cls-1",
      studentId: "stu-1",
      tutorId: "tut-1",
      goalId: "goal-1",
      status: "ACTIVE",
      studentDisplayName: "Nguyễn Văn Nam",
      subjectName: "Toán hình học",
      educationLevelName: "Lớp 11",
      scheduleStatus: "SCHEDULED",
      sessionCount: 0,
      completedCount: 0,
      scheduledCount: 9,
      totalCount: 9,
      startedAt: "2026-10-17T10:00:00Z",
    },
    {
      classId: "cls-2",
      studentId: "stu-2",
      tutorId: "tut-1",
      goalId: "goal-2",
      status: "ACTIVE",
      studentDisplayName: "Trần Mai Anh",
      subjectName: "Vật lý",
      educationLevelName: "Lớp 10",
      scheduleStatus: "SCHEDULED",
      sessionCount: 3,
      completedCount: 3,
      scheduledCount: 5,
      totalCount: 8,
      startedAt: "2026-10-01T10:00:00Z",
    },
  ];

  const mockPendingRequests: TutorInboxItem[] = [
    {
      requestId: "req-1",
      goalType: "FOUNDATION",
      subjectId: 1,
      educationLevelId: 4,
      studentDisplayName: "Lê Hoàng",
      subjectName: "Toán học",
      educationLevelName: "Lớp 9",
      senderRole: "STUDENT",
      status: "PENDING",
      desiredSlots: ["Thứ 2 · 19:00-21:00"],
      expiresAt: "2026-10-25T00:00:00Z",
      message: "Em muốn học bổ trợ môn Toán",
      hasSlotConflict: false,
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();

    vi.spyOn(verificationService, "getMyRequest").mockResolvedValue({
      status: "VERIFIED",
    } as any);

    vi.spyOn(profileService, "getMyTutorProfile").mockResolvedValue({
      subjects: [{ subjectId: 1, subjectName: "Toán", educationLevelId: 4, educationLevelName: "Lớp 9" }],
      availabilitySlots: [{ slotId: "s1", dayOfWeek: 1, startTime: "08:00:00", endTime: "10:00:00" }],
      pricePerSession: 250000,
      ratingAvg: 4.9,
      reviewCount: 15,
    } as any);
  });

  it("nạp dữ liệu thực tế và tính toán đúng số buổi hoàn thành 3 / 17 trong KPI grid", async () => {
    vi.spyOn(connectionApi, "getTutorClasses").mockResolvedValue(mockClasses);
    vi.spyOn(connectionApi, "getPendingRequests").mockResolvedValue(mockPendingRequests);
    vi.spyOn(schedulingApi, "getCalendar").mockResolvedValue({
      success: true,
      data: [],
    });

    render(<TutorDashboardView user={mockUser} />);

    expect(await screen.findByText("Chào Thầy/Cô Thầy Hưng 👋")).toBeInTheDocument();

    // KPI: 3 completed / 17 planned sessions
    expect(await screen.findByText("3 / 17")).toBeInTheDocument();
    // 2 active students
    expect(screen.getByText("2 học sinh")).toBeInTheDocument();
    // Pending requests counter
    expect(screen.getByText("Lê Hoàng")).toBeInTheDocument();
  });

  it("lọc bỏ các buổi học đã kết thúc (COMPLETED) hoặc trong quá khứ khỏi danh sách sắp tới", async () => {
    const now = new Date();
    const futureDate = new Date(now.getTime() + 2 * 3600 * 1000); // +2 hours
    const pastDate = new Date(now.getTime() - 4 * 3600 * 1000); // -4 hours

    const mockCalendar: CalendarItem[] = [
      {
        sessionId: "sess-completed-past",
        classId: "cls-1",
        tutorId: "tut-1",
        studentId: "stu-1",
        startAt: new Date(pastDate.getTime() - 3600 * 1000).toISOString(),
        endAt: pastDate.toISOString(),
        mode: "ONLINE",
        status: "COMPLETED",
        isLateCancel: false,
      },
      {
        sessionId: "sess-active-upcoming",
        classId: "cls-1",
        tutorId: "tut-1",
        studentId: "stu-1",
        startAt: futureDate.toISOString(),
        endAt: new Date(futureDate.getTime() + 3600 * 1000).toISOString(),
        mode: "ONLINE",
        status: "SCHEDULED",
        isLateCancel: false,
      },
    ];

    vi.spyOn(connectionApi, "getTutorClasses").mockResolvedValue(mockClasses);
    vi.spyOn(connectionApi, "getPendingRequests").mockResolvedValue([]);
    vi.spyOn(schedulingApi, "getCalendar").mockResolvedValue({
      success: true,
      data: mockCalendar,
    });

    render(<TutorDashboardView user={mockUser} />);

    expect(await screen.findByText("Chào Thầy/Cô Thầy Hưng 👋")).toBeInTheDocument();

    // Buổi học tương lai SCHEDULED hiển thị với badge 'Đã chốt lịch'
    expect(await screen.findByText("Đã chốt lịch")).toBeInTheDocument();
    expect(screen.getByText("Học sinh: Nguyễn Văn Nam")).toBeInTheDocument();

    // Không có badge 'Chờ xác nhận' sai lệch cho buổi học COMPLETED
    expect(screen.queryByText("Chờ xác nhận")).not.toBeInTheDocument();
  });
});
