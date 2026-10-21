"use client";
import { useState, type FormEvent } from "react";
import { Wallet, ArrowDownToLine, ArrowUpFromLine } from "lucide-react";
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
export function WalletPanel({ data, tutor, busy, execute }: Props) {
  const [amount, setAmount] = useState("100000");
  const [bank, setBank] = useState("");
  const [account, setAccount] = useState("");
  const [holder, setHolder] = useState("");
  const [checkout, setCheckout] = useState<Checkout | null>(null);
  async function submit(e: FormEvent) {
    e.preventDefault();
    await execute(async () => {
      if (tutor)
        return paymentApi.withdraw(Number(amount), bank, account, holder);
      const response = await paymentApi.topup(Number(amount));
      if (response.data) setCheckout(response.data);
    });
  }
  return (
    <section className="rounded-2xl border border-stone-200 bg-white p-5 sm:p-6 space-y-5">
      <div className="flex items-center gap-2">
        <Wallet className="size-5 text-amber-600" />
        <h2 className="font-bold">
          {tutor ? "Ví thu nhập gia sư" : "Ví học phí của bạn"}
        </h2>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm text-neutral-500">
            {tutor ? "Có thể yêu cầu rút" : "Số dư khả dụng"}
          </p>
          <p className="mt-1 text-2xl sm:text-3xl font-extrabold">
            {money(data.wallet.available)}
          </p>
        </div>
        <div>
          <p className="text-sm text-neutral-500">
            {tutor ? "Đang chờ chi trả" : "Đang giữ cho buổi học"}
          </p>
          <p className="mt-1 text-2xl sm:text-3xl font-extrabold text-amber-700">
            {money(data.wallet.held)}
          </p>
        </div>
      </div>
      {!tutor && !data.topupEnabled && (
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
          {tutor ? (
            <ArrowUpFromLine className="size-4" />
          ) : (
            <ArrowDownToLine className="size-4" />
          )}
          {tutor ? "Yêu cầu rút về ngân hàng" : "Nạp tiền bằng QR"}
        </h3>
        <label className="block text-sm">
          Số tiền (VND)
          <input
            aria-label={tutor ? "Số tiền rút" : "Số tiền nạp"}
            type="number"
            min="10000"
            max={tutor ? Math.min(data.wallet.available, 100000000) : 100000000}
            step="1"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="mt-1 w-full rounded-lg border border-stone-300 p-2"
          />
        </label>
        {tutor && (
          <div className="grid gap-3 sm:grid-cols-3">
            <label className="text-sm">
              Ngân hàng
              <input
                required
                maxLength={100}
                value={bank}
                onChange={(e) => setBank(e.target.value)}
                className="mt-1 w-full rounded-lg border p-2"
              />
            </label>
            <label className="text-sm">
              Số tài khoản
              <input
                required
                pattern="[0-9]{5,34}"
                inputMode="numeric"
                value={account}
                onChange={(e) => setAccount(e.target.value)}
                className="mt-1 w-full rounded-lg border p-2"
              />
            </label>
            <label className="text-sm">
              Chủ tài khoản
              <input
                required
                maxLength={150}
                value={holder}
                onChange={(e) => setHolder(e.target.value)}
                className="mt-1 w-full rounded-lg border p-2"
              />
            </label>
          </div>
        )}
        <button
          disabled={
            busy ||
            (!tutor && !data.topupEnabled) ||
            (tutor && data.wallet.available < 10000)
          }
          className="rounded-lg bg-amber-500 px-4 py-2 font-bold text-sm disabled:opacity-50"
        >
          {tutor ? "Gửi yêu cầu rút tiền" : "Tạo yêu cầu nạp tiền"}
        </button>
        <p className="text-xs text-neutral-500">
          {tutor
            ? "Admin đối soát và chuyển tiền vào tài khoản bạn cung cấp. Kiểm tra kỹ thông tin trước khi gửi."
            : "Số dư chỉ tăng khi giao dịch đã được xác minh. Thanh toán thành công không phụ thuộc việc quay lại trang này."}
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
