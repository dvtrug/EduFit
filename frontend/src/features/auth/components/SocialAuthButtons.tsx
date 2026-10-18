"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Loader2, Mail, Sparkles, X } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/shared/ui/Toast";
import { OAuthRoleSelectionModal } from "./OAuthRoleSelectionModal";
import type { OAuthLoginResult } from "../types";

interface SocialAuthButtonsProps {
  label?: string;
}

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: unknown) => void;
          prompt: () => void;
          renderButton: (element: HTMLElement, config: unknown) => void;
        };
      };
    };
  }
}

export function SocialAuthButtons({ label = "Hoặc tiếp tục với" }: SocialAuthButtonsProps) {
  const router = useRouter();
  const { loginWithGoogle } = useAuth();
  const { showToast } = useToast();

  // Google Modal State
  const [isGoogleModalOpen, setIsGoogleModalOpen] = useState(false);
  const [customEmail, setCustomEmail] = useState("");
  const [customName, setCustomName] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // State quản lý Modal chọn vai trò nếu là tài khoản mới
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [onboardingData, setOnboardingData] = useState<{
    onboardingToken: string;
    email?: string;
    fullName?: string;
    avatarUrl?: string;
  } | null>(null);

  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

  // Xử lý chung kết quả đăng nhập Google OAuth
  const handleOAuthSuccess = useCallback((result: OAuthLoginResult, providerName = "Google") => {
    if (result.needsRoleSelection && result.onboardingToken) {
      setOnboardingData({
        onboardingToken: result.onboardingToken,
        email: result.email,
        fullName: result.fullName,
        avatarUrl: result.avatarUrl,
      });
      setIsGoogleModalOpen(false);
      setRoleModalOpen(true);
    } else if (result.user) {
      showToast(`Đăng nhập ${providerName} thành công! Chào ${result.user.fullName}.`, "success");
      setIsGoogleModalOpen(false);
      const role = result.user.role;
      if (role === "ADMIN") router.push("/admin/dashboard");
      else if (role === "TUTOR") router.push("/tutor/dashboard");
      else if (role === "PARENT") router.push("/parent/dashboard");
      else router.push("/student/dashboard");
    }
  }, [router, showToast]);

  // Nạp Google Identity Services Script nếu có Google Client ID
  useEffect(() => {
    if (!googleClientId) return;
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => {
      if (window.google) {
        window.google.accounts.id.initialize({
          client_id: googleClientId,
          auto_select: false,
          callback: async (response: { credential?: string }) => {
            if (response.credential) {
              setLoading(true);
              try {
                if (!loginWithGoogle) {
                  throw new Error("Dịch vụ đăng nhập Google chưa sẵn sàng");
                }
                const res = await loginWithGoogle({ idToken: response.credential });
                handleOAuthSuccess(res, "Google");
              } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : "Đăng nhập Google thất bại";
                showToast(msg, "error");
              } finally {
                setLoading(false);
              }
            }
          },
        });
      }
    };
    document.body.appendChild(script);
    return () => {
      document.body.removeChild(script);
    };
  }, [googleClientId, handleOAuthSuccess, loginWithGoogle, showToast]);

  // Render nút Google chính thức nếu mở modal và có Client ID
  useEffect(() => {
    if (isGoogleModalOpen && googleClientId && window.google) {
      const container = document.getElementById("google-real-button");
      if (container) {
        window.google.accounts.id.renderButton(container, {
          theme: "outline",
          size: "large",
          width: 320,
          text: "continue_with",
          shape: "rectangular",
        });
      }
    }
  }, [isGoogleModalOpen, googleClientId]);

  // Handle Google simulation
  const handleSimulateGoogleLogin = async (email: string, fullName: string, avatarUrl?: string) => {
    setLoading(true);
    setErrorMsg(null);
    try {
      if (!loginWithGoogle) {
        throw new Error("Dịch vụ đăng nhập Google chưa sẵn sàng");
      }
      const numericHash = Math.abs(
        Array.from(email).reduce((acc, char) => (acc * 31 + char.charCodeAt(0)) | 0, 0)
      );
      const res = await loginWithGoogle({
        provider: "GOOGLE",
        email,
        fullName,
        providerUserId: `google-${numericHash}`,
        avatarUrl: avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(email)}`,
      });
      handleOAuthSuccess(res, "Google");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Đăng nhập thất bại. Vui lòng thử lại.";
      setErrorMsg(msg);
      showToast(msg, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleCustomGoogleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customEmail) {
      setErrorMsg("Vui lòng nhập địa chỉ email Google");
      return;
    }
    const name = customName.trim() || customEmail.split("@")[0];
    handleSimulateGoogleLogin(customEmail.trim(), name);
  };

  return (
    <>
      <div className="space-y-4">
        <div className="relative flex items-center justify-center">
          <div className="border-t border-stone-200 w-full" />
          <span className="bg-white px-3 text-xs text-neutral-400 absolute">{label}</span>
        </div>

        <div>
          {/* Nút Đăng nhập Google */}
          <button
            type="button"
            onClick={() => setIsGoogleModalOpen(true)}
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 border border-stone-200 rounded-xl text-xs font-semibold text-neutral-700 hover:bg-stone-50 transition-colors shadow-xs cursor-pointer disabled:opacity-50"
            data-testid="google-auth-button"
          >
            {loading && isGoogleModalOpen ? (
              <Loader2 className="size-4 animate-spin text-amber-500" />
            ) : (
              <svg className="size-4" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
            )}
            Tiếp tục với Google
          </button>
        </div>
      </div>

      {/* Modal đăng nhập Google tiện ích / mô phỏng tài khoản */}
      {isGoogleModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-900/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="relative w-full max-w-md bg-white rounded-3xl p-6 sm:p-7 shadow-2xl border border-stone-100 space-y-5 animate-in zoom-in-95 duration-200">
            <button
              type="button"
              onClick={() => setIsGoogleModalOpen(false)}
              className="absolute right-4 top-4 size-8 flex items-center justify-center rounded-full hover:bg-stone-100 text-neutral-400 hover:text-neutral-700 transition-colors cursor-pointer"
            >
              <X className="size-4" />
            </button>

            <div className="text-center space-y-1.5">
              <div className="inline-flex items-center justify-center size-11 rounded-2xl bg-amber-50 border border-amber-100 text-amber-600 mb-1">
                <Sparkles className="size-5" />
              </div>
              <h3 className="font-heading text-lg font-bold text-neutral-900">
                Đăng nhập bằng tài khoản Google
              </h3>
              <p className="text-xs text-neutral-500">
                Chọn tài khoản mẫu hoặc nhập địa chỉ Google của bạn để trải nghiệm ngay luồng liên kết.
              </p>
            </div>

            {/* Nút bấm Google chính thức nếu đã nạp Client ID */}
            {googleClientId && (
              <div className="flex flex-col items-center justify-center pt-1 pb-2">
                <div id="google-real-button" className="min-h-[44px] flex items-center justify-center" />
                <div className="relative flex items-center justify-center w-full mt-3">
                  <div className="border-t border-stone-200 w-full" />
                  <span className="bg-white px-2 text-[10px] text-neutral-400 absolute">HOẶC DÙNG TÀI KHOẢN MẪU</span>
                </div>
              </div>
            )}

            {errorMsg && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl">
                {errorMsg}
              </div>
            )}

            {/* Danh sách tài khoản Google mẫu nhanh */}
            <div className="space-y-2">
              <span className="text-[11px] font-semibold text-neutral-500 block">
                Tài khoản Google gợi ý thử nghiệm:
              </span>
              <div className="space-y-1.5">
                <button
                  type="button"
                  disabled={loading}
                  onClick={() =>
                    handleSimulateGoogleLogin(
                      "thuha.google@gmail.com",
                      "Nguyễn Thu Hà (Google)",
                      "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120&h=120&fit=crop&crop=face"
                    )
                  }
                  className="w-full flex items-center justify-between p-2.5 rounded-2xl border border-stone-200 hover:border-amber-500 hover:bg-amber-50/40 text-left transition-all cursor-pointer group"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="size-8 rounded-full bg-emerald-100 text-emerald-700 font-bold text-xs flex items-center justify-center">
                      H
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold text-neutral-900 group-hover:text-amber-900 truncate">
                        Nguyễn Thu Hà
                      </p>
                      <p className="text-[10px] text-neutral-400 truncate">thuha.google@gmail.com</p>
                    </div>
                  </div>
                  <span className="text-[10px] font-semibold text-amber-700 bg-amber-100/60 px-2 py-0.5 rounded-md">
                    Tài khoản mới
                  </span>
                </button>

                <button
                  type="button"
                  disabled={loading}
                  onClick={() =>
                    handleSimulateGoogleLogin(
                      "tutor1@edufit.vn",
                      "Thầy Trần Bảo (Gia sư mẫu)",
                      "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&h=120&fit=crop&crop=face"
                    )
                  }
                  className="w-full flex items-center justify-between p-2.5 rounded-2xl border border-stone-200 hover:border-amber-500 hover:bg-amber-50/40 text-left transition-all cursor-pointer group"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="size-8 rounded-full bg-sky-100 text-sky-700 font-bold text-xs flex items-center justify-center">
                      B
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold text-neutral-900 group-hover:text-amber-900 truncate">
                        Trần Bảo (Gia sư)
                      </p>
                      <p className="text-[10px] text-neutral-400 truncate">tutor1@edufit.vn</p>
                    </div>
                  </div>
                  <span className="text-[10px] font-semibold text-sky-700 bg-sky-50 px-2 py-0.5 rounded-md">
                    Đã có sẵn vai trò
                  </span>
                </button>
              </div>
            </div>

            {/* Hoặc nhập email Google tùy ý */}
            <form onSubmit={handleCustomGoogleSubmit} className="pt-2 border-t border-stone-100 space-y-3">
              <span className="text-[11px] font-semibold text-neutral-500 block">
                Hoặc nhập địa chỉ email Google khác:
              </span>
              <div className="space-y-2">
                <div className="relative">
                  <Mail className="size-3.5 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    required
                    value={customEmail}
                    onChange={(e) => setCustomEmail(e.target.value)}
                    placeholder="ví dụ: yourname@gmail.com"
                    className="w-full pl-9 pr-3 py-2 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 transition-all text-neutral-900"
                  />
                </div>
                <input
                  type="text"
                  value={customName}
                  onChange={(e) => setCustomName(e.target.value)}
                  placeholder="Họ và tên hiển thị (tùy chọn)"
                  className="w-full px-3 py-2 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 transition-all text-neutral-900"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 bg-neutral-900 hover:bg-neutral-800 text-white font-bold text-xs rounded-xl shadow-xs transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
              >
                {loading && <Loader2 className="size-3.5 animate-spin" />}
                <span>Tiếp tục với tài khoản này</span>
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Modal chọn vai trò nếu tài khoản Google chưa có vai trò */}
      <OAuthRoleSelectionModal
        isOpen={roleModalOpen}
        onClose={() => setRoleModalOpen(false)}
        onboardingData={onboardingData}
      />
    </>
  );
}
