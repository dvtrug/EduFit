import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { TutorPendingRequests } from "@/features/connection/components/TutorPendingRequests";
import { connectionApi } from "@/features/connection/api/connectionApi";
import type { TutorInboxItem } from "@/features/connection/types";

const mockPendingRequests: TutorInboxItem[] = [
  {
    requestId: "req-1",
    goalType: "EXAM_PREPARATION",
    subjectId: 1,
    educationLevelId: 4,
    desiredSlots: ["Thứ 2 · 19:00-21:00"],
    message: "Em cần gia sư kèm môn Toán 9",
    senderRole: "STUDENT",
    status: "PENDING",
    expiresAt: "2026-10-25T00:00:00Z",
    studentDisplayName: "Trần Minh",
    subjectName: "Toán học",
    educationLevelName: "Lớp 9",
    hasSlotConflict: false,
  },
  {
    requestId: "req-2",
    goalType: "FOUNDATION",
    subjectId: 2,
    educationLevelId: 5,
    desiredSlots: ["Thứ 3 · 18:00-20:00", "Thứ 5 · 18:00-20:00"],
    message: "Phụ huynh muốn tìm gia sư cho con",
    senderRole: "PARENT",
    status: "PENDING",
    expiresAt: "2026-10-25T00:00:00Z",
    studentDisplayName: "Lê Hoàng",
    subjectName: "Vật lý",
    educationLevelName: "Lớp 10",
    hasSlotConflict: true,
    conflictingSlots: ["Thứ 3 · 18:00-20:00"],
  },
];

describe("TutorPendingRequests Component", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("hiển thị trạng thái rỗng (empty state) khi không có yêu cầu nào", async () => {
    vi.spyOn(connectionApi, "getTutorInbox").mockResolvedValue([]);

    render(<TutorPendingRequests />);

    expect(
      await screen.findByText("Chưa có yêu cầu kết nối nào đang chờ")
    ).toBeInTheDocument();
  });

  it("hiển thị trạng thái lỗi (error state) khi API thất bại và cho phép thử lại", async () => {
    const inboxSpy = vi
      .spyOn(connectionApi, "getTutorInbox")
      .mockRejectedValueOnce(new Error("Lỗi mạng khi tải hộp thư"))
      .mockResolvedValueOnce(mockPendingRequests);

    render(<TutorPendingRequests />);

    expect(await screen.findByText("Lỗi mạng khi tải hộp thư")).toBeInTheDocument();

    const retryBtn = screen.getByRole("button", { name: "Thử lại" });
    fireEvent.click(retryBtn);

    expect(await screen.findByText("Trần Minh")).toBeInTheDocument();
    expect(inboxSpy).toHaveBeenCalledTimes(2);
  });

  it("hiển thị danh sách yêu cầu với cảnh báo xung đột lịch học trực quan", async () => {
    vi.spyOn(connectionApi, "getTutorInbox").mockResolvedValue(mockPendingRequests);

    render(<TutorPendingRequests />);

    expect(await screen.findByText("Trần Minh")).toBeInTheDocument();
    expect(screen.getByText("Lê Hoàng")).toBeInTheDocument();

    // Kiểm tra cảnh báo xung đột lịch học của request số 2
    const conflictWarning = screen.getByTestId("conflict-warning");
    expect(conflictWarning).toBeInTheDocument();
    expect(conflictWarning).toHaveTextContent("Cảnh báo trùng khung giờ");
    expect(conflictWarning).toHaveTextContent("Thứ 3 · 18:00-20:00");
  });

  it("mở modal chấp thuận khi nhấn 'Chấp thuận tạo lớp'", async () => {
    vi.spyOn(connectionApi, "getTutorInbox").mockResolvedValue(mockPendingRequests);

    render(<TutorPendingRequests />);

    const approveButtons = await screen.findAllByRole("button", {
      name: /Chấp thuận tạo lớp/i,
    });
    fireEvent.click(approveButtons[0]);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Chấp thuận yêu cầu kết nối")).toBeInTheDocument();
  });

  it("mở modal từ chối khi nhấn 'Từ chối'", async () => {
    vi.spyOn(connectionApi, "getTutorInbox").mockResolvedValue(mockPendingRequests);

    render(<TutorPendingRequests />);

    const rejectButtons = await screen.findAllByRole("button", {
      name: "Từ chối",
    });
    fireEvent.click(rejectButtons[0]);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Từ chối yêu cầu kết nối")).toBeInTheDocument();
  });
});
