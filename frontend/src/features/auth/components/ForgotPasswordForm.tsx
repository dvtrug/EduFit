"use client";

import React, { useState } from "react";
import Link from "next/link";
import { authService } from "@/services/auth";
import { AuthCardHeader } from "./AuthCardHeader";
import { Mail, ArrowLeft, Loader2, CheckCircle2 } from "lucide-react";

/**
 * =========================================================================
 * SPRING BOOT API CONNECTION:
 * =========================================================================
 * Endpoint: POST /api/v1/auth/forgot-password
 * Controller: vn.edufit.iam.web.AuthController#forgotPassword
 * Payload: ForgotPasswordPayload { email: String }
 * Contract: docs/authentication-contract.md
 * =========================================================================
 */
export function ForgotPasswordForm() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");
    setLoading(true);

    try {
      await authService.forgotPassword({ email: email.trim() });
      setSubmitted(true);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg("Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white rounded-3xl border border-stone-200/90 shadow-xl shadow-stone-200/50 p-6 sm:p-8 space-y-6">
      <AuthCardHeader
        title="Quên mật khẩu?"
        subtitle="Nhập email của bạn để nhận liên kết đặt lại mật khẩu"
      />

      {submitted ? (
        <div className="space-y-5 text-center py-2 animate-in fade-in">
          <div className="size-14 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center mx-auto shadow-xs">
            <CheckCircle2 className="size-8" />
          </div>
          <div className="space-y-1.5">
            <h4 className="font-heading text-base font-bold text-neutral-900">
              Kiểm tra hộp thư đến của bạn
            </h4>
            <p className="text-xs text-neutral-500 leading-relaxed">
              Chúng tôi đã gửi hướng dẫn đặt lại mật khẩu đến địa chỉ email:{" "}
              <strong className="text-neutral-800">{email}</strong>. Liên kết có hiệu lực trong vòng 30 phút.
            </p>
          </div>
          <Link
            href="/login"
            className="inline-flex items-center justify-center gap-2 text-xs font-bold text-amber-600 hover:text-amber-700 hover:underline pt-2"
          >
            <ArrowLeft className="size-3.5" />
            <span>Quay lại Đăng nhập</span>
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          {errorMsg && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-2xl animate-in fade-in">
              {errorMsg}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1.5">
              Địa chỉ Email đã đăng ký
            </label>
            <div className="relative">
              <Mail className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@edufit.vn"
                className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all text-neutral-900 placeholder:text-neutral-400"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-2xl shadow-md shadow-amber-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            {loading && <Loader2 className="size-4 animate-spin" />}
            <span>{loading ? "Đang gửi yêu cầu..." : "Gửi liên kết khôi phục"}</span>
          </button>

          <div className="text-center pt-2">
            <Link
              href="/login"
              className="inline-flex items-center justify-center gap-1.5 text-xs text-neutral-500 hover:text-neutral-800 font-medium transition-colors"
            >
              <ArrowLeft className="size-3.5" />
              <span>Quay lại trang Đăng nhập</span>
            </Link>
          </div>
        </form>
      )}
    </div>
  );
}
