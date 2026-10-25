"use client";
import { useEffect, useState, type FormEvent } from "react";
import {
  paymentApi,
  money,
  chargeLabels,
  type PaymentSession,
} from "../api/paymentApi";
interface Props {
  session: PaymentSession;
  disabled: boolean;
  execute: (work: () => Promise<unknown>) => Promise<boolean>;
}
export function SessionPaymentCard({ session: s, disabled, execute }: Props) {
  const [mode, setMode] = useState<
    "fund" | "confirm" | "dispute" | "report" | null
  >(null);
  const [text, setText] = useState("");
  const [amount, setAmount] = useState(String(s.amount));
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const participant = s.canActAsLearner || s.canActAsTutor;
  const afterEnd = now >= new Date(s.endAt).getTime();
  const confirmationDeadline = new Date(s.endAt).getTime() + 86400000;
  const withinConfirmationWindow = now < confirmationDeadline;
  const pending = s.status === "FUNDED" || s.status === "AWAITING_CONFIRMATION";
  const ownAccepted = s.canActAsTutor ? s.tutorAccepted : s.learnerAccepted;
  const canConfirm =
    participant &&
    pending &&
    s.sessionStatus === "COMPLETED" &&
    afterEnd &&
    withinConfirmationWindow &&
    !ownAccepted;
  const canDispute =
    participant && pending && afterEnd && withinConfirmationWindow;
  const canReport =
    participant &&
    s.status === "DISPUTED" &&
    !!s.disputeDeadline &&
    now < new Date(s.disputeDeadline).getTime();
  async function submit(event: FormEvent) {
    event.preventDefault();
    const ok = await execute(() => {
      if (mode === "fund") return paymentApi.fund(s.sessionId, s.amount);
      if (mode === "confirm") return paymentApi.confirm(s.sessionId);
      if (mode === "dispute") return paymentApi.dispute(s.sessionId, text);
      return paymentApi.report(s.sessionId, Number(amount), text);
    });
    if (ok) {
      setMode(null);
      setText("");
    }
  }
  const button =
    "rounded-lg border border-stone-300 px-3 py-2 text-sm font-semibold disabled:opacity-50 hover:bg-stone-50";
  return (
    <article className="rounded-2xl border border-stone-200 bg-white p-5 space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 className="font-bold text-neutral-900">{s.tutorName}</h3>
          <p className="text-sm text-neutral-500">
            {new Date(s.startAt).toLocaleString("vi-VN")} · Lớp{" "}
            {s.classId.slice(0, 8)}
          </p>
          <p className="text-xs text-neutral-500">
            Học sinh {s.studentId.slice(0, 8)} · Kết quả: {s.sessionStatus}
          </p>
        </div>
        <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-bold text-amber-900">
          {chargeLabels[s.status]}
        </span>
      </div>
      <div className="flex flex-wrap justify-between gap-2 text-sm">
        <strong className="text-xl">{money(s.amount)}</strong>
        <span>
          Phí nền tảng {s.commissionBps / 100}% · Gia sư nhận{" "}
          {money(s.amount - Math.floor((s.amount * s.commissionBps) / 10000))}
        </span>
      </div>
      {s.payerId && (
        <p className="text-xs text-neutral-500">
          Ví cấp tiền: {s.payerId.slice(0, 8)} · Xác nhận người học:{" "}
          {s.learnerAccepted
            ? s.autoAccepted
              ? "Đã xác nhận (lịch sử)"
              : "Đã đồng ý"
            : "Chưa xác nhận"}{" "}
          · Gia sư: {s.tutorAccepted ? "Đã đồng ý" : "Chưa xác nhận"}
        </p>
      )}
      {pending && (
        <p className="text-xs text-neutral-500">
          {withinConfirmationWindow
            ? `Hai bên có 24 giờ sau buổi học để xác nhận hoặc khiếu nại. Không có khiếu nại, học phí tự tất toán sau ${new Date(
                confirmationDeadline,
              ).toLocaleString(
                "vi-VN",
              )} khi buổi học đã hoàn thành. Hai bên xác nhận sớm thì tất toán ngay.`
            : "Đã hết hạn khiếu nại. Đang chờ hệ thống tất toán khi buổi học đã hoàn thành."}
        </p>
      )}
      {s.disputeReason && (
        <div className="rounded-xl bg-amber-50 p-3 text-sm space-y-1">
          <p>
            <strong>Vấn đề:</strong> {s.disputeReason}
          </p>
          <p>
            Hạn giải quyết khiếu nại (48 giờ từ lúc mở):{" "}
            {s.disputeDeadline &&
              new Date(s.disputeDeadline).toLocaleString("vi-VN")}
          </p>
          <p>
            Người học:{" "}
            {s.learnerAmount === null ? "Chưa gửi" : money(s.learnerAmount)} —{" "}
            {s.learnerReport}
          </p>
          <p>
            Gia sư: {s.tutorAmount === null ? "Chưa gửi" : money(s.tutorAmount)}{" "}
            — {s.tutorReport}
          </p>
          {s.status === "DISPUTED" && (
            <p>Hết hạn chưa thống nhất: trả toàn bộ tiền về ví đã cấp tiền.</p>
          )}
          {s.status === "AWAITING_ADMIN" && (
            <p>Hai bên đã thống nhất; tiền đang được giữ để Admin duyệt.</p>
          )}
        </div>
      )}
      {s.status === "SETTLED" && (
        <p className="text-sm text-emerald-700">
          Đã tất toán {money(s.settledAmount)}; gia sư được hưởng{" "}
          {money(
            s.settledAmount -
              Math.floor((s.settledAmount * s.commissionBps) / 10000),
          )}
          .{s.autoAccepted && (!s.learnerAccepted || !s.tutorAccepted) && " Tự tất toán sau 24 giờ không có khiếu nại."}
        </p>
      )}
      <div className="flex flex-wrap gap-2">
        {s.canFund && s.amount > 0 && (
          <button
            className={button}
            disabled={disabled}
            onClick={() => setMode("fund")}
          >
            Cấp tiền từ ví
          </button>
        )}
        {canConfirm && (
          <button
            className={button}
            disabled={disabled}
            onClick={() => setMode("confirm")}
          >
            Xác nhận buổi học
          </button>
        )}
        {canDispute && (
          <button
            className={button}
            disabled={disabled}
            onClick={() => setMode("dispute")}
          >
            Khiếu nại
          </button>
        )}
        {canReport && (
          <button
            className={button}
            disabled={disabled}
            onClick={() => setMode("report")}
          >
            Gửi báo cáo giải quyết
          </button>
        )}
      </div>
      {mode && (
        <form
          onSubmit={submit}
          className="rounded-xl border border-amber-200 p-4 space-y-3"
        >
          <p className="text-sm font-semibold">
            {mode === "fund"
              ? `Đồng ý giữ ${money(
                  s.amount,
                )} từ ví để thanh toán buổi học này?`
              : mode === "confirm"
                ? `Xác nhận buổi học và cho phép tất toán ${money(
                    s.amount,
                  )} khi đủ xác nhận hai bên?`
                : mode === "dispute"
                  ? "Mở tranh chấp và chặn tất toán"
                  : "Báo cáo kết quả hai bên đã thống nhất"}
          </p>
          {mode === "report" && (
            <label className="block text-sm">
              Học phí thống nhất (trước phí nền tảng)
              <input
                aria-label="Học phí thống nhất"
                className="mt-1 w-full rounded-lg border p-2"
                type="number"
                min="0"
                max={s.amount}
                step="1"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </label>
          )}
          {(mode === "dispute" || mode === "report") && (
            <label className="block text-sm">
              {mode === "dispute" ? "Lý do tranh chấp" : "Nội dung báo cáo"}
              <textarea
                className="mt-1 w-full rounded-lg border p-2"
                required
                maxLength={mode === "dispute" ? 2000 : 4000}
                value={text}
                onChange={(e) => setText(e.target.value)}
              />
            </label>
          )}
          <div className="flex gap-2">
            <button
              className="rounded-lg bg-amber-500 px-4 py-2 text-sm font-bold disabled:opacity-50"
              disabled={disabled}
            >
              Gửi xác nhận
            </button>
            <button
              className={button}
              disabled={disabled}
              type="button"
              onClick={() => setMode(null)}
            >
              Hủy
            </button>
          </div>
        </form>
      )}
    </article>
  );
}
