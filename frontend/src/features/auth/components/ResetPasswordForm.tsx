"use client";

import React from "react";
import Link from "next/link";
import { Lock, ArrowRight, Loader2, CheckCircle2, XCircle } from "lucide-react";
import { AuthCardHeader } from "./AuthCardHeader";
import { useResetPasswordForm } from "../hooks/usePasswordReset";

interface ResetPasswordFormProps {
  token: string;
}

export function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const {
    newPassword,
    confirmPassword,
    setNewPassword,
    setConfirmPassword,
    passwordCriteria,
    fieldErrors,
    generalError,
    isSubmitting,
    success,
    handleSubmit,
  } = useResetPasswordForm(token);

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
          {generalError && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-2xl animate-in fade-in">
              {generalError}
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
            {fieldErrors.newPassword && (
              <p className="text-[11px] text-rose-600 font-medium mt-1 pl-1">
                {fieldErrors.newPassword}
              </p>
            )}
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
            {fieldErrors.confirmPassword && (
              <p className="text-[11px] text-rose-600 font-medium mt-1 pl-1">
                {fieldErrors.confirmPassword}
              </p>
            )}
          </div>

          {/* Tiêu chí mật khẩu trực quan BR-02 */}
          <div className="p-3 bg-stone-50 rounded-2xl border border-stone-200/80 space-y-1.5 text-xs">
            <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block">
              Yêu cầu mật khẩu (BR-02):
            </span>
            <div className="flex items-center gap-2">
              {passwordCriteria.hasMinLength ? (
                <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
              ) : (
                <XCircle className="size-3.5 text-neutral-400 shrink-0" />
              )}
              <span className={passwordCriteria.hasMinLength ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                Tối thiểu 8 ký tự
              </span>
            </div>
            <div className="flex items-center gap-2">
              {passwordCriteria.hasLetter ? (
                <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
              ) : (
                <XCircle className="size-3.5 text-neutral-400 shrink-0" />
              )}
              <span className={passwordCriteria.hasLetter ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                Ít nhất 1 chữ cái (a-z, A-Z)
              </span>
            </div>
            <div className="flex items-center gap-2">
              {passwordCriteria.hasDigit ? (
                <CheckCircle2 className="size-3.5 text-emerald-600 shrink-0" />
              ) : (
                <XCircle className="size-3.5 text-neutral-400 shrink-0" />
              )}
              <span className={passwordCriteria.hasDigit ? "text-emerald-700 font-medium" : "text-neutral-500"}>
                Ít nhất 1 chữ số (0-9)
              </span>
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting || !passwordCriteria.isValid}
            className="w-full py-3 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-2xl shadow-md shadow-amber-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            {isSubmitting && <Loader2 className="size-4 animate-spin" />}
            <span>{isSubmitting ? "Đang cập nhật..." : "Lưu mật khẩu mới"}</span>
          </button>
        </form>
      )}
    </div>
  );
}
