"use client";

import React, { useState, useId } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  GraduationCap,
  Users,
  BookOpen,
  Eye,
  EyeOff,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ArrowRight,
  ArrowLeft,
  ShieldCheck,
  Check,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/api";

type RoleOption = "STUDENT" | "PARENT" | "TUTOR";

/**
 * Trang Đăng ký Tài khoản EduFit (UC1.1 - Self Registration)
 * 
 * =========================================================================
 * ĐỐI SOÁT CÁC QUY TẮC NGHIỆP VỤ (BUSINESS RULES TRACEABILITY):
 * =========================================================================
 * 1. BR-01 (Chuẩn hóa Email):
 *    - Dữ liệu email được người dùng nhập vào sẽ được cắt khoảng trắng (trim)
 *      và chuyển về chữ thường ở cả client lẫn server để đảm bảo tính duy nhất.
 * 
 * 2. BR-02 (Chính sách Mật khẩu - Password Policy):
 *    - Mật khẩu phải có độ dài tối thiểu 8 ký tự.
 *    - Phải chứa ít nhất 1 chữ cái (a-z, A-Z) và ít nhất 1 chữ số (0-9).
 *    - Giao diện có thanh đo độ mạnh (Password Strength Bar) và các tiêu chí
 *      được kiểm tra theo thời gian thực (realtime) giúp người dùng dễ dàng đáp ứng.
 * 
 * 3. BR-03 (Giới hạn vai trò đăng ký công khai):
 *    - Chỉ cho phép người dùng tự đăng ký với 1 trong 3 vai trò:
 *      + STUDENT: Học sinh (tìm gia sư, theo dõi tiến độ học tập).
 *      + PARENT: Phụ huynh (quản lý học tập của con, nhận báo cáo).
 *      + TUTOR: Gia sư (đăng ký hồ sơ, nhận lớp, lên lịch dạy).
 *    - Tuyệt đối KHÔNG có tùy chọn ADMIN trên giao diện đăng ký công khai.
 * 
 * 4. BR-04 (Trạng thái kích hoạt trực tiếp):
 *    - Tài khoản mới tạo được hệ thống kích hoạt ngay ở trạng thái ACTIVE (theo
 *      quyết định mới của nhóm và migration V2), sẵn sàng đăng nhập tức thì.
 * 
 * 5. NFR-06 (Phản hồi lỗi an toàn & rõ ràng):
 *    - Bắt mã lỗi `USER_ALREADY_EXISTS` (HTTP 409) để hiển thị thông báo thân thiện:
 *      "Email này đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác."
 */
