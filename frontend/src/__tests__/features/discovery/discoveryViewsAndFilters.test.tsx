import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { QuickFilterPills } from "@/features/discovery/components/filters/QuickFilterPills";
import { MatchingNeedModal } from "@/features/discovery/components/MatchingNeedModal";
import { ConnectionRequestDialog } from "@/features/connection/components/ConnectionRequestDialog";
import { useTutorMatching } from "@/features/discovery/hooks/useTutorMatching";
import { profileService, type LearningGoalData } from "@/services/profile";
import { catalogService } from "@/services/catalog";
import { connectionService } from "@/services/connections";
import { MAJOR_VIETNAM_CITIES } from "@/shared/constants/locations";
import type { MatchingNeed, TutorSearchParams } from "@/features/discovery/types";
import { renderHook, act } from "@testing-library/react";

const mockGoal1: LearningGoalData = {
  goalId: "goal-1",
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
  readyForMatching: true,
  availabilitySlots: [
    { dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" },
    { dayOfWeek: 5, startTime: "19:00:00", endTime: "21:00:00" },
  ],
};

const mockGoal2: LearningGoalData = {
  goalId: "goal-2",
  subjectId: 3,
  subjectName: "Tiếng Anh",
  educationLevelId: 4,
  educationLevelName: "Lớp 9",
  currentLevel: 2,
  desiredLevel: 5,
  goalType: "CONSOLIDATE",
  deadline: "2030-06-01",
  mode: "OFFLINE",
  area: "Hà Nội",
  budgetMax: 250000,
  status: "ACTIVE",
  readyForMatching: true,
  availabilitySlots: [
    { dayOfWeek: 6, startTime: "08:00:00", endTime: "10:00:00" },
  ],
};

const mockNeed: MatchingNeed = {
  subjectId: 1,
  subjectName: "Toán",
  educationLevelId: 4,
  gradeName: "Lớp 9",
  goalType: "EXAM_PREPARATION",
  targetDescription: "Ôn thi chuyển cấp / thi tuyển",
  teachingMode: "BOTH",
  area: "Hà Nội",
  currentLevel: 1,
  desiredLevel: 4,
  deadline: "2030-06-01",
  budgetMin: 150000,
  budgetMax: 300000,
  availabilitySlots: [
    { dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" },
    { dayOfWeek: 5, startTime: "19:00:00", endTime: "21:00:00" },
  ],
  timeSlotsDescription: "Tối Thứ 3 & Thứ 6",
};

describe("QuickFilterPills: Chuẩn hóa khu vực và lịch rảnh (Fix 400 Bad Request)", () => {
  it("hiển thị danh sách Tỉnh/Thành phố toàn quốc (MAJOR_VIETNAM_CITIES) thay vì 8 quận TP.HCM", () => {
    render(
      <QuickFilterPills
        params={{}}
        subjects={[]}
        levels={[]}
        showAdvanced={false}
        hasActiveFilters={false}
        onUpdateFilters={vi.fn()}
        onResetFilters={vi.fn()}
        onToggleAdvanced={vi.fn()}
        onClearKeywordInput={vi.fn()}
      />
    );

    const areaSelect = screen.getByLabelText("Lọc theo khu vực");
    expect(areaSelect).toBeInTheDocument();

    // Phải có Hà Nội, Đà Nẵng, TP. Hồ Chí Minh
    expect(screen.getByRole("option", { name: "Hà Nội" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "TP. Hồ Chí Minh" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Đà Nẵng" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Toàn quốc (Online)" })).toBeInTheDocument();

    // Không còn hardcode chỉ 8 quận
    expect(screen.queryByRole("option", { name: "Tân Bình" })).not.toBeInTheDocument();
  });

  it("gửi kèm availableFrom và availableTo khi chọn dayOfWeek để tránh lỗi 400 Bad Request", () => {
    const onUpdateFilters = vi.fn();
    render(
      <QuickFilterPills
        params={{}}
        subjects={[]}
        levels={[]}
        showAdvanced={false}
        hasActiveFilters={false}
        onUpdateFilters={onUpdateFilters}
        onResetFilters={vi.fn()}
        onToggleAdvanced={vi.fn()}
        onClearKeywordInput={vi.fn()}
      />
    );

    const daySelect = screen.getByLabelText("Lọc theo lịch rảnh trong tuần");
    fireEvent.change(daySelect, { target: { value: "2" } }); // Thứ 3

    expect(onUpdateFilters).toHaveBeenCalledWith({
      dayOfWeek: 2,
      availableFrom: "00:00:00",
      availableTo: "23:59:59",
    });
  });

  it("xóa availableFrom và availableTo khi bỏ chọn dayOfWeek", () => {
    const onUpdateFilters = vi.fn();
    render(
      <QuickFilterPills
        params={{ dayOfWeek: 2, availableFrom: "00:00:00", availableTo: "23:59:59" }}
        subjects={[]}
        levels={[]}
        showAdvanced={false}
        hasActiveFilters={true}
        onUpdateFilters={onUpdateFilters}
        onResetFilters={vi.fn()}
        onToggleAdvanced={vi.fn()}
        onClearKeywordInput={vi.fn()}
      />
    );

    const daySelect = screen.getByLabelText("Lọc theo lịch rảnh trong tuần");
    fireEvent.change(daySelect, { target: { value: "" } });

    expect(onUpdateFilters).toHaveBeenCalledWith({
      dayOfWeek: undefined,
      availableFrom: undefined,
      availableTo: undefined,
    });
  });
});

describe("MatchingNeedModal: Multi-select & Toggle Khung giờ mong muốn", () => {
  beforeEach(() => {
    vi.spyOn(catalogService, "getSubjects").mockResolvedValue([
      { subjectId: 1, name: "Toán" },
      { subjectId: 3, name: "Tiếng Anh" },
    ]);
    vi.spyOn(catalogService, "getEducationLevels").mockResolvedValue([
      { levelId: 4, name: "Lớp 9", sortOrder: 4 },
    ]);
  });

  afterEach(() => vi.restoreAllMocks());

  it("cho phép click preset để toggle bỏ chọn hoặc gộp nhiều khung giờ", async () => {
    render(
      <MatchingNeedModal
        isOpen={true}
        currentNeed={mockNeed}
        onClose={vi.fn()}
        onSave={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/Tối Thứ 3 & Thứ 6/)).toBeInTheDocument();
    });

    // Preset "Tối Thứ 3 & Thứ 6" đang được chọn theo mockNeed
    const tueFriBtn = screen.getByRole("button", { name: /Tối Thứ 3 & Thứ 6/i });
    const monWedBtn = screen.getByRole("button", { name: /Tối Thứ 2 & Thứ 4/i });

    // Click preset 2 (Tối Thứ 2 & Thứ 4) -> Gộp cả hai preset
    fireEvent.click(monWedBtn);

    // Click "Xóa tất cả"
    const clearBtn = screen.getByRole("button", { name: "Xóa tất cả" });
    fireEvent.click(clearBtn);

    expect(screen.getByText(/Chưa chọn khung giờ nào/i)).toBeInTheDocument();

    // Click lại preset 1 để chọn lại
    fireEvent.click(tueFriBtn);
    expect(screen.getByText(/Đã chọn:/i)).toBeInTheDocument();
  });
});

