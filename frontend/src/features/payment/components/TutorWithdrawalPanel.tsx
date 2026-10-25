"use client";
import { useState, type FormEvent } from "react";
import banks from "../data/banks.json";
import {
  money,
  paymentApi,
  type Overview,
  type PayoutAccount,
} from "../api/paymentApi";

type Execute = (work: () => Promise<unknown>) => Promise<boolean>;
const mask = (value: string) => `•••• ${value.slice(-4)}`;

function RecipientEditor({
  recipient,
  busy,
  execute,
  close,
}: {
  recipient: PayoutAccount | null;
  busy: boolean;
  execute: Execute;
  close: () => void;
}) {
  const [bin, setBin] = useState(recipient?.bank_bin ?? "");
  const [account, setAccount] = useState(recipient?.bank_account ?? "");
  const [holder, setHolder] = useState(recipient?.bank_holder ?? "");
  const [checked, setChecked] = useState(false);
  async function save(event: FormEvent) {
    event.preventDefault();
    const bank = banks.find((item) => item.bin === bin);
    if (!checked || !bank || busy) return;
    const ok = await execute(() =>
      paymentApi.savePayoutAccount({
        bank: bank.name,
        bankBin: bin,
        account: account.trim(),
        holder: holder.trim(),
        expectedAccountId: recipient?.id ?? null,
      }),
    );
    if (ok) close();
  }
  return (
    <form
      onSubmit={save}
      className="space-y-4 rounded-xl border border-stone-200 bg-stone-50 p-4"
    >
      <h3 className="font-semibold">
        {recipient
          ? "Đổi tài khoản nhận tiền"
          : "Lưu tài khoản nhận tiền một lần"}
      </h3>
      <p className="text-sm text-neutral-600">
        Các lần rút sau sẽ dùng tài khoản này. Yêu cầu rút đang chờ vẫn giữ tài
        khoản đã chọn trước đó.
      </p>
      <fieldset disabled={busy} className="grid gap-3 sm:grid-cols-2">
        <label className="text-sm">
          Ngân hàng
          <select
            required
            value={bin}
            onChange={(event) => {
              setBin(event.target.value);
              setChecked(false);
            }}
            className="mt-1 w-full rounded-lg border bg-white p-2"
          >
            <option value="">Chọn ngân hàng nhận</option>
            {banks.map((bank) => (
              <option key={bank.bin} value={bank.bin}>
                {bank.name} — {bank.fullName}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          Số tài khoản
          <input
            required
            inputMode="numeric"
            pattern="[0-9]{5,34}"
            maxLength={34}
            value={account}
            onChange={(event) => {
              setAccount(event.target.value);
              setChecked(false);
            }}
            className="mt-1 w-full rounded-lg border p-2"
          />
        </label>
        <label className="text-sm sm:col-span-2">
          Chủ tài khoản
          <input
            required
            maxLength={150}
            value={holder}
            onChange={(event) => {
              setHolder(event.target.value);
              setChecked(false);
            }}
            className="mt-1 w-full rounded-lg border p-2"
          />
        </label>
        <label className="flex items-start gap-2 text-sm sm:col-span-2">
          <input
            type="checkbox"
            required
            checked={checked}
            onChange={(event) => setChecked(event.target.checked)}
            className="mt-1"
          />
          Tôi đã kiểm tra ngân hàng, số tài khoản và tên người nhận.
        </label>
      </fieldset>
      <p className="text-xs text-neutral-500">
        Thông tin do bạn cung cấp; Admin đối soát người nhận trước khi chi trả.
        Lưu tài khoản chưa tạo yêu cầu rút tiền.
      </p>
      <div className="flex flex-wrap gap-3">
        <button
          disabled={busy || !checked}
          className="rounded-lg bg-amber-500 px-4 py-2 text-sm font-bold disabled:opacity-50"
        >
          Lưu tài khoản nhận tiền
        </button>
        {recipient && (
          <button
            type="button"
            disabled={busy}
            onClick={close}
            className="rounded-lg border bg-white px-4 py-2 text-sm"
          >
            Hủy thay đổi
          </button>
        )}
      </div>
    </form>
  );
}

export function TutorWithdrawalPanel({
  data,
  busy,
  execute,
}: {
  data: Overview;
  busy: boolean;
  execute: Execute;
}) {
  const recipient = data.payoutAccount ?? null;
  const [editing, setEditing] = useState(false);
  const [amount, setAmount] = useState("");
  const [confirmation, setConfirmation] = useState<{
    amount: number;
    recipient: PayoutAccount;
  } | null>(null);
  const pending = data.withdrawals.find(
    (item) => item.status === "PENDING" || item.status === "PROCESSING",
  );
  const maximum = Math.min(data.wallet.available, 100000000);
  const value = Number(amount);
  const valid =
    Number.isSafeInteger(value) && value >= 10000 && value <= maximum;
  async function confirm() {
    if (
      !confirmation ||
      busy ||
      pending ||
      !recipient ||
      recipient.id !== confirmation.recipient.id ||
      confirmation.amount > data.wallet.available
    )
      return;
    const ok = await execute(() =>
      paymentApi.withdrawSaved(confirmation.amount, confirmation.recipient.id),
    );
    if (ok) {
      setConfirmation(null);
      setAmount("");
    }
  }
  return (
    <section className="space-y-5 rounded-2xl border border-stone-200 bg-white p-5 sm:p-6">
      <h2 className="font-bold">Ví thu nhập gia sư</h2>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm text-neutral-500">Có thể yêu cầu rút</p>
          <p className="mt-1 text-2xl font-extrabold sm:text-3xl">
            {money(data.wallet.available)}
          </p>
        </div>
        <div>
          <p className="text-sm text-neutral-500">Đang chờ chi trả</p>
          <p className="mt-1 text-2xl font-extrabold text-amber-700 sm:text-3xl">
            {money(data.wallet.held)}
          </p>
        </div>
      </div>
      {recipient && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-stone-50 p-4">
          <div>
            <p className="text-xs text-neutral-500">
              Tài khoản nhận tiền mặc định
            </p>
            <p className="font-semibold">
              {recipient.bank_name} · {mask(recipient.bank_account)}
            </p>
            <p className="text-sm">{recipient.bank_holder}</p>
          </div>
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              setEditing(true);
              setConfirmation(null);
            }}
            className="rounded-lg border bg-white px-3 py-2 text-sm"
          >
            Đổi tài khoản
          </button>
        </div>
      )}
      {!recipient || editing ? (
        <RecipientEditor
          key={recipient?.id ?? "new"}
          recipient={recipient}
          busy={busy}
          execute={execute}
          close={() => setEditing(false)}
        />
      ) : (
        <>
          {pending && (
            <p role="status" className="rounded-lg bg-amber-50 p-3 text-sm">
              Bạn có yêu cầu rút đang chờ duyệt hoặc chuyển tiền. Yêu cầu hiện
              tại vẫn nhận vào {pending.bank_name} ·{" "}
              {mask(pending.bank_account)}. Hãy chờ kết quả trước khi rút tiếp.
            </p>
          )}
          {data.wallet.available < 10000 && (
            <p role="status" className="text-sm text-amber-800">
              Số dư khả dụng tối thiểu để rút là 10.000đ. Thu nhập phải được tất
              toán vào ví trước.
            </p>
          )}
          <form
            onSubmit={(event) => {
              event.preventDefault();
              if (valid && recipient && !pending && !busy)
                setConfirmation({ amount: value, recipient });
            }}
            className="space-y-3 border-t pt-4"
          >
            <label className="block text-sm">
              Số tiền rút
              <input
                type="number"
                required
                min={10000}
                max={maximum}
                step={1}
                value={amount}
                onChange={(event) => {
                  setAmount(event.target.value);
                  setConfirmation(null);
                }}
                placeholder="Nhập số tiền cần rút"
                className="mt-1 w-full rounded-lg border p-2"
              />
            </label>
            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                disabled={busy || maximum < 10000 || !!pending}
                onClick={() => {
                  setAmount(String(maximum));
                  setConfirmation(null);
                }}
                className="rounded-lg border px-3 py-2 text-sm disabled:opacity-50"
              >
                Rút toàn bộ
              </button>
              <button
                disabled={busy || !valid || !!pending}
                className="rounded-lg bg-amber-500 px-4 py-2 text-sm font-bold disabled:opacity-50"
              >
                Tiếp tục rút tiền
              </button>
            </div>
            <p className="text-xs text-neutral-500">
              Mỗi yêu cầu từ 10.000đ đến 100.000.000đ, tối đa bằng số dư khả
              dụng. Hoa hồng 10% đã tính lúc tất toán buổi học; không trừ lại
              khi rút.
            </p>
          </form>
          {confirmation && (
            <div
              role="region"
              aria-label="Xác nhận yêu cầu rút"
              className="space-y-3 rounded-xl border border-amber-200 bg-amber-50 p-4"
            >
              <p className="font-semibold">
                Bạn muốn rút {money(confirmation.amount)}?
              </p>
              <p className="text-sm">
                Nhận vào {confirmation.recipient.bank_name} ·{" "}
                {mask(confirmation.recipient.bank_account)} ·{" "}
                {confirmation.recipient.bank_holder}
              </p>
              <p className="text-sm">
                Phí rút EduFit: 0đ. Số tiền yêu cầu nhận:{" "}
                {money(confirmation.amount)}. Admin duyệt trước khi chuyển;
                trạng thái sẽ cập nhật trong lịch sử.
              </p>
              {recipient.id !== confirmation.recipient.id && (
                <p role="alert" className="text-sm text-red-700">
                  Tài khoản nhận đã đổi. Hãy kiểm tra lại và tạo xác nhận mới.
                </p>
              )}
              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  disabled={
                    busy ||
                    !!pending ||
                    recipient.id !== confirmation.recipient.id ||
                    confirmation.amount > data.wallet.available
                  }
                  onClick={() => void confirm()}
                  className="rounded-lg bg-amber-500 px-4 py-2 text-sm font-bold disabled:opacity-50"
                >
                  Xác nhận rút tiền
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setConfirmation(null)}
                  className="rounded-lg border bg-white px-3 py-2 text-sm"
                >
                  Quay lại
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  );
}
