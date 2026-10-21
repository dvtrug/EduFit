import { fetchApi } from "@/lib/api";
export interface Wallet {
  available: number;
  held: number;
}
export interface Entry {
  id: string;
  kind: string;
  available_delta: number;
  held_delta: number;
  created_at: string;
  reference_id: string;
}
export interface Topup {
  id: string;
  user_id: string;
  order_code: number;
  amount: number;
  provider: "MANUAL" | "PAYOS";
  status: "PENDING" | "PAID" | "CANCELLED";
  checkout_url?: string;
  created_at: string;
}
export interface Withdrawal {
  id: string;
  user_id: string;
  amount: number;
  status: "PENDING" | "PAID" | "REJECTED";
  bank_name: string;
  bank_account: string;
  bank_holder: string;
  created_at: string;
}
export interface Overview {
  wallet: Wallet;
  entries: Entry[];
  topups: Topup[];
  withdrawals: Withdrawal[];
  topupEnabled: boolean;
  topupProvider: string;
}
export interface PaymentSession {
  sessionId: string;
  classId: string;
  studentId: string;
  tutorName: string;
  sessionStatus: string;
  startAt: string;
  endAt: string;
  amount: number;
  commissionBps: number;
  payerId: string | null;
  status:
    | "UNFUNDED"
    | "FUNDED"
    | "AWAITING_CONFIRMATION"
    | "DISPUTED"
    | "AWAITING_ADMIN"
    | "SETTLED"
    | "REFUNDED"
    | "EXPIRED";
  learnerAccepted: boolean;
  tutorAccepted: boolean;
  autoAccepted: boolean;
  disputeDeadline: string | null;
  disputeReason: string | null;
  learnerAmount: number | null;
  tutorAmount: number | null;
  learnerReport: string | null;
  tutorReport: string | null;
  settledAmount: number;
  canFund: boolean;
  canActAsLearner: boolean;
  canActAsTutor: boolean;
}
export interface Checkout {
  id: string;
  provider: string;
  checkoutUrl?: string;
  qrUrl?: string;
  bankAccount?: string;
  bankHolder?: string;
  description?: string;
  amount?: number;
}
export interface AdminOverview {
  topups: Topup[];
  withdrawals: Withdrawal[];
}
const post = <T>(path: string, body?: unknown) =>
  fetchApi<T>(path, {
    method: "POST",
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
const base = "/api/v1/payment";
export const paymentApi = {
  wallet: (offset = 0) => fetchApi<Overview>(`${base}/wallet?offset=${offset}`),
  sessions: (from: string, to: string) =>
    fetchApi<PaymentSession[]>(
      `${base}/sessions?${new URLSearchParams({ from, to })}`,
    ),
  session: (id: string) =>
    fetchApi<PaymentSession>(`${base}/sessions/${encodeURIComponent(id)}`),
  topup: (amount: number) => post<Checkout>(`${base}/topups`, { amount }),
  fund: (id: string, amount: number) =>
    post<PaymentSession>(`${base}/sessions/${encodeURIComponent(id)}/fund`, {
      amount,
    }),
  confirm: (id: string) =>
    post<PaymentSession>(`${base}/sessions/${encodeURIComponent(id)}/confirm`),
  dispute: (id: string, text: string) =>
    post<PaymentSession>(`${base}/sessions/${encodeURIComponent(id)}/dispute`, {
      text,
    }),
  report: (id: string, amount: number, text: string) =>
    post<PaymentSession>(`${base}/sessions/${encodeURIComponent(id)}/report`, {
      amount,
      text,
    }),
  withdraw: (amount: number, bank: string, account: string, holder: string) =>
    post<Withdrawal>(`${base}/withdrawals`, { amount, bank, account, holder }),
  admin: (offset = 0) =>
    fetchApi<AdminOverview>(`/api/v1/admin/payment?offset=${offset}`),
  disputes: (offset = 0) =>
    fetchApi<PaymentSession[]>(
      `/api/v1/admin/payment/disputes?offset=${offset}`,
    ),
  review: (
    kind: "topups" | "withdrawals" | "disputes",
    id: string,
    approve: boolean,
    note: string,
    reference: string,
  ) =>
    post<unknown>(
      `/api/v1/admin/payment/${kind}/${encodeURIComponent(id)}/${
        kind === "topups" ? "confirm" : "review"
      }`,
      { approve, note, reference },
    ),
};
export const money = (amount: number) => `${amount.toLocaleString("vi-VN")}đ`;
export const paymentError = (error: unknown) =>
  error instanceof Error
    ? error.message
    : "Không xử lý được yêu cầu. Vui lòng thử lại.";
export const chargeLabels: Record<PaymentSession["status"], string> = {
  UNFUNDED: "Chưa cấp tiền",
  FUNDED: "Đã giữ học phí",
  AWAITING_CONFIRMATION: "Chờ xác nhận",
  DISPUTED: "Đang tranh chấp",
  AWAITING_ADMIN: "Chờ Admin tất toán",
  SETTLED: "Đã tất toán",
  REFUNDED: "Đã hoàn về ví",
  EXPIRED: "Hết hạn tranh chấp · đã hoàn ví",
};
export const entryLabels: Record<string, string> = {
  TOPUP: "Nạp tiền",
  SESSION_RESERVED: "Giữ học phí",
  SESSION_SETTLED: "Tất toán / giải phóng học phí",
  TUITION_INCOME: "Thu nhập buổi dạy",
  WITHDRAW_RESERVED: "Giữ tiền rút",
  WITHDRAW_PAID: "Đã chuyển ngân hàng",
  WITHDRAW_REJECTED: "Hoàn yêu cầu rút",
};
