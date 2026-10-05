"use client";

import React, { useState, useEffect, Suspense, useId } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Lock,
  Mail,
  Eye,
  EyeOff,
  AlertCircle,
  ArrowRight,
  ShieldAlert,
  Sparkles,
  CheckCircle2,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/api";

/**
 * Trang Đăng nhập Hệ thống EduFit (UC1.2 - Authenticate Account)
 * 
 * =========================================================================
 * ĐỐI SOÁT CÁC QUY TẮC NGHIỆP VỤ & BẢO MẬT (SRS & THREAT MODEL):
 * =========================================================================
 * 1. BR-06 (Quy tắc Khóa Đăng nhập - Lockout Rule):
 *    - Nếu người dùng nhập sai mật khẩu 5 lần liên tiếp, tài khoản sẽ bị khóa tạm thời 15 phút.
 *    - Backend trả về mã lỗi `ACCOUNT_LOCKED` (HTTP 423 Locked).
 *    - Giao diện bắt đúng mã lỗi này để hiển thị cảnh báo đỏ nổi bật với icon khóa bảo vệ,
 *      thông báo rõ thời gian khóa tạm thời để bảo vệ tài khoản người dùng khỏi tấn công dò mật khẩu (Brute-force).
 * 
 * 2. NFR-06 (Chống Dò quét Tài khoản - Account Enumeration Resistance):
 *    - Khi đăng nhập thất bại do sai email HOẶC sai mật khẩu, Backend trả về cùng một mã lỗi
 *      duy nhất `INVALID_CREDENTIALS` (HTTP 401 Unauthorized).
 *    - Phía Client tuyệt đối không tự ý phân biệt "Email không tồn tại" hay "Sai mật khẩu"
 *      để ngăn kẻ tấn công dò quét danh sách email người dùng trên hệ thống.
 * 
 * 3. Phiên Stateful Session (Cookie Policy):
 *    - Đăng nhập thành công -> Backend thiết lập Cookie `EDUFIT_SESSION` (HttpOnly, SameSite=Lax).
 *    - Client nhận thông tin người dùng và tự động chuyển hướng vào `redirectUrl` hoặc `/dashboard`.
 */
