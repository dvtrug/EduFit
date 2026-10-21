import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ConnectionRequestDialog } from "@/features/connection";
import { connectionService } from "@/services/connections";
import { profileService, type LearningGoalData } from "@/services/profile";

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

const tutorWithSubjects = {
  tutorId: "tutor-123",
  displayName: "Thầy Nguyễn Văn A",
  pricePerSession: 250000,
  teachingMode: "ONLINE" as const,
  subjects: [
    { subjectId: 1, subjectName: "Toán", educationLevelId: 4, educationLevelName: "Lớp 9" },
    { subjectId: 2, subjectName: "Vật lý", educationLevelId: 4, educationLevelName: "Lớp 9" },
    { subjectId: 3, subjectName: "Hóa học", educationLevelId: 4, educationLevelName: "Lớp 9" },
  ],
  availabilitySlots: [
    { dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" },
    { dayOfWeek: 4, startTime: "19:00:00", endTime: "21:00:00" },
  ],
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

  it("hiển thị danh sách môn gia sư thực sự giảng dạy và huy hiệu có mục tiêu", () => {
    render(
      <ConnectionRequestDialog
        tutorId={tutorWithSubjects.tutorId}
        tutor={tutorWithSubjects}
        goal={goal}
        goals={[goal]}
        onClose={vi.fn()}
        onSent={vi.fn()}
      />
    );

    // Môn học gia sư giảng dạy (3 môn)
    expect(screen.getByText(/Môn học gia sư giảng dạy \(3\)/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Toán · Lớp 9/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Vật lý · Lớp 9/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hóa học · Lớp 9/ })).toBeInTheDocument();
    expect(screen.getByText("Đã có mục tiêu")).toBeInTheDocument();
  });

  it("học sinh chọn môn chưa có mục tiêu -> hiển thị form tạo nhanh inline và gửi thành công", async () => {
    const createdGoal: LearningGoalData = {
      goalId: "new-physics-goal",
      subjectId: 2,
      subjectName: "Vật lý",
      educationLevelId: 4,
      educationLevelName: "Lớp 9",
      currentLevel: 1,
      desiredLevel: 3,
      goalType: "IMPROVE_GRADES",
      deadline: "2030-10-01",
      mode: "ONLINE",
      budgetMax: 250000,
      status: "ACTIVE",
      availabilitySlots: [{ dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" }],
    };

    const createGoalSpy = vi.spyOn(profileService, "createLearningGoal").mockResolvedValue(createdGoal);
    const sendSpy = vi.spyOn(connectionService, "send").mockResolvedValue({
      requestId: "req-phys",
      studentId: "student",
      tutorId: tutorWithSubjects.tutorId,
      goalId: "new-physics-goal",
      status: "PENDING",
      createdAt: "2030-01-01",
      expiresAt: "2030-01-08",
    });

    const onSent = vi.fn();
    const onGoalCreated = vi.fn();

    render(
      <ConnectionRequestDialog
        tutorId={tutorWithSubjects.tutorId}
        tutor={tutorWithSubjects}
        goals={[goal]}
        userRole="STUDENT"
        onGoalCreated={onGoalCreated}
        onClose={vi.fn()}
        onSent={onSent}
      />
    );

    // Click chuyển sang môn Vật lý · Lớp 9
    const physicsBtn = screen.getByRole("button", { name: /Vật lý · Lớp 9/ });
    fireEvent.click(physicsBtn);

    // Form khởi tạo nhanh xuất hiện
    expect(screen.getByText(/Khởi tạo nhanh mục tiêu môn Vật lý · Lớp 9/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Khởi tạo mục tiêu & Gửi yêu cầu" })).toBeInTheDocument();

    // Click gửi yêu cầu
    fireEvent.click(screen.getByRole("button", { name: "Khởi tạo mục tiêu & Gửi yêu cầu" }));

    await waitFor(() => {
      expect(createGoalSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          subjectId: 2,
          currentLevel: 1,
          desiredLevel: 3,
          goalType: "IMPROVE_GRADES",
          mode: "ONLINE",
        })
      );
      expect(sendSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          goalId: "new-physics-goal",
          tutorId: tutorWithSubjects.tutorId,
        })
      );
      expect(onGoalCreated).toHaveBeenCalledWith(createdGoal);
      expect(onSent).toHaveBeenCalled();
    });
  });

  it("phụ huynh khi học sinh chưa có mục tiêu môn được chọn -> hiển thị hướng dẫn và nút nhắc nhở con", () => {
    render(
      <ConnectionRequestDialog
        tutorId={tutorWithSubjects.tutorId}
        tutor={tutorWithSubjects}
        goals={[goal]} // Chỉ có mục tiêu môn Toán
        userRole="PARENT"
        selectedChildName="Bé An"
        onClose={vi.fn()}
        onSent={vi.fn()}
      />
    );

    // Chọn môn Vật lý
    const physicsBtn = screen.getByRole("button", { name: /Vật lý · Lớp 9/ });
    fireEvent.click(physicsBtn);

    // Hiển thị cảnh báo hướng dẫn phụ huynh
    expect(screen.getByText(/Chưa có mục tiêu học tập môn Vật lý/)).toBeInTheDocument();
    expect(screen.getByText(/Bé An/)).toBeInTheDocument();

    // Nút nhắc con tạo mục tiêu
    const reminderBtn = screen.getByRole("button", { name: "Nhắc con tạo mục tiêu môn này" });
    expect(reminderBtn).toBeInTheDocument();
    fireEvent.click(reminderBtn);
    expect(screen.getByRole("button", { name: "Đã gửi nhắc nhở cho con" })).toBeDisabled();

    // Có gợi ý chuyển sang môn con đã có sẵn mục tiêu (môn Toán)
    const mathSwitchBtn = screen.getByRole("button", { name: /Toán \(1 mục tiêu\)/ });
    expect(mathSwitchBtn).toBeInTheDocument();

    // Click chuyển về môn Toán
    fireEvent.click(mathSwitchBtn);
    expect(screen.getByRole("button", { name: "Xác nhận gửi yêu cầu" })).toBeInTheDocument();
  });

  it("ưu tiên khớp đúng cả khối lớp (educationLevelId) khi gia sư dạy cùng một môn ở nhiều khối khác nhau", () => {
    const multiLevelTutor = {
      ...tutorWithSubjects,
      subjects: [
        { subjectId: 1, subjectName: "Toán", educationLevelId: 4, educationLevelName: "Lớp 9" },
        { subjectId: 1, subjectName: "Toán", educationLevelId: 7, educationLevelName: "Lớp 12" },
      ],
    };

    const goalGrade12: LearningGoalData = {
      ...goal,
      goalId: "goal-12",
      educationLevelId: 7,
      educationLevelName: "Lớp 12",
    };

    render(
      <ConnectionRequestDialog
        tutorId={multiLevelTutor.tutorId}
        tutor={multiLevelTutor}
        goal={goalGrade12}
        goals={[goalGrade12]}
        onClose={vi.fn()}
        onSent={vi.fn()}
      />
    );

    // Nút Toán · Lớp 12 phải là nút được chọn (active)
    const btnGrade12 = screen.getByRole("button", { name: /Toán · Lớp 12/ });
    expect(btnGrade12).toHaveClass("bg-blue-600");
  });

  it("hiển thị trạng thái thông báo rõ ràng khi gia sư chưa cập nhật môn học và học sinh chưa có mục tiêu", () => {
    render(
      <ConnectionRequestDialog
        tutorId="tutor-empty"
        tutor={{ tutorId: "tutor-empty", displayName: "Gia sư mới", subjects: [] }}
        goals={[]}
        onClose={vi.fn()}
        onSent={vi.fn()}
      />
    );

    expect(screen.getByText("Chưa có thông tin môn học giảng dạy")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Đóng" })).toBeInTheDocument();
  });
});
