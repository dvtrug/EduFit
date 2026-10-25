"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { RefreshCw } from "lucide-react";
import {
  paymentApi,
  paymentError,
  money,
  entryLabels,
  type Overview,
  type PaymentSession,
} from "../api/paymentApi";
import { WalletPanel } from "./WalletPanel";
import { SessionPaymentCard } from "./SessionPaymentCard";
export function PaymentWorkspace({
  role,
}: {
  role: "STUDENT" | "PARENT" | "TUTOR";
}) {
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  });
  const [offset, setOffset] = useState(0);
  const [data, setData] = useState<Overview | null>(null);
  const [sessions, setSessions] = useState<PaymentSession[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const balance = useRef<string | null>(null);
  const generation = useRef(0);
  const alive = useRef(true);
  const load = useCallback(async () => {
    const current = ++generation.current;
    try {
      const [year, m] = month.split("-").map(Number);
      if (!year || !m) return;
      const [wallet, list] = await Promise.all([
        paymentApi.wallet(offset),
        paymentApi.sessions(
          new Date(year, m - 1, 1).toISOString(),
          new Date(year, m, 1).toISOString(),
        ),
      ]);
      if (alive.current && current === generation.current) {
        const currentBalance = wallet.data
          ? `${wallet.data.wallet.available}:${wallet.data.wallet.held}`
          : null;
        if (balance.current !== null && currentBalance !== balance.current)
          setNotice(
            "Số dư ví vừa được cập nhật. Xem sổ giao dịch để biết chi tiết.",
          );
        balance.current = currentBalance;
        setData(wallet.data ?? null);
        setSessions(list.data ?? []);
        setError(null);
      }
    } catch (e) {
      if (alive.current && current === generation.current)
        setError(paymentError(e));
    }
  }, [month, offset]);
  useEffect(() => {
    alive.current = true;
    async function initial() {
      await load();
    }
    void initial();
    const timer = setInterval(() => {
      if (!document.hidden) void load();
    }, 15000);
    const focused = () => void load();
    window.addEventListener("focus", focused);
    return () => {
      alive.current = false;
      clearInterval(timer);
      window.removeEventListener("focus", focused);
    };
  }, [load]);
  async function execute(work: () => Promise<unknown>) {
    setBusy(true);
    setNotice(null);
    setError(null);
    try {
      await work();
      setNotice("Đã ghi nhận yêu cầu.");
      await load();
      return true;
    } catch (e) {
      setError(paymentError(e));
      return false;
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold">
            {role === "TUTOR" ? "Thu nhập & Thanh toán" : "Ví & Học phí"}
          </h1>
          <p className="mt-1 text-sm text-neutral-500">
            Nạp trước · Xác nhận sau buổi học · Phí nền tảng 10%
          </p>
        </div>
        <button
          disabled={busy}
          onClick={() => void load()}
          className="inline-flex items-center gap-2 rounded-lg border bg-white px-3 py-2 text-sm"
        >
          <RefreshCw className="size-4" />
          Làm mới
        </button>
      </div>
      {error && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 p-4 text-sm text-red-800"
        >
          {error}
        </p>
      )}
      {notice && (
        <p
          role="status"
          className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800"
        >
          {notice}
        </p>
      )}
      {!data ? (
        <p role="status">
          {error ? "Chưa tải được dữ liệu ví." : "Đang tải ví và học phí…"}
        </p>
      ) : (
        <>
          <WalletPanel
            data={data}
            tutor={role === "TUTOR"}
            busy={busy}
            execute={execute}
          />
          <section className="space-y-3">
            <div className="flex flex-wrap justify-between gap-3">
              <h2 className="text-lg font-bold">Học phí theo buổi</h2>
              <label className="text-sm">
                Tháng{" "}
                <input
                  aria-label="Tháng học phí"
                  type="month"
                  value={month}
                  onChange={(e) => setMonth(e.target.value)}
                  className="ml-2 rounded-lg border bg-white p-2"
                />
              </label>
            </div>
            <p className="text-sm text-neutral-500">
              Khiếu nại chặn thanh toán. Hai bên có 48 giờ từ lúc mở khiếu nại để gửi báo cáo cùng
              số tiền; Admin duyệt kết quả. Chưa thống nhất đúng hạn thì tiền
              trở về ví đã cấp.
            </p>
            {sessions.length === 0 ? (
              <div className="rounded-xl border border-dashed p-6 text-sm text-neutral-500">
                Chưa có buổi học trong tháng đã chọn.
              </div>
            ) : (
              sessions.map((s) => (
                <SessionPaymentCard
                  key={s.sessionId}
                  session={s}
                  disabled={busy}
                  execute={execute}
                />
              ))
            )}
          </section>
          <section className="rounded-2xl border bg-white p-5 space-y-3">
            <h2 className="font-bold">Lịch sử nạp / rút</h2>
            {data.topups.length === 0 && data.withdrawals.length === 0 && (
              <p className="text-sm text-neutral-500">
                Chưa có yêu cầu nạp hoặc rút tiền.
              </p>
            )}
            {data.topups.map((t) => (
              <div
                key={t.id}
                className="flex flex-wrap justify-between gap-2 border-t py-2 text-sm"
              >
                <span>
                  Nạp {money(t.amount)} · Mã EF
                  {t.order_code.toString(36).toUpperCase()}
                </span>
                <span>
                  {t.status === "PAID"
                    ? "Đã cộng ví"
                    : t.status === "CANCELLED"
                      ? "Đã hủy"
                      : "Chờ xác minh"}{" "}
                  {t.provider === "PAYOS" && t.status === "PENDING" && (
                    <button
                      disabled={busy}
                      className="ml-2 text-sky-700 underline"
                      onClick={() =>
                        void execute(() => paymentApi.refreshTopup(t.id))
                      }
                    >
                      Kiểm tra tiền đã chuyển
                    </button>
                  )}
                  {t.checkout_url && t.status === "PENDING" && (
                    <a
                      className="ml-2 text-sky-700 underline"
                      href={t.checkout_url}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Mở thanh toán
                    </a>
                  )}
                </span>
              </div>
            ))}
            {data.withdrawals.map((w) => (
              <p key={w.id} className="border-t py-2 text-sm">
                Rút {money(w.amount)} · {w.bank_name} / {w.bank_account} ·{" "}
                {w.status === "PAID"
                  ? "Đã chuyển ngân hàng"
                  : w.status === "REJECTED" || w.status === "FAILED"
                    ? "Đã từ chối, hoàn ví"
                    : w.status === "PROCESSING"
                      ? "Đang chuyển ngân hàng · đang giữ tiền"
                      : "Chờ Admin chi trả"}
              </p>
            ))}
          </section>
          <section className="rounded-2xl border bg-white p-5 space-y-3">
            <h2 className="font-bold">Sổ giao dịch ví</h2>
            {data.entries.length === 0 ? (
              <p className="text-sm text-neutral-500">Chưa có giao dịch.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[520px] text-left text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="py-2">Thời gian</th>
                      <th>Nội dung</th>
                      <th>Thay đổi khả dụng</th>
                      <th>Thay đổi đang giữ</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.entries.map((e) => (
                      <tr key={e.id} className="border-b border-stone-100">
                        <td className="py-3">
                          {new Date(e.created_at).toLocaleString("vi-VN")}
                        </td>
                        <td>{entryLabels[e.kind] ?? e.kind}</td>
                        <td>{money(e.available_delta)}</td>
                        <td>{money(e.held_delta)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="flex justify-between">
              <button
                disabled={offset === 0 || busy}
                onClick={() => setOffset(Math.max(0, offset - 50))}
                className="text-sm disabled:opacity-40"
              >
                Trang trước
              </button>
              <span className="text-sm">Trang {offset / 50 + 1}</span>
              <button
                disabled={
                  busy ||
                  Math.max(
                    data.entries.length,
                    data.topups.length,
                    data.withdrawals.length,
                  ) < 50
                }
                onClick={() => setOffset(offset + 50)}
                className="text-sm disabled:opacity-40"
              >
                Trang sau
              </button>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
