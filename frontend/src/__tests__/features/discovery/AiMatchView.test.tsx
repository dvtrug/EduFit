import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AiMatchView } from "@/features/discovery/components/AiMatchView";
import type { MatchingNeed, TutorMatchResult } from "@/features/discovery/types";

const need: MatchingNeed = {
  goalId: "goal", subjectId: 1, subjectName: "Toán", educationLevelId: 2,
  gradeName: "THCS", targetDescription: "Củng cố", teachingMode: "ONLINE",
  timeSlotsDescription: "Thứ hai", budgetMin: 100000, budgetMax: 300000,
  goalType: "CONSOLIDATE", area: "", currentLevel: 2, desiredLevel: 4,
  deadline: "2030-06-01", availabilitySlots: [],
};
const matches: TutorMatchResult[] = ["Lan", "Hà"].map((name, i) => ({
  tutorId: name, matchScore: 95 - i,
  scoreBreakdown: { subjectFit: 100, levelFit: 50, scheduleFit: 100,
    ratingFit: 80, budgetFit: 100, overlappingSlots: 2 },
  explanation: `Lý do riêng cho ${name}`, aiGenerated: i === 0,
  tutorInfo: { tutorId: name, displayName: name, headline: null, shortBio: null,
    teachingMode: "ONLINE", area: null, pricePerSession: 200000,
    ratingAvg: 4, reviewCount: 8, verified: true, subjects: [] },
}));

describe("Personalized matching", () => {
  it("highlights the first ranked tutor and lists alternatives with their own explanation", () => {
    render(<AiMatchView matches={matches} isMatching={false} error={null} need={need} />);
    expect(screen.getByText("Gợi ý phù hợp nhất")).toBeInTheDocument();
    expect(screen.getByText("Các gia sư khác")).toBeInTheDocument();
    expect(screen.getByText("Lý do riêng cho Lan")).toBeInTheDocument();
    expect(screen.getByText("Lý do riêng cho Hà")).toBeInTheDocument();
  });

  it("shows four weighted criteria and the combined subject/level fit", () => {
    render(<AiMatchView matches={matches} isMatching={false} error={null} need={need} />);
    fireEvent.click(screen.getAllByRole("button", { name: "Xem lý do" })[0]);
    expect(screen.getByText("Nhu cầu học · 40%")).toBeInTheDocument();
    expect(screen.getByText("Năng lực & độ tin cậy · 25%")).toBeInTheDocument();
    expect(screen.getByText(/Lịch & hình thức · 25%/)).toBeInTheDocument();
    expect(screen.getByText("Ngân sách · 10%")).toBeInTheDocument();
    expect(screen.getByText("75%")).toBeInTheDocument();
  });
});
