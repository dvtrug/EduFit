"use client";
import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  paymentApi,
  paymentError,
  money,
  type AdminOverview,
  type PaymentSession,
} from "../api/paymentApi";
interface ReviewTarget {
  kind: "topups" | "withdrawals" | "disputes";
  id: string;
  approve: boolean;
  label: string;
  payout?: boolean;
}
export function PaymentAdminWorkspace() {
  const [data, setData] = useState<AdminOverview | null>(null);
  const [disputes, setDisputes] = useState<PaymentSession[]>([]);
  const [offset, setOffset] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [target, setTarget] = useState<ReviewTarget | null>(null);
  const [note, setNote] = useState("");
  const [reference, setReference] = useState("");
  const load = useCallback(async () => {
    const [wallets, cases] = await Promise.all([
      paymentApi.admin(offset),
      paymentApi.disputes(offset),
    ]);
    setData(wallets.data ?? null);
    setDisputes(cases.data ?? []);
  }, [offset]);
  useEffect(() => {
    let alive = true;
    const run = async () => {
      try {
        const [wallets, cases] = await Promise.all([
          paymentApi.admin(offset),
          paymentApi.disputes(offset),
        ]);
        if (alive) {
          setData(wallets.data ?? null);
          setDisputes(cases.data ?? []);
          setError(null);
        }
      } catch (e) {
        if (alive) setError(paymentError(e));
      }
    };
    void run();
    const timer = setInterval(() => {
      if (!document.hidden) void run();
    }, 15000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [offset]);
  function select(value: ReviewTarget) {
    setTarget(value);
    setNote("");
    setReference("");
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!target) return;
    setBusy(true);
    setError(null);
    try {
      if (target.payout) await paymentApi.payout(target.id, note);
      else
        await paymentApi.review(
          target.kind,
          target.id,
          target.approve,
          note,
          reference,
        );
      setTarget(null);
      await load();
    } catch (e) {
      setError(paymentError(e));
    } finally {
      setBusy(false);
    }
  }
  const button =
    "rounded-lg border border-stone-300 px-3 py-2 text-sm font-semibold hover:bg-stone-50 disabled:opacity-50";
  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <div>
        <h1 className="text-2xl font-extrabold">
          Đối soát & Tranh chấp học phí
        </h1>
        <p className="mt-1 text-sm text-neutral-500">
          Xác minh giao dịch ngân hàng và phân xử hồ sơ vấn đề.
        </p>
      </div>
      {data?.configuration && (
        <section
          className="rounded-2xl border bg-white p-5 space-y-2"
          aria-label="Trạng thái kênh thanh toán"
        >
          <h2 className="font-bold">Kết nối thanh toán</h2>
          <p>
            Nạp tự động payOS:{" "}
            {data.configuration.topupReady
              ? "Đã có cấu hình"
              : "Chưa đủ cấu hình"}
          </p>
          <p>
            QR đối soát thủ công:{" "}
            {data.configuration.manualReady
              ? "Đã có cấu hình"
              : "Chưa cấu hình"}
          </p>
          <p>
            Chi hộ payOS:{" "}
            {data.configuration.payoutReady
              ? "Đã bật và có cấu hình"
              : data.configuration.payoutEnabled
                ? "Đã bật nhưng thiếu khóa"
                : "Chưa bật"}
          </p>
          <p className="text-xs text-neutral-500">
            Trạng thái trên phản ánh cấu hình backend. Cần kiểm tra kênh trên
            payOS và webhook thực tế trước khi nhận tiền.
          </p>
        </section>
      )}
      {error && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 p-4 text-sm text-red-800"
        >
          {error}
        </p>
      )}
      {!data && <p role="status">Đang tải dữ liệu đối soát…</p>}
      {target && (
        <form
          onSubmit={submit}
          className="space-y-3 rounded-2xl border border-amber-300 bg-amber-50 p-5"
        >
          <h2 className="font-bold">{target.label}</h2>
          <p className="text-sm">Mã {target.id}</p>
          <label className="block text-sm">
            Ghi chú / lý do
            <textarea
              required
              maxLength={2000}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="mt-1 w-full rounded-lg border bg-white p-2"
            />
          </label>
          {target.kind !== "disputes" && target.approve && !target.payout && (
            <label className="block text-sm">
              Mã giao dịch ngân hàng đã xác minh
              <input
                required
                minLength={6}
                maxLength={200}
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                className="mt-1 w-full rounded-lg border bg-white p-2"
              />
              <span className="text-xs">
                Chỉ xác nhận sau khi đã đối chiếu giao dịch thật. Không nhập mã
                giả.
              </span>
            </label>
          )}
          <div className="flex gap-2">
            <button
              disabled={busy}
              className="rounded-lg bg-amber-500 px-4 py-2 text-sm font-bold disabled:opacity-50"
            >
              Xác nhận xử lý
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => setTarget(null)}
              className={button}
            >
              Hủy
            </button>
          </div>
        </form>
      )}
      <section className="space-y-3">
        <h2 className="text-lg font-bold">Hồ sơ vấn đề cần Admin xem xét</h2>
        {disputes.length === 0 && (
          <p className="rounded-xl border border-dashed p-5 text-sm text-neutral-500">
            Chưa có hồ sơ chờ Admin duyệt hoặc phân xử.
          </p>
        )}
        {disputes.map((s) => (
          <article
            key={s.sessionId}
            className="space-y-3 rounded-2xl border bg-white p-5"
          >
            <h3 className="font-bold">
              {s.tutorName} · {new Date(s.startAt).toLocaleString("vi-VN")}
            </h3>
            <p className="text-sm">
              Buổi {s.sessionId} · Ví cấp tiền {s.payerId}
            </p>
            <p className="text-sm">
              Đang giữ {money(s.amount)} ·{" "}
              {s.status === "ADMIN_REVIEW"
                ? "Quá hạn chưa thống nhất, cần phân xử"
                : "Hai bên đã thống nhất"}
            </p>
            <p className="text-sm">Lý do: {s.disputeReason}</p>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg bg-stone-50 p-3 text-sm">
                <strong>Báo cáo người học</strong>
                <p>{s.learnerReport}</p>
                <p>{money(s.learnerAmount ?? 0)}</p>
              </div>
              <div className="rounded-lg bg-stone-50 p-3 text-sm">
                <strong>Báo cáo gia sư</strong>
                <p>{s.tutorReport}</p>
                <p>{money(s.tutorAmount ?? 0)}</p>
              </div>
            </div>
            <Link
              className="inline-block rounded-lg border px-3 py-2 text-sm font-semibold text-sky-700"
              href={`/admin/disputes/${encodeURIComponent(s.sessionId)}`}
            >
              Mở hồ sơ và xử lý
            </Link>
          </article>
        ))}
      </section>
      <section className="space-y-3">
        <h2 className="text-lg font-bold">Yêu cầu nạp tiền</h2>
        {data?.topups.length === 0 && (
          <p className="text-sm text-neutral-500">Chưa có yêu cầu.</p>
        )}
        {data?.topups.map((t) => (
          <article
            key={t.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-white p-4 text-sm"
          >
            <div>
              <p className="font-semibold">
                {money(t.amount)} · {t.provider} · {t.status}
              </p>
              <p>
                Ví {t.user_id} · Mã EF{t.order_code.toString(36).toUpperCase()}
              </p>
            </div>
            {t.provider === "MANUAL" && t.status === "PENDING" && (
              <button
                disabled={busy}
                className={button}
                onClick={() =>
                  select({
                    kind: "topups",
                    id: t.id,
                    approve: true,
                    label: `Xác minh đã nhận ${money(t.amount)} qua ngân hàng`,
                  })
                }
              >
                Xác minh giao dịch
              </button>
            )}
          </article>
        ))}
      </section>
      <section className="space-y-3">
        <h2 className="text-lg font-bold">Yêu cầu chi trả gia sư</h2>
        {data?.withdrawals.length === 0 && (
          <p className="text-sm text-neutral-500">Chưa có yêu cầu.</p>
        )}
        {data?.withdrawals.map((w) => (
          <article
            key={w.id}
            className="space-y-2 rounded-xl border bg-white p-4 text-sm"
          >
            <p className="font-semibold">
              {money(w.amount)} · {w.status}
            </p>
            <p>
              {w.bank_name} · {w.bank_account} · {w.bank_holder}
            </p>
            <p>
              Ví gia sư {w.user_id} · BIN {w.bank_bin ?? "Chưa có"}
            </p>
            {w.provider_state && <p>Trạng thái chi hộ: {w.provider_state}</p>}
            {w.provider_message && (
              <p role="status" className="text-amber-800">
                {w.provider_message}
              </p>
            )}
            {w.status === "PROCESSING" && (
              <div className="space-y-2">
                <p>
                  Đang giữ tiền để xác minh kết quả ngân hàng. Không chuyển lại
                  thủ công.
                </p>
                <button
                  disabled={busy}
                  className={button}
                  onClick={async () => {
                    setBusy(true);
                    try {
                      await paymentApi.refreshPayout(w.id);
                      await load();
                    } catch (e) {
                      setError(paymentError(e));
                    } finally {
                      setBusy(false);
                    }
                  }}
                >
                  Đối soát kết quả chi
                </button>
              </div>
            )}
            {w.status === "PENDING" && (
              <div className="flex gap-2">
                {data.configuration?.payoutReady && w.bank_bin ? (
                  <button
                    disabled={busy}
                    className={button}
                    onClick={() =>
                      select({
                        kind: "withdrawals",
                        id: w.id,
                        approve: true,
                        payout: true,
                        label: `Duyệt chi hộ ${money(w.amount)} đến ${
                          w.bank_account
                        }`,
                      })
                    }
                  >
                    Duyệt và chuyển qua payOS
                  </button>
                ) : (
                  <button
                    disabled={busy}
                    className={button}
                    onClick={() =>
                      select({
                        kind: "withdrawals",
                        id: w.id,
                        approve: true,
                        label: `Xác nhận đã chuyển ngân hàng ${money(
                          w.amount,
                        )}`,
                      })
                    }
                  >
                    Đã chuyển ngân hàng
                  </button>
                )}
                <button
                  disabled={busy}
                  className={button}
                  onClick={() =>
                    select({
                      kind: "withdrawals",
                      id: w.id,
                      approve: false,
                      label: "Từ chối chi trả và trả tiền về số dư gia sư",
                    })
                  }
                >
                  Từ chối
                </button>
              </div>
            )}
          </article>
        ))}
      </section>
      <div className="flex justify-between">
        <button
          disabled={offset === 0 || busy}
          className={button}
          onClick={() => setOffset(Math.max(0, offset - 50))}
        >
          Trang trước
        </button>
        <span className="text-sm">Trang {offset / 50 + 1}</span>
        <button
          disabled={
            busy ||
            Math.max(
              data?.topups.length ?? 0,
              data?.withdrawals.length ?? 0,
              disputes.length,
            ) < 50
          }
          className={button}
          onClick={() => setOffset(offset + 50)}
        >
          Trang sau
        </button>
      </div>
    </div>
  );
}
