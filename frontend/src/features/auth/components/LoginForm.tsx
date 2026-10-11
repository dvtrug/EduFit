"use client";

import React from "react";
import Link from "next/link";
import { Lock, Mail, Loader2 } from "lucide-react";
import { AuthCardHeader } from "./AuthCardHeader";
import { QuickLoginPills } from "./QuickLoginPills";
import { SocialAuthButtons } from "./SocialAuthButtons";
import { useLoginForm } from "../hooks/useLoginForm";

export function LoginForm() {
  const {
    email,
    password,
    setEmail,
    setPassword,
    fieldErrors,
    generalError,
    isSubmitting,
    fillQuickLogin,
    handleSubmit,
  } = useLoginForm();

  return (
    <div className="w-full max-w-md bg-white rounded-3xl border border-stone-200/90 shadow-xl shadow-stone-200/50 p-6 sm:p-8 space-y-6">
      <AuthCardHeader
        title="Chào mừng trở lại!"
        subtitle="Đăng nhập để tiếp tục hành trình học tập cùng EduFit"
      />

      {/* Demo 1-Click login pills */}
      <QuickLoginPills onSelect={fillQuickLogin} />

      {generalError && (
        <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-2xl animate-in fade-in">
          {generalError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="loginEmail" className="block text-xs font-semibold text-neutral-700 mb-1.5">
            Email tài khoản
          </label>
          <div className="relative">
            <Mail className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              id="loginEmail"
              name="email"
              type="email"
              required
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@edufit.vn"
              className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all text-neutral-900 placeholder:text-neutral-400"
            />
          </div>
          {fieldErrors.email && (
            <p className="text-[11px] text-rose-600 font-medium mt-1 pl-1">
              {fieldErrors.email}
            </p>
          )}
        </div>

        <div>
          <div className="flex justify-between items-center mb-1.5">
            <label htmlFor="loginPassword" className="text-xs font-semibold text-neutral-700">
              Mật khẩu
            </label>
            <Link href="/forgot-password" className="text-xs text-amber-600 hover:underline font-medium">
              Quên mật khẩu?
            </Link>
          </div>
          <div className="relative">
            <Lock className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              id="loginPassword"
              name="password"
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all text-neutral-900 placeholder:text-neutral-400"
            />
          </div>
          {fieldErrors.password && (
            <p className="text-[11px] text-rose-600 font-medium mt-1 pl-1">
              {fieldErrors.password}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full py-3 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-2xl shadow-md shadow-amber-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
        >
          {isSubmitting && <Loader2 className="size-4 animate-spin" />}
          <span>{isSubmitting ? "Đang đăng nhập..." : "Đăng nhập ngay"}</span>
        </button>
      </form>

      <SocialAuthButtons />

      <p className="text-center text-xs text-neutral-500 pt-1">
        Chưa có tài khoản EduFit?{" "}
        <Link href="/register" className="text-amber-600 font-bold hover:underline">
          Đăng ký miễn phí
        </Link>
      </p>
    </div>
  );
}
