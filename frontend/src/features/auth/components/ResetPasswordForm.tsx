"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { AuthCardHeader } from "./AuthCardHeader";
import { Lock, ArrowRight, Loader2, CheckCircle2, XCircle } from "lucide-react";

/**
 * =========================================================================
 * SPRING BOOT API CONNECTION:
 * =========================================================================
 * Endpoint: POST /api/v1/auth/reset-password
 * Controller: vn.edufit.iam.web.AuthController#resetPassword
 * Payload: ResetPasswordPayload { token: String, newPassword: String }
 * Business Rule: BR-02 - Mật khẩu tối thiểu 8 ký tự, ít nhất 1 chữ cái và 1 chữ số
 * =========================================================================
 */

interface ResetPasswordFormProps {
  token: string;
}

export function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const router = useRouter();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  // Kiểm tra tiêu chí mật khẩu (BR-02)
  const hasMinLength = newPassword.length >= 8;
  const hasLetter = /[a-zA-Z]/.test(newPassword);
  const hasDigit = /\d/.test(newPassword);
  const isPasswordValid = hasMinLength && hasLetter && hasDigit;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");

    if (!isPasswordValid) {
      setErrorMsg("Mật khẩu chưa đáp ứng đầy đủ yêu cầu bảo mật (BR-02)!");
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMsg("Mật khẩu xác nhận không khớp!");
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword({
        token,
        newPassword,
      });
      setSuccess(true);
      setTimeout(() => {
        router.push("/login");
      }, 3000);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg("Mã token không hợp lệ hoặc đã hết hạn (30 phút). Vui lòng yêu cầu lại.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white rounded-3xl border border-stone-200/90 shadow-xl shadow-stone-200/50 p-6 sm:p-8 space-y-6">
      <AuthCardHeader
        title="Thiết lập mật khẩu mới"
        subtitle="Nhập mật khẩu mới an toàn cho tài khoản của bạn"
      />

      {success ? (
        <div className="space-y-4 text-center py-3 animate-in fade-in">
          <div className="size-14 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center mx-auto shadow-xs">
            <CheckCircle2 className="size-8" />
          </div>
          <div className="space-y-1.5">
            <h4 className="font-heading text-base font-bold text-neutral-900">
              Đặt lại mật khẩu thành công!
            </h4>
            <p className="text-xs text-neutral-500">
              Hệ thống sẽ tự động chuyển hướng về trang Đăng nhập trong 3 giây...
            </p>
          </div>
          <Link
            href="/login"
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-amber-500 text-white font-bold text-xs rounded-xl shadow-xs hover:bg-amber-600 transition-colors"
          >
            <span>Đăng nhập ngay</span>
            <ArrowRight className="size-3.5" />
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
              Mật khẩu mới
            </label>
            <div className="relative">
              <Lock className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Tối thiểu 8 ký tự"
                className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all text-neutral-900"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1.5">
              Xác nhận mật khẩu mới
            </label>
            <div className="relative">
              <Lock className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Nhập lại mật khẩu"
                className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all text-neutral-900"
              />
            </div>
          </div>

          {/* Tiêu chí mật khẩu trực quan BR-02 */}
          <div className="p-3 bg-stone-50 rounded-2xl border border-stone-200/80 space-y-1.5 text-xs">
            <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block">
              Yêu cầu mật khẩu (BR-02):
            </span>
            <div className="flex items-center gap-2">
              {hasMinLength ? (
                <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
              ) : (
                <XCircle className="size-3.5 text-neutral-400 shrink-0" />
              )}
              <span className={hasMinLength ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                Tối thiểu 8 ký tự
              </span>
            </div>
            <div className="flex items-center gap-2">
              {hasLetter ? (
                <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
              ) : (
                <XCircle className="size-3.5 text-neutral-400 shrink-0" />
              )}
              <span className={hasLetter ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                Ít nhất 1 chữ cái (a-z, A-Z)
              </span>
            </div>
            <div className="flex items-center gap-2">
              {hasDigit ? (
                <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
              ) : (
                <XCircle className="size-3.5 text-neutral-400 shrink-0" />
              )}
              <span className={hasDigit ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                Ít nhất 1 chữ số (0-9)
              </span>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || !isPasswordValid}
            className="w-full py-3 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-2xl shadow-md shadow-amber-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            {loading && <Loader2 className="size-4 animate-spin" />}
            <span>{loading ? "Đang cập nhật..." : "Lưu mật khẩu mới"}</span>
          </button>
        </form>
      )}
    </div>
  );
}
