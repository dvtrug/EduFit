"use client";

import React, { useState } from "react";
import { Bell, Search, LogOut, User as UserIcon, PanelLeft, ChevronDown, ShieldCheck } from "lucide-react";
import { UserRole } from "./Sidebar";

interface TopHeaderProps {
  role?: UserRole;
  user: {
    fullName: string;
    email: string;
    role: string;
  };
  title?: string;
  onLogout: () => void;
  onToggleSidebar?: () => void;
}

const ROLE_LABEL_MAP: Record<string, string> = {
  STUDENT: "Học sinh",
  TUTOR: "Gia sư",
  PARENT: "Phụ huynh",
  ADMIN: "Quản trị viên",
};

export function TopHeader({
  role = "STUDENT",
  user,
  title = "Dashboard",
  onLogout,
  onToggleSidebar,
}: TopHeaderProps) {
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const [unreadNotifications] = useState(2);

  const roleLabel = ROLE_LABEL_MAP[role] || "Thành viên";

  return (
    <header className="h-16 px-6 sm:px-8 bg-white border-b border-stone-200/80 flex items-center justify-between sticky top-0 z-30">
      {/* Topbar Left */}
      <div className="flex items-center gap-3">
        {onToggleSidebar && (
          <button
            type="button"
            onClick={onToggleSidebar}
            className="lg:hidden p-2 rounded-xl hover:bg-stone-100 text-neutral-600 transition-colors"
          >
            <PanelLeft className="size-5" />
          </button>
        )}
        <h1 className="font-heading text-lg font-bold text-neutral-900 tracking-tight">
          {title}
        </h1>
        <span className="hidden sm:inline text-stone-300">/</span>
        <span className="hidden sm:inline text-xs text-neutral-500 font-medium">{roleLabel}</span>
      </div>

      {/* Topbar Right */}
      <div className="flex items-center gap-3 sm:gap-4">
        {/* Search Bar */}
        <div className="hidden md:flex items-center gap-2 px-3 py-1.5 bg-stone-50 border border-stone-200 rounded-xl text-xs text-neutral-400 focus-within:border-amber-400 focus-within:bg-white transition-all w-56">
          <Search className="size-3.5 text-neutral-400" />
          <input
            type="text"
            placeholder="Tìm kiếm nội dung..."
            className="bg-transparent border-none outline-hidden text-neutral-800 placeholder:text-neutral-400 w-full text-xs"
          />
        </div>

        {/* Notifications */}
        <button
          type="button"
          aria-label="Thông báo"
          className="relative p-2 rounded-xl border border-stone-200/60 hover:bg-stone-50 text-neutral-600 transition-colors"
        >
          <Bell className="size-4.5" />
          {unreadNotifications > 0 && (
            <span className="absolute top-1 right-1 size-2 rounded-full bg-amber-500 animate-pulse ring-2 ring-white" />
          )}
        </button>

        {/* Profile Dropdown */}
        <div className="relative">
          <button
            type="button"
            onClick={() => setProfileMenuOpen(!profileMenuOpen)}
            className="flex items-center gap-2.5 p-1.5 pr-2.5 rounded-xl border border-stone-200/60 hover:bg-stone-50 transition-colors group"
          >
            <div className="size-8 rounded-lg bg-gradient-to-tr from-amber-500 to-orange-400 text-white font-bold text-xs flex items-center justify-center shadow-xs">
              {user.fullName ? user.fullName.charAt(0).toUpperCase() : "U"}
            </div>
            <div className="hidden sm:flex flex-col text-left">
              <span className="text-xs font-semibold text-neutral-800 leading-tight">
                {user.fullName || "Tài khoản EduFit"}
              </span>
              <span className="text-[10px] text-neutral-400 leading-tight">{roleLabel}</span>
            </div>
            <ChevronDown className="size-3.5 text-neutral-400 group-hover:text-neutral-700 transition-colors" />
          </button>

          {profileMenuOpen && (
            <>
              <div
                className="fixed inset-0 z-40"
                onClick={() => setProfileMenuOpen(false)}
              />
              <div className="absolute right-0 mt-2 w-56 bg-white border border-stone-200 rounded-2xl shadow-xl p-1.5 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
                <div className="px-3 py-2 border-b border-stone-100 mb-1">
                  <p className="text-xs font-semibold text-neutral-900 truncate">{user.fullName}</p>
                  <p className="text-[10px] text-neutral-400 truncate">{user.email}</p>
                </div>

                <button
                  type="button"
                  onClick={() => setProfileMenuOpen(false)}
                  className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-neutral-600 hover:bg-stone-50 hover:text-neutral-900 transition-colors"
                >
                  <UserIcon className="size-4 text-neutral-400" />
                  Hồ sơ cá nhân
                </button>

                <div className="my-1 border-t border-stone-100" />

                <button
                  type="button"
                  onClick={() => {
                    setProfileMenuOpen(false);
                    onLogout();
                  }}
                  className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs text-rose-600 hover:bg-rose-50 transition-colors font-medium"
                >
                  <LogOut className="size-4 text-rose-500" />
                  Đăng xuất
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
