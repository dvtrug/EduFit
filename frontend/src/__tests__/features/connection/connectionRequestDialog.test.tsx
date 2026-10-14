import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ConnectionRequestDialog } from "@/features/connection";
import { connectionService } from "@/services/connections";
import type { LearningGoalData } from "@/services/profile";

const goal: LearningGoalData = {
  goalId: "goal",
  subjectId: 1,
  subjectName: "Toán",
  educationLevelId: 4,
  educationLevelName: "Lớp 9",
  currentLevel: 1,
  desiredLevel: 4,
  goalType: "EXAM_PREPARATION",
  deadline: "2030-06-01",
  mode: "ONLINE",
  budgetMax: 300000,
  status: "ACTIVE",
  availabilitySlots: [{ dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" }],
};

afterEach(() => vi.restoreAllMocks());

describe("Connection request dialog", () => {
  it("shows server errors and never reports success for a failed connection", async () => {
    vi.spyOn(connectionService, "send").mockRejectedValue(new Error("Đã có yêu cầu đang chờ"));
    const onSent = vi.fn();
    render(<ConnectionRequestDialog tutorId="tutor" goal={goal} onClose={vi.fn()} onSent={onSent} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận gửi yêu cầu" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Đã có yêu cầu đang chờ");
    expect(onSent).not.toHaveBeenCalled();
  });

  it("sends the selected goal, tutor, availability and optional message before success", async () => {
    const send = vi.spyOn(connectionService, "send").mockResolvedValue({
      requestId: "request",
      studentId: "student",
      tutorId: "tutor",
      goalId: "goal",
      status: "PENDING",
      createdAt: "2030-01-01",
      expiresAt: "2030-01-08",
    });
    const onSent = vi.fn();
    render(<ConnectionRequestDialog tutorId="tutor" goal={goal} onClose={vi.fn()} onSent={onSent} />);
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Xin học thử" } });
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận gửi yêu cầu" }));
    await waitFor(() => expect(onSent).toHaveBeenCalledTimes(1));
    expect(send).toHaveBeenCalledWith({
      goalId: "goal",
      tutorId: "tutor",
      desiredSlots: ["Thứ 3 · 19:00–21:00"],
      message: "Xin học thử",
    });
  });
});
