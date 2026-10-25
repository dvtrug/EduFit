import React from "react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { SessionPaymentCard } from "@/features/payment/components/SessionPaymentCard";
import {
  paymentApi,
  type PaymentSession,
} from "@/features/payment/api/paymentApi";
vi.mock("@/features/payment/api/paymentApi", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/payment/api/paymentApi")
  >("@/features/payment/api/paymentApi");
  return {
    ...actual,
    paymentApi: {
      fund: vi.fn(),
      confirm: vi.fn(),
      dispute: vi.fn(),
      report: vi.fn(),
    },
  };
});
const session = (patch: Partial<PaymentSession> = {}): PaymentSession => ({
  sessionId: "session-1",
  classId: "class-1",
  studentId: "student-1",
  tutorName: "Gia sư An",
  sessionStatus: "COMPLETED",
  startAt: new Date(Date.now() - 7200000).toISOString(),
  endAt: new Date(Date.now() - 1800000).toISOString(),
  amount: 300000,
  commissionBps: 1000,
  payerId: "parent-1",
  status: "AWAITING_CONFIRMATION",
  learnerAccepted: false,
  tutorAccepted: true,
  autoAccepted: false,
  disputeDeadline: null,
  disputeReason: null,
  learnerAmount: null,
  tutorAmount: null,
  learnerReport: null,
  tutorReport: null,
  settledAmount: 0,
  canFund: false,
  canActAsLearner: true,
  canActAsTutor: false,
  ...patch,
});
const execute = async (work: () => Promise<unknown>) => {
  await work();
  return true;
};
beforeEach(() => vi.clearAllMocks());
describe("Session payment decisions", () => {
  it("explains automatic settlement without fabricating either participant's consent", () => {
    render(
      <SessionPaymentCard
        session={session({
          status: "SETTLED",
          autoAccepted: true,
          learnerAccepted: false,
          tutorAccepted: false,
          settledAmount: 300000,
        })}
        disabled={false}
        execute={execute}
      />,
    );
    expect(
      screen.getByText(/Tự tất toán sau 24 giờ không có khiếu nại/),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Khiếu nại" }),
    ).not.toBeInTheDocument();
  });
  it("offers both decisions for a tutor and closes complaints after 24 hours", () => {
    const { rerender } = render(
      <SessionPaymentCard
        session={session({
          tutorAccepted: false,
          canActAsLearner: false,
          canActAsTutor: true,
        })}
        disabled={false}
        execute={execute}
      />,
    );
    expect(
      screen.getByRole("button", { name: "Xác nhận buổi học" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Khiếu nại" }),
    ).toBeInTheDocument();
    expect(screen.getByText(/24 giờ/)).toBeInTheDocument();
    rerender(
      <SessionPaymentCard
        session={session({
          endAt: new Date(Date.now() - 86401000).toISOString(),
        })}
        disabled={false}
        execute={execute}
      />,
    );
    expect(
      screen.queryByRole("button", { name: "Khiếu nại" }),
    ).not.toBeInTheDocument();
    expect(screen.getByText(/Đang chờ hệ thống tất toán/)).toBeInTheDocument();
  });
  it("requires explicit confirmation before sending payment acceptance", async () => {
    render(
      <SessionPaymentCard
        session={session()}
        disabled={false}
        execute={execute}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận buổi học" }));
    expect(paymentApi.confirm).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Gửi xác nhận" }));
    await waitFor(() =>
      expect(paymentApi.confirm).toHaveBeenCalledWith("session-1"),
    );
  });
  it("does not offer payment confirmation before scheduling outcome is recorded", () => {
    render(
      <SessionPaymentCard
        session={session({ sessionStatus: "SCHEDULED" })}
        disabled={false}
        execute={execute}
      />,
    );
    expect(
      screen.queryByRole("button", { name: "Xác nhận buổi học" }),
    ).not.toBeInTheDocument();
  });
  it("opens a dispute with the supplied reason", async () => {
    render(
      <SessionPaymentCard
        session={session()}
        disabled={false}
        execute={execute}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Khiếu nại" }));
    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "Buổi học thiếu thời lượng" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi xác nhận" }));
    await waitFor(() =>
      expect(paymentApi.dispute).toHaveBeenCalledWith(
        "session-1",
        "Buổi học thiếu thời lượng",
      ),
    );
  });
  it("dispute removes confirm and allows a partial settlement report", async () => {
    render(
      <SessionPaymentCard
        session={session({
          status: "DISPUTED",
          disputeDeadline: new Date(Date.now() + 86400000).toISOString(),
          disputeReason: "Thiếu giờ",
        })}
        disabled={false}
        execute={execute}
      />,
    );
    expect(
      screen.queryByRole("button", { name: "Xác nhận buổi học" }),
    ).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Gửi báo cáo giải quyết" }),
    );
    fireEvent.change(screen.getByLabelText("Học phí thống nhất"), {
      target: { value: "150000" },
    });
    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "Hai bên thống nhất tính nửa buổi" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi xác nhận" }));
    await waitFor(() =>
      expect(paymentApi.report).toHaveBeenCalledWith(
        "session-1",
        150000,
        "Hai bên thống nhất tính nửa buổi",
      ),
    );
  });
  it("cannot submit expired reports or act for another learner", () => {
    render(
      <SessionPaymentCard
        session={session({
          status: "DISPUTED",
          disputeDeadline: new Date(Date.now() - 1000).toISOString(),
          canActAsLearner: false,
        })}
        disabled={false}
        execute={execute}
      />,
    );
    expect(
      screen.queryByRole("button", { name: "Gửi báo cáo giải quyết" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Xác nhận buổi học" }),
    ).not.toBeInTheDocument();
  });
  it("funding asks payer to accept the displayed price", async () => {
    render(
      <SessionPaymentCard
        session={session({
          status: "UNFUNDED",
          canFund: true,
          canActAsLearner: false,
        })}
        disabled={false}
        execute={execute}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Cấp tiền từ ví" }));
    expect(paymentApi.fund).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Gửi xác nhận" }));
    await waitFor(() =>
      expect(paymentApi.fund).toHaveBeenCalledWith("session-1", 300000),
    );
  });
});