describe("ConnectionRequestDialog: Hỗ trợ chọn Mục tiêu khi có nhiều hơn 1", () => {
  it("hiển thị dropdown chọn mục tiêu khi goals có nhiều phần tử", () => {
    const onSelectGoal = vi.fn();
    render(
      <ConnectionRequestDialog
        tutorId="tutor-123"
        goal={mockGoal1}
        goals={[mockGoal1, mockGoal2]}
        onSelectGoal={onSelectGoal}
        onClose={vi.fn()}
        onSent={vi.fn()}
      />
    );

    const goalSelect = screen.getByLabelText("Chọn mục tiêu học tập ghép lớp");
    expect(goalSelect).toBeInTheDocument();

    // Chuyển sang mục tiêu Tiếng Anh
    fireEvent.change(goalSelect, { target: { value: "goal-2" } });
    expect(onSelectGoal).toHaveBeenCalledWith(mockGoal2);
    expect(screen.getAllByText(/Tiếng Anh · Lớp 9/).length).toBeGreaterThanOrEqual(2);
  });
});

describe("useTutorMatching: Cập nhật mục tiêu qua PUT tránh sinh rác", () => {
  beforeEach(() => {
    vi.spyOn(profileService, "getMyLearningGoals").mockResolvedValue([mockGoal1]);
  });

  afterEach(() => vi.restoreAllMocks());

  it("gọi updateLearningGoal thay vì createLearningGoal khi goalId đã tồn tại", async () => {
    const updateGoalSpy = vi.spyOn(profileService, "updateLearningGoal").mockResolvedValue({
      ...mockGoal1,
    });
    const createGoalSpy = vi.spyOn(profileService, "createLearningGoal");
    vi.spyOn(profileService, "updateStudentProfile").mockResolvedValue({} as any);

    const { result } = renderHook(() => useTutorMatching({ enabled: true }));
    await waitFor(() => expect(result.current.goalId).toBe("goal-1"));

    await act(async () => {
      await result.current.updateNeed(mockNeed);
    });

    expect(updateGoalSpy).toHaveBeenCalledWith("goal-1", expect.objectContaining({
      subjectId: 1,
      goalType: "EXAM_PREPARATION",
    }));
    expect(createGoalSpy).not.toHaveBeenCalled();
  });
});
