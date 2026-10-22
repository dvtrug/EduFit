import React from "react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { WalletPanel } from "@/features/payment/components/WalletPanel";
import { paymentApi, type Overview } from "@/features/payment/api/paymentApi";
vi.mock("@/features/payment/api/paymentApi", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/payment/api/paymentApi")
  >("@/features/payment/api/paymentApi");
  return { ...actual, paymentApi: { topup: vi.fn(), withdraw: vi.fn() } };
});
const execute = async (work: () => Promise<unknown>) => {
  await work();
  return true;
};
beforeEach(() => vi.clearAllMocks());
const overview: Overview = {
  wallet: { available: 1000000, held: 300000 },
  entries: [],
  topups: [],
  withdrawals: [],
  topupEnabled: true,
  topupProvider: "MANUAL",
};
describe("Wallet funding", () => {
  it("disabled provider cannot create a topup or show fake success", () => {
    render(
      <WalletPanel
        data={{ ...overview, topupEnabled: false }}
        tutor={false}
        busy={false}
        execute={execute}
      />,
    );
    expect(
      screen.getByRole("button", { name: "Tạo yêu cầu nạp tiền" }),
    ).toBeDisabled();
    expect(
      screen.getByText(/Kênh nạp tiền chưa được cấu hình/),
    ).toBeInTheDocument();
  });
  it("manual checkout only shows bank instructions, never increments the balance", async () => {
    vi.mocked(paymentApi.topup).mockResolvedValue({
      success: true,
      data: {
        id: "deposit-1",
        provider: "MANUAL",
        amount: 100000,
        bankAccount: "12345678",
        bankHolder: "EDUFIT",
        description: "EF1",
      },
    });
    render(
      <WalletPanel
        data={overview}
        tutor={false}
        busy={false}
        execute={execute}
      />,
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Tạo yêu cầu nạp tiền" }),
    );
    await screen.findByText("EF1");
    expect(
      screen.getByText(/Admin xác minh giao dịch thực tế/),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Đã nạp thành công/)).not.toBeInTheDocument();
  });
});

it("submits beneficiary BIN with a withdrawal and requires all bank details", async () => {
  render(
    <WalletPanel data={overview} tutor={true} busy={false} execute={execute} />,
  );
  expect(screen.getByRole("combobox", { name: "Ngân hàng" })).toBeRequired();
  fireEvent.change(screen.getByRole("combobox", { name: "Ngân hàng" }), {
    target: { value: "970422" },
  });
  fireEvent.change(screen.getByLabelText("Số tài khoản"), {
    target: { value: "12345678" },
  });
  fireEvent.change(screen.getByLabelText("Chủ tài khoản"), {
    target: { value: "Tutor" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Gửi yêu cầu rút tiền" }));
  expect(paymentApi.withdraw).toHaveBeenCalledWith(
    100000,
    "MBBank",
    "12345678",
    "Tutor",
    "970422",
  );
});
