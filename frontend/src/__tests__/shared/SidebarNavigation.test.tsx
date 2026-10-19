import React from "react";
import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { Sidebar } from "@/shared/ui/Sidebar";
import { StudentQuickActions } from "@/features/dashboard/components/student/StudentQuickActions";
import { ParentQuickActions } from "@/features/dashboard/components/parent/ParentQuickActions";
import { ParentDashboardView } from "@/features/dashboard/components/parent/ParentDashboardView";

// Mock next/navigation
vi.mock("next/navigation", () => ({
  usePathname: () => "/student/dashboard",
  useSearchParams: () => new URLSearchParams(),
}));

describe("Sidebar Finance Badges and Navigation", () => {
  it("renders Student sidebar with badge Sắp ra mắt on payments menu", () => {
    render(<Sidebar role="STUDENT" collapsed={false} onToggleCollapse={vi.fn()} />);
    const link = screen.getByRole("link", { name: /Học phí & Thanh toán/i });
    expect(link).toHaveAttribute("href", "/student/payments");
    expect(link).toHaveTextContent("Sắp ra mắt");
  });

  it("renders Tutor sidebar with badge Sắp ra mắt on earnings menu", () => {
    render(<Sidebar role="TUTOR" collapsed={false} onToggleCollapse={vi.fn()} />);
    const link = screen.getByRole("link", { name: /Báo cáo thu nhập/i });
    expect(link).toHaveAttribute("href", "/tutor/earnings");
    expect(link).toHaveTextContent("Sắp ra mắt");
  });

  it("renders Parent sidebar with badge Sắp ra mắt on billing menu", () => {
    render(<Sidebar role="PARENT" collapsed={false} onToggleCollapse={vi.fn()} />);
    const link = screen.getByRole("link", { name: /Gói học & Thanh toán/i });
    expect(link).toHaveAttribute("href", "/parent/billing");
    expect(link).toHaveTextContent("Sắp ra mắt");
  });

  it("includes badge in collapsed title tooltip for accessibility", () => {
    render(<Sidebar role="STUDENT" collapsed={true} onToggleCollapse={vi.fn()} />);
    const link = screen.getByTitle("Học phí & Thanh toán (Sắp ra mắt)");
    expect(link).toHaveAttribute("href", "/student/payments");
  });
});

describe("Dashboard Payment CTAs", () => {
  it("renders StudentQuickActions pointing to /student/payments with honest copy and badge", () => {
    render(<StudentQuickActions />);
    const link = screen.getByRole("link", { name: /Thông tin học phí/i });
    expect(link).toHaveAttribute("href", "/student/payments");
    expect(link).toHaveTextContent("Sắp ra mắt");
  });

  it("renders ParentQuickActions pointing to /parent/billing with honest copy and badge", () => {
    render(<ParentQuickActions />);
    const link = screen.getByRole("link", { name: /Thông tin học phí & gói học/i });
    expect(link).toHaveAttribute("href", "/parent/billing");
    expect(link).toHaveTextContent("Sắp ra mắt");
  });

  it("renders ParentDashboardView payment button pointing to /parent/billing with honest copy and badge", () => {
    render(<ParentDashboardView user={{ fullName: "Nguyễn Văn A", email: "a@test.com", role: "PARENT" }} />);
    const link = screen.getByRole("link", { name: /Thông tin học phí/i });
    expect(link).toHaveAttribute("href", "/parent/billing");
    expect(link).toHaveTextContent("Sắp ra mắt");
  });
});