function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, user } = useAuth();

  // Form State
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // UI State
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isAccountLocked, setIsAccountLocked] = useState(false);
  const [registeredSuccessMsg, setRegisteredSuccessMsg] = useState<string | null>(null);

  // Accessible unique IDs for form controls
  const emailId = useId();
  const passwordId = useId();

  // Tự động điền email nếu vừa chuyển hướng từ trang Đăng ký thành công sang
  useEffect(() => {
    const registeredEmail = searchParams.get("registeredEmail");
    if (registeredEmail) {
      setEmail(registeredEmail);
      setRegisteredSuccessMsg("Tài khoản của bạn đã được kích hoạt! Hãy nhập mật khẩu để đăng nhập.");
    }
  }, [searchParams]);

  // Nếu người dùng đã có phiên đăng nhập hợp lệ từ trước -> chuyển thẳng vào Dashboard
  useEffect(() => {
    if (user) {
      router.push(user.redirectUrl || "/dashboard");
    }
  }, [user, router]);

  /**
   * Xử lý gửi thông tin đăng nhập
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setIsAccountLocked(false);
    setRegisteredSuccessMsg(null);

    if (!email.trim() || !password) {
      setErrorMessage("Vui lòng nhập đầy đủ địa chỉ email và mật khẩu.");
      return;
    }

    try {
      setIsSubmitting(true);
      // Gửi yêu cầu đăng nhập: POST /api/v1/auth/login
      const authUser = await login({
        email: email.trim().toLowerCase(),
        password,
      });

      // Đăng nhập thành công -> Chuyển hướng theo vai trò (Student -> /dashboard, Tutor -> /dashboard...)
      router.push(authUser.redirectUrl || "/dashboard");
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === "ACCOUNT_LOCKED") {
          // Bắt lỗi khóa 15 phút do sai 5 lần liên tiếp (BR-06)
          setIsAccountLocked(true);
          setErrorMessage(
            "Tài khoản của bạn tạm thời bị khóa 15 phút do nhập sai mật khẩu quá 5 lần liên tiếp. Vui lòng thử lại sau hoặc đặt lại mật khẩu."
          );
        } else if (err.code === "INVALID_CREDENTIALS") {
          // Chống dò quét tài khoản (NFR-06): Thông báo chung
          setErrorMessage("Email hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại.");
        } else if (err.code === "USER_NOT_ACTIVE") {
          setErrorMessage("Tài khoản này đã bị vô hiệu hóa bởi Quản trị viên.");
        } else {
          setErrorMessage(err.message || "Đăng nhập không thành công. Vui lòng thử lại.");
        }
      } else {
        setErrorMessage("Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="w-full max-w-4xl bg-white rounded-3xl border border-stone-200/80 shadow-xl overflow-hidden grid grid-cols-1 lg:grid-cols-12">
      {/* =================================================================== */}
      {/* CỘT TRÁI: FORM ĐĂNG NHẬP                                           */}
      {/* =================================================================== */}
      <div className="lg:col-span-7 p-8 sm:p-12 flex flex-col justify-between">
        <div>
          {/* Header Logo */}
          <div className="flex items-center gap-2.5 mb-8">
            <Link href="/" className="flex items-center gap-2.5 group">
              <div className="size-9 rounded-xl bg-gradient-to-tr from-sky-500 via-emerald-400 to-orange-400 flex items-center justify-center shadow-sm">
                <span className="text-white font-bold text-lg font-heading">E</span>
              </div>
              <span className="font-heading text-2xl font-bold tracking-tight text-neutral-900 group-hover:text-sky-600 transition-colors">
                EduFit
              </span>
            </Link>
          </div>

          <div className="mb-6">
            <h1 className="font-heading text-3xl font-semibold text-neutral-900 tracking-tight">
              Đăng nhập tài khoản
            </h1>
            <p className="text-sm text-neutral-600 mt-1.5">
              Chào mừng bạn quay trở lại với hành trình học tập.
            </p>
          </div>

          {/* Thông báo vừa đăng ký thành công */}
          {registeredSuccessMsg && (
            <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-start gap-3 text-emerald-800 text-sm">
              <CheckCircle2 className="size-5 shrink-0 mt-0.5 text-emerald-600" />
              <span className="leading-relaxed font-medium">{registeredSuccessMsg}</span>
            </div>
          )}

          {/* Cảnh báo khóa tài khoản 15 phút (BR-06) */}
          {isAccountLocked && (
            <div className="mb-6 p-4 bg-amber-50 border border-amber-300 rounded-2xl flex items-start gap-3 text-amber-900 text-sm">
              <ShieldAlert className="size-5 shrink-0 mt-0.5 text-amber-600" />
              <div className="leading-relaxed">
                <div className="font-semibold mb-0.5">Cảnh báo bảo mật (BR-06)</div>
                {errorMessage}
              </div>
            </div>
          )}

          {/* Lỗi đăng nhập thông thường (NFR-06) */}
          {errorMessage && !isAccountLocked && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start gap-3 text-red-700 text-sm">
              <AlertCircle className="size-5 shrink-0 mt-0.5 text-red-600" />
              <span className="leading-relaxed">{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* 1. ĐỊA CHỈ EMAIL */}
            <div>
              <label htmlFor={emailId} className="block text-xs font-semibold text-neutral-700 mb-1.5">
                Địa chỉ Email
              </label>
              <div className="relative">
                <input
                  id={emailId}
                  type="email"
                  required
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-stone-200 bg-stone-50/50 text-neutral-900 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500/40 focus:border-sky-500 transition-colors pl-10"
                />
                <Mail className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              </div>
            </div>

            {/* 2. MẬT KHẨU */}
            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label htmlFor={passwordId} className="block text-xs font-semibold text-neutral-700">
                  Mật khẩu
                </label>
                <Link
                  href="/forgot-password"
                  className="text-xs text-sky-600 hover:text-sky-700 font-medium transition-colors"
                >
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative">
                <input
                  id={passwordId}
                  type={showPassword ? "text" : "password"}
                  required
                  placeholder="Nhập mật khẩu của bạn"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-stone-200 bg-stone-50/50 text-neutral-900 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500/40 focus:border-sky-500 transition-colors pl-10 pr-10"
                />
                <Lock className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-neutral-400 hover:text-neutral-700 absolute right-3.5 top-1/2 -translate-y-1/2 transition-colors"
                >
                  {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </button>
              </div>
            </div>

            {/* Nút gửi form */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 px-6 rounded-xl bg-sky-600 hover:bg-sky-700 disabled:bg-neutral-300 disabled:cursor-not-allowed text-white text-sm font-semibold shadow-sm hover:shadow transition-all flex items-center justify-center gap-2 active:scale-98 mt-2"
            >
              {isSubmitting ? (
                <>
                  <div className="size-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Đang đăng nhập...</span>
                </>
              ) : (
                <>
                  <span>Vào hệ thống</span>
                  <ArrowRight className="size-4" />
                </>
              )}
            </button>
          </form>
        </div>

        {/* Footer chuyển sang trang đăng ký */}
        <div className="mt-8 pt-6 border-t border-stone-100 text-center text-sm text-neutral-600">
          Chưa có tài khoản EduFit?{" "}
          <Link href="/register" className="font-semibold text-orange-600 hover:text-orange-700 underline underline-offset-4">
            Đăng ký miễn phí ngay
          </Link>
        </div>
      </div>

      {/* =================================================================== */}
      {/* CỘT PHẢI: BANNER THƯƠNG HIỆU & GIỚI THIỆU (FIGMA DESIGN)             */}
      {/* =================================================================== */}
      <div className="hidden lg:col-span-5 bg-gradient-to-br from-sky-50 via-stone-100 to-amber-50 p-10 lg:flex flex-col justify-between border-l border-stone-200/60 relative overflow-hidden">
        <div className="size-56 bg-sky-300/30 rounded-full absolute -top-16 -right-16 blur-2xl pointer-events-none" />
        <div className="size-56 bg-orange-300/30 rounded-full absolute -bottom-16 -left-16 blur-2xl pointer-events-none" />

        <div className="relative">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 bg-white/90 backdrop-blur rounded-full shadow-xs border border-stone-200/60 mb-6">
            <Sparkles className="size-4 text-amber-500" />
            <span className="text-neutral-800 text-xs font-semibold">Nền tảng Học tập THCS</span>
          </div>

          <h2 className="font-heading text-3xl font-medium text-neutral-900 leading-snug">
            Quay lại với
            <br />
            mục tiêu của bạn.
          </h2>
          <p className="text-neutral-700 text-sm mt-3 leading-relaxed">
            Xem lại nhận xét buổi học, các cột mốc đã hoàn thành và lịch dạy-học sắp tới cùng gia sư.
          </p>
        </div>

        {/* Thẻ trích dẫn phụ huynh từ Figma */}
        <div className="relative bg-white/90 backdrop-blur p-6 rounded-2xl border border-stone-200/80 shadow-md flex flex-col gap-3 my-6">
          <div className="text-neutral-800 text-xs italic leading-relaxed">
            &ldquo;Từ khi học cùng EduFit, con tôi có lộ trình rõ ràng từng tuần và điểm số môn Toán cải thiện rõ rệt từ 6.5 lên 8.2.&rdquo;
          </div>
          <div className="flex items-center gap-3 pt-1 border-t border-stone-100">
            <div className="size-8 rounded-full bg-orange-200 flex items-center justify-center text-xs font-bold text-orange-900 font-heading">
              PH
            </div>
            <div>
              <div className="text-xs font-semibold text-neutral-900">Chị Thu Hằng</div>
              <div className="text-[10px] text-neutral-500">Phụ huynh học sinh lớp 9 - Hà Nội</div>
            </div>
          </div>
        </div>

        <div className="relative text-xs text-neutral-500">
          Hệ thống bảo vệ phiên làm việc Stateful an toàn và chống tấn công dò quét tài khoản.
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-orange-200">
      <Suspense fallback={
        <div className="w-full max-w-md p-8 bg-white rounded-3xl border border-stone-200 shadow-md text-center text-sm text-neutral-500">
          Đang tải dữ liệu...
        </div>
      }>
        <LoginForm />
      </Suspense>
    </div>
  );
}
