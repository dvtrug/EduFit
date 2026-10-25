"use client";
import { useState, type FormEvent } from "react";
import { TutorWithdrawalPanel } from "./TutorWithdrawalPanel";
import { Wallet, ArrowDownToLine } from "lucide-react";
import {
  paymentApi,
  money,
  type Overview,
  type Checkout,
} from "../api/paymentApi";
interface Props {
  data: Overview;
  tutor: boolean;
  busy: boolean;
  execute: (work: () => Promise<unknown>) => Promise<boolean>;
}
export function WalletPanel(props: Props) {
  return props.tutor ? (
    <TutorWithdrawalPanel {...props} />
  ) : (
    <TopupPanel {...props} />
  );
}
function TopupPanel({ data, busy, execute }: Props) {
  const [amount, setAmount] = useState("100000");
  const [checkout, setCheckout] = useState<Checkout | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    await execute(async () => {
      const response = await paymentApi.topup(Number(amount));
      if (response.data) setCheckout(response.data);
    });
  }
  return (
    <section className="rounded-2xl border border-stone-200 bg-white p-5 sm:p-6 space-y-5">
      <div className="flex items-center gap-2">
        <Wallet className="size-5 text-amber-600" />
        <h2 className="font-bold">Ví học phí của bạn</h2>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm text-neutral-500">Số dư khả dụng</p>
          <p className="mt-1 text-2xl sm:text-3xl font-extrabold">
            {money(data.wallet.available)}
          </p>
        </div>
        <div>
          <p className="text-sm text-neutral-500">Đang giữ cho buổi học</p>
          <p className="mt-1 text-2xl sm:text-3xl font-extrabold text-amber-700">
            {money(data.wallet.held)}
          </p>
        </div>
      </div>
      {!data.topupEnabled && (
        <p
          role="status"
          className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900"
        >
          Kênh nạp tiền chưa được cấu hình. Liên hệ Admin để bật QR ngân hàng
          hoặc payOS. Chưa có tiền được nạp.
        </p>
      )}
      <form
        onSubmit={submit}
        className="space-y-3 border-t border-stone-100 pt-4"
      >
        <h3 className="flex items-center gap-2 font-semibold">
          <ArrowDownToLine className="size-4" /> Nạp tiền bằng QR
        </h3>
        <label className="block text-sm">
          Số tiền (VND)
          <input
            aria-label="Số tiền nạp"
            type="number"
            min="10000"
            max={100000000}
            step="1"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="mt-1 w-full rounded-lg border border-stone-300 p-2"
          />
        </label>
        <button
          disabled={busy || !data.topupEnabled}
          className="rounded-lg bg-amber-500 px-4 py-2 font-bold text-sm disabled:opacity-50"
        >
          Tạo yêu cầu nạp tiền
        </button>
        <p className="text-xs text-neutral-500">
          Số dư chỉ tăng khi giao dịch đã được xác minh. Thanh toán thành công
          không phụ thuộc việc quay lại trang này.
        </p>
      </form>
      {checkout && (
        <div
          role="status"
          className="rounded-xl bg-stone-50 p-4 space-y-2 text-sm"
        >
          {checkout.checkoutUrl ? (
            <a
              className="font-bold text-sky-700 underline"
              href={checkout.checkoutUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              Mở trang QR thanh toán payOS
            </a>
          ) : (
            <>
              <p>
                Chuyển {money(checkout.amount ?? 0)} đến {checkout.bankAccount}{" "}
                · {checkout.bankHolder}
              </p>
              <p>
                Nội dung bắt buộc: <strong>{checkout.description}</strong>
              </p>
              {checkout.qrUrl && (
                <a
                  className="text-sky-700 underline"
                  href={checkout.qrUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Mở mã QR chuyển khoản
                </a>
              )}
              <p>Admin xác minh giao dịch thực tế trước khi cộng ví.</p>
            </>
          )}
        </div>
      )}
    </section>
  );
}