export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();

  // Form State
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<RoleOption>("STUDENT");

  // UI Interactive State
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Accessible unique IDs for form controls
  const fullNameId = useId();
  const emailId = useId();
  const passwordId = useId();

  // =========================================================================
  // KIỂM TRA ĐỘ MẠNH MẬT KHẨU REALTIME (BR-02)
  // =========================================================================
  const hasMinLength = password.length >= 8;
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const isPasswordValid = hasMinLength && hasLetter && hasNumber;

  /**
   * Xử lý gửi biểu mẫu đăng ký
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    // 1. Kiểm tra họ tên
    if (!fullName.trim()) {
      setErrorMessage("Vui lòng nhập họ và tên của bạn.");
      return;
    }

    // 2. Kiểm tra chính sách mật khẩu (BR-02)
    if (!isPasswordValid) {
      setErrorMessage("Mật khẩu chưa đáp ứng chính sách bảo mật (tối thiểu 8 ký tự, gồm cả chữ và số).");
      return;
    }

    try {
      setIsSubmitting(true);
      // Gửi yêu cầu đăng ký tới API Backend: POST /api/v1/auth/register
      await register({
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        password,
        role,
      });

      // Đăng ký thành công (BR-04: Tài khoản ACTIVE)
      setSuccessMessage("Đăng ký tài khoản thành công! Đang chuyển hướng đến trang đăng nhập...");
      setTimeout(() => {
        router.push(`/login?registeredEmail=${encodeURIComponent(email.trim().toLowerCase())}`);
      }, 1500);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === "USER_ALREADY_EXISTS") {
          setErrorMessage("Email này đã được đăng ký trong hệ thống. Vui lòng đăng nhập hoặc sử dụng email khác.");
        } else if (err.code === "VALIDATION_FAILED") {
          setErrorMessage(err.message || "Dữ liệu nhập vào chưa hợp lệ. Vui lòng kiểm tra lại.");
        } else {
          setErrorMessage(err.message || "Đăng ký không thành công. Vui lòng thử lại.");
        }
      } else {
        setErrorMessage("Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans selection:bg-orange-200">
      <div className="w-full max-w-5xl bg-white rounded-3xl border border-stone-200/80 shadow-xl overflow-hidden grid grid-cols-1 lg:grid-cols-12 animate-fade-in-up">
        
        {/* =================================================================== */}
        {/* CỘT TRÁI: FORM ĐĂNG KÝ                                             */}
        {/* =================================================================== */}
        <div className="lg:col-span-7 p-8 sm:p-12 flex flex-col justify-between">
          <div>
            {/* Header: Logo & Quay lại */}
            <div className="flex items-center justify-between mb-8">
              <Link href="/" className="flex items-center gap-2.5 group">
                <div className="size-9 rounded-xl bg-gradient-to-tr from-sky-500 via-emerald-400 to-orange-400 flex items-center justify-center shadow-sm">
                  <span className="text-white font-bold text-lg font-heading">E</span>
                </div>
                <span className="font-heading text-2xl font-bold tracking-tight text-neutral-900 group-hover:text-sky-600 transition-colors">
                  EduFit
                </span>
              </Link>

              <Link
                href="/"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-neutral-900 transition-colors group px-3 py-1.5 rounded-full hover:bg-stone-100"
              >
                <ArrowLeft className="size-3.5 transition-transform group-hover:-translate-x-1" />
                <span>Quay lại trang chủ</span>
              </Link>
            </div>

            <div className="mb-6">
              <h1 className="font-heading text-3xl font-semibold text-neutral-900 tracking-tight">
                Tạo tài khoản mới
              </h1>
              <p className="text-sm text-neutral-600 mt-1.5">
                Bắt đầu lộ trình học tập có mục tiêu và tiến bộ rõ ràng.
              </p>
            </div>

            {/* Thông báo lỗi nghiệp vụ */}
            {errorMessage && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start gap-3 text-red-700 text-sm animate-shake">
                <AlertCircle className="size-5 shrink-0 mt-0.5 text-red-600" />
                <span className="leading-relaxed">{errorMessage}</span>
              </div>
            )}

            {/* Thông báo thành công */}
            {successMessage && (
              <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-start gap-3 text-emerald-800 text-sm">
                <CheckCircle2 className="size-5 shrink-0 mt-0.5 text-emerald-600" />
                <span className="leading-relaxed font-medium">{successMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              {/* 1. CHỌN VAI TRÒ (BR-03: STUDENT / PARENT / TUTOR) */}
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-2 uppercase tracking-wider">
                  Bạn tham gia EduFit với vai trò nào?
                </label>
                <div className="grid grid-cols-3 gap-2.5">
                  {/* Option: Học sinh */}
                  <button
                    type="button"
                    onClick={() => setRole("STUDENT")}
                    className={`flex flex-col items-center justify-center gap-1.5 p-3 rounded-2xl border text-center transition-all duration-200 hover:scale-[1.03] active:scale-[0.98] ${
                      role === "STUDENT"
                        ? "border-sky-600 bg-sky-50/70 text-sky-800 shadow-xs ring-1 ring-sky-500/30"
                        : "border-stone-200 bg-stone-50/50 hover:bg-stone-100/70 text-neutral-700"
                    }`}
                  >
                    <BookOpen className={`size-5 ${role === "STUDENT" ? "text-sky-600" : "text-neutral-500"}`} />
                    <span className="text-xs font-semibold">Học sinh</span>
                  </button>

                  {/* Option: Phụ huynh */}
                  <button
                    type="button"
                    onClick={() => setRole("PARENT")}
                    className={`flex flex-col items-center justify-center gap-1.5 p-3 rounded-2xl border text-center transition-all duration-200 hover:scale-[1.03] active:scale-[0.98] ${
                      role === "PARENT"
                        ? "border-sky-600 bg-sky-50/70 text-sky-800 shadow-xs ring-1 ring-sky-500/30"
                        : "border-stone-200 bg-stone-50/50 hover:bg-stone-100/70 text-neutral-700"
                    }`}
                  >
                    <Users className={`size-5 ${role === "PARENT" ? "text-sky-600" : "text-neutral-500"}`} />
                    <span className="text-xs font-semibold">Phụ huynh</span>
                  </button>

                  {/* Option: Gia sư */}
                  <button
                    type="button"
                    onClick={() => setRole("TUTOR")}
                    className={`flex flex-col items-center justify-center gap-1.5 p-3 rounded-2xl border text-center transition-all duration-200 hover:scale-[1.03] active:scale-[0.98] ${
                      role === "TUTOR"
                        ? "border-sky-600 bg-sky-50/70 text-sky-800 shadow-xs ring-1 ring-sky-500/30"
                        : "border-stone-200 bg-stone-50/50 hover:bg-stone-100/70 text-neutral-700"
                    }`}
                  >
                    <GraduationCap className={`size-5 ${role === "TUTOR" ? "text-sky-600" : "text-neutral-500"}`} />
                    <span className="text-xs font-semibold">Gia sư</span>
                  </button>
                </div>
              </div>

              {/* 2. HỌ VÀ TÊN */}
              <div>
                <label htmlFor={fullNameId} className="block text-xs font-semibold text-neutral-700 mb-1.5">
                  Họ và tên <span className="text-red-500">*</span>
                </label>
                <input
                  id={fullNameId}
                  type="text"
                  required
                  placeholder="Ví dụ: Nguyễn Văn An"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-stone-200 bg-stone-50/50 text-neutral-900 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500/40 focus:border-sky-500 transition-colors"
                />
              </div>

              {/* 3. EMAIL (BR-01) */}
              <div>
                <label htmlFor={emailId} className="block text-xs font-semibold text-neutral-700 mb-1.5">
                  Địa chỉ Email <span className="text-red-500">*</span>
                </label>
                <input
                  id={emailId}
                  type="email"
                  required
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-stone-200 bg-stone-50/50 text-neutral-900 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500/40 focus:border-sky-500 transition-colors"
                />
              </div>

              {/* 4. MẬT KHẨU (BR-02) */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label htmlFor={passwordId} className="block text-xs font-semibold text-neutral-700">
                    Mật khẩu <span className="text-red-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="text-xs text-neutral-500 hover:text-neutral-800 flex items-center gap-1 transition-colors"
                  >
                    {showPassword ? (
                      <>
                        <EyeOff className="size-3.5" /> Ẩn
                      </>
                    ) : (
                      <>
                        <Eye className="size-3.5" /> Hiện
                      </>
                    )}
                  </button>
                </div>
                <div className="relative">
                  <input
                    id={passwordId}
                    type={showPassword ? "text" : "password"}
                    required
                    placeholder="Tối thiểu 8 ký tự"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-stone-200 bg-stone-50/50 text-neutral-900 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500/40 focus:border-sky-500 transition-colors pr-10"
                  />
                </div>

                {/* Tiêu chí mật khẩu trực quan theo BR-02 */}
                {password.length > 0 && (
                  <div className="mt-2.5 p-3 bg-stone-50 rounded-xl border border-stone-200/60 space-y-1.5">
                    <div className="text-[11px] font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                      Yêu cầu mật khẩu (BR-02):
                    </div>
                    <div className="flex items-center gap-2 text-xs">
                      {hasMinLength ? (
                        <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
                      ) : (
                        <XCircle className="size-3.5 text-neutral-400 shrink-0" />
                      )}
                      <span className={hasMinLength ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                        Tối thiểu 8 ký tự ({password.length}/8)
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-xs">
                      {hasLetter ? (
                        <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
                      ) : (
                        <XCircle className="size-3.5 text-neutral-400 shrink-0" />
                      )}
                      <span className={hasLetter ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                        Chứa ít nhất 1 chữ cái (a-z, A-Z)
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-xs">
                      {hasNumber ? (
                        <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
                      ) : (
                        <XCircle className="size-3.5 text-neutral-400 shrink-0" />
                      )}
                      <span className={hasNumber ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                        Chứa ít nhất 1 chữ số (0-9)
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* Nút gửi form */}
              <button
                type="submit"
                disabled={isSubmitting || !isPasswordValid}
                className="w-full py-3.5 px-6 rounded-xl bg-orange-700 hover:bg-orange-800 disabled:bg-neutral-300 disabled:cursor-not-allowed text-white text-sm font-semibold shadow-sm hover:shadow transition-all flex items-center justify-center gap-2 btn-interactive"
              >
                {isSubmitting ? (
                  <>
                    <div className="size-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>Đang xử lý...</span>
                  </>
                ) : (
                  <>
                    <span>Đăng ký ngay</span>
                    <ArrowRight className="size-4" />
                  </>
                )}
              </button>
            </form>
          </div>

          {/* Footer chuyển sang trang đăng nhập */}
          <div className="mt-8 pt-6 border-t border-stone-100 text-center text-sm text-neutral-600">
            Đã có tài khoản EduFit?{" "}
            <Link href="/login" className="font-semibold text-sky-600 hover:text-sky-700 underline underline-offset-4">
              Đăng nhập tại đây
            </Link>
          </div>
        </div>

        {/* =================================================================== */}
        {/* CỘT PHẢI: BANNER MINH HỌA GIÁ TRỊ (FIGMA CODE & BRANDING)            */}
        {/* =================================================================== */}
        <div className="hidden lg:col-span-5 bg-gradient-to-br from-amber-50 via-orange-50 to-sky-50 p-10 lg:flex flex-col justify-between border-l border-stone-200/60 relative overflow-hidden">
          {/* Họa tiết trang trí mờ */}
          <div className="size-56 bg-amber-300/30 rounded-full absolute -top-16 -right-16 blur-2xl pointer-events-none" />
          <div className="size-56 bg-sky-300/30 rounded-full absolute -bottom-16 -left-16 blur-2xl pointer-events-none" />

          {/* Huy hiệu cam kết */}
          <div className="relative">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 bg-white/90 backdrop-blur rounded-full shadow-xs border border-stone-200/60 mb-6">
              <ShieldCheck className="size-4 text-emerald-600" />
              <span className="text-neutral-800 text-xs font-semibold">Bảo đảm chất lượng gia sư</span>
            </div>

            <h2 className="font-heading text-3xl font-medium text-neutral-900 leading-snug">
              Học đúng cách.
              <br />
              Tiến bộ rõ ràng.
            </h2>
            <p className="text-neutral-700 text-sm mt-3 leading-relaxed">
              EduFit đồng hành cùng học sinh THCS và phụ huynh trong việc xây dựng lộ trình học 1-1 minh bạch và hiệu quả.
            </p>
          </div>

          {/* Thẻ minh họa từ Figma Code */}
          <div className="relative bg-white/90 backdrop-blur p-6 rounded-2xl border border-stone-200/80 shadow-md flex flex-col gap-3 my-6">
            <div className="flex justify-between items-center text-xs">
              <span className="font-semibold text-neutral-900">Mục tiêu mẫu: Toán 9</span>
              <span className="px-2 py-0.5 bg-amber-400/30 text-amber-900 rounded-full font-bold text-[10px]">
                Ôn thi vào 10
              </span>
            </div>
            <div className="w-full h-2 bg-stone-100 rounded-full overflow-hidden">
              <div className="h-full bg-emerald-500 rounded-full" style={{ width: "75%" }} />
            </div>
            <div className="flex items-center justify-between text-xs text-neutral-500">
              <span>Đã hoàn thành 9 / 12 buổi</span>
              <span className="font-semibold text-neutral-800">75%</span>
            </div>
          </div>

          {/* Lợi ích ngắn */}
          <div className="relative space-y-3">
            <div className="flex items-center gap-2.5 text-xs font-medium text-neutral-700">
              <div className="size-5 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
                <Check className="size-3" />
              </div>
              <span>100% Gia sư có bằng cấp và CCCD được xác minh</span>
            </div>
            <div className="flex items-center gap-2.5 text-xs font-medium text-neutral-700">
              <div className="size-5 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
                <Check className="size-3" />
              </div>
              <span>Khớp gia sư theo đúng mục tiêu, lịch rảnh và vị trí</span>
            </div>
            <div className="flex items-center gap-2.5 text-xs font-medium text-neutral-700">
              <div className="size-5 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
                <Check className="size-3" />
              </div>
              <span>Báo cáo tiến bộ chi tiết sau từng buổi học</span>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
