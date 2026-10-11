"use client";

import React from "react";
import Link from "next/link";
import { EduFitLogo } from "@/shared/ui/EduFitLogo";
import {
  LayoutDashboard,
  Search,
  Sparkles,
  UserCheck,
  BookOpen,
  Calendar,
  TrendingUp,
  CreditCard,
  PanelLeftClose,
  PanelLeft,
  GraduationCap,
  Users,
  ShieldCheck,
  AlertTriangle,
  Settings,
  DollarSign,
  FileCheck2,
  HeartHandshake,
  BarChart3,
  LucideIcon,
} from "lucide-react";

export type UserRole = "STUDENT" | "TUTOR" | "PARENT" | "ADMIN";

interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  badge?: string | null;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const ROLE_NAV_CONFIG: Record<UserRole, { roleTitle: string; sections: NavSection[] }> = {
  STUDENT: {
    roleTitle: "Cổng Học Sinh",
    sections: [
      {
        title: "TỔNG QUAN",
        items: [
          { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
          { label: "Tiến độ học tập", href: "/dashboard/progress", icon: TrendingUp },
        ],
      },
      {
        title: "TÌM KIẾM",
        items: [
          { label: "Tìm gia sư", href: "/dashboard/tutors", icon: Search },
          { label: "Gợi ý AI Match", href: "/dashboard/tutors?tab=matches", icon: Sparkles, badge: "AI" },
          { label: "Yêu cầu kết nối", href: "/dashboard/tutors?tab=requests", icon: UserCheck },
        ],
      },
      {
        title: "HỌC TẬP",
        items: [
          { label: "Lớp của tôi", href: "/dashboard/classes", icon: BookOpen },
          { label: "Lịch học tuần", href: "/dashboard/schedule", icon: Calendar },
        ],
      },
      {
        title: "TÀI CHÍNH",
        items: [
          { label: "Học phí & Thanh toán", href: "/dashboard/payments", icon: CreditCard },
        ],
      },
      {
        title: "CÀI ĐẶT",
        items: [
          { label: "Hồ sơ học tập", href: "/dashboard/profile", icon: Settings },
        ],
      },
    ],
  },
  TUTOR: {
    roleTitle: "Cổng Gia Sư",
    sections: [
      {
        title: "TỔNG QUAN",
        items: [
          { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
          { label: "Xác minh bằng cấp", href: "/dashboard/verification", icon: FileCheck2, badge: "KYC" },
        ],
      },
      {
        title: "GIẢNG DẠY",
        items: [
          { label: "Lịch dạy", href: "/dashboard/schedule", icon: Calendar },
          { label: "Lớp học 1-1", href: "/dashboard/classes", icon: BookOpen },
          { label: "Yêu cầu học viên", href: "/dashboard/requests", icon: UserCheck },
        ],
      },
      {
        title: "THU NHẬP",
        items: [
          { label: "Báo cáo thu nhập", href: "/dashboard/earnings", icon: DollarSign },
        ],
      },
      {
        title: "CÀI ĐẶT",
        items: [
          { label: "Hồ sơ & Môn dạy", href: "/dashboard/profile", icon: Settings },
        ],
      },
    ],
  },
  PARENT: {
    roleTitle: "Cổng Phụ Huynh",
    sections: [
      {
        title: "TỔNG QUAN",
        items: [
          { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
          { label: "Liên kết con em", href: "/dashboard/children", icon: HeartHandshake, badge: "2" },
        ],
      },
      {
        title: "THEO DÕI",
        items: [
          { label: "Tiến độ học tập", href: "/dashboard/progress", icon: BarChart3 },
          { label: "Lịch học gia đình", href: "/dashboard/schedule", icon: Calendar },
          { label: "Gia sư đồng hành", href: "/dashboard/tutors", icon: GraduationCap },
        ],
      },
      {
        title: "TÀI CHÍNH",
        items: [
          { label: "Gói học & Thanh toán", href: "/dashboard/billing", icon: CreditCard },
        ],
      },
    ],
  },
  ADMIN: {
    roleTitle: "Quản Trị Hệ Thống",
    sections: [
      {
        title: "HỆ THỐNG",
        items: [
          { label: "Tổng quan", href: "/dashboard", icon: LayoutDashboard },
          { label: "Duyệt bằng cấp KYC", href: "/dashboard/kyc-queue", icon: ShieldCheck, badge: "5" },
        ],
      },
      {
        title: "QUẢN LÝ NGƯỜI DÙNG",
        items: [
          { label: "Học sinh & Phụ huynh", href: "/dashboard/learners", icon: Users },
          { label: "Gia sư", href: "/dashboard/tutors-management", icon: GraduationCap },
          { label: "Khiếu nại & Tranh chấp", href: "/dashboard/disputes", icon: AlertTriangle, badge: "2" },
        ],
      },
      {
        title: "CẤU HÌNH",
        items: [
          { label: "Danh mục môn học", href: "/dashboard/catalogs", icon: BookOpen },
          { label: "Cài đặt hệ thống", href: "/dashboard/settings", icon: Settings },
        ],
      },
    ],
  },
};

interface SidebarProps {
  role?: UserRole;
  collapsed: boolean;
  onToggleCollapse: () => void;
  currentPath?: string;
  user?: {
    fullName: string;
    email: string;
  };
}

export function Sidebar({
  role = "STUDENT",
  collapsed,
  onToggleCollapse,
  currentPath = "/dashboard",
  user,
}: SidebarProps) {
  const currentConfig = ROLE_NAV_CONFIG[role] || ROLE_NAV_CONFIG.STUDENT;

  return (
    <aside
      className={`${
        collapsed ? "w-20" : "w-64"
      } bg-white border-r border-stone-200/80 transition-[width] duration-300 ease-in-out flex flex-col justify-between shrink-0 h-screen sticky top-0 z-40 select-none`}
    >
      <div className="p-4 flex flex-col gap-5 overflow-y-auto overflow-x-hidden">
        {/* Logo EduFit chuẩn thương hiệu */}
        <div className="h-12 px-1 flex items-center">
          <EduFitLogo href="/dashboard#dashboard-top" showText={!collapsed} roleBadge={currentConfig.roleTitle} />
        </div>

        {/* Navigation Sections */}
        <nav className="flex flex-col gap-5">
          {currentConfig.sections.map((section, idx) => (
            <div key={idx} className="flex flex-col gap-1">
              <span
                aria-hidden={collapsed}
                className={`h-4 px-2.5 text-[10px] font-bold uppercase tracking-wider text-neutral-400 font-sans whitespace-nowrap transition-opacity duration-200 ${
                  collapsed ? "opacity-0" : "opacity-100"
                }`}
              >
                  {section.title}
              </span>
              {section.items.map((item, itemIdx) => {
                const Icon = item.icon;
                const isActive = currentPath === item.href;

                return (
                  <Link
                    key={itemIdx}
                    href={item.href}
                    className={`h-10 flex items-center justify-between px-2.5 rounded-xl text-xs font-medium transition-colors group ${
                      isActive
                        ? "bg-amber-500/10 text-amber-900 font-semibold shadow-xs"
                        : "text-neutral-600 hover:bg-stone-100 hover:text-neutral-900"
                    }`}
                    title={collapsed ? item.label : undefined}
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <Icon
                        className={`size-4.5 shrink-0 transition-transform group-hover:scale-105 ${
                          isActive ? "text-amber-600" : "text-neutral-400 group-hover:text-neutral-700"
                        }`}
                      />
                      <span
                        aria-hidden={collapsed}
                        className={`truncate whitespace-nowrap transition-[max-width,opacity] duration-200 ${
                          collapsed ? "max-w-0 opacity-0" : "max-w-40 opacity-100"
                        }`}
                      >
                        {item.label}
                      </span>
                    </div>

                    {!collapsed && item.badge && (
                      <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-md bg-amber-500 text-white leading-none shadow-xs">
                        {item.badge}
                      </span>
                    )}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>
      </div>

      {/* Footer / Toggle & User pill */}
      <div className="p-3 border-t border-stone-100 flex flex-col gap-2 bg-stone-50/50">
        <button
          type="button"
          onClick={onToggleCollapse}
          className="w-full flex items-center justify-center gap-2 p-2 rounded-xl hover:bg-stone-200/60 text-neutral-500 text-xs transition-colors"
          title={collapsed ? "Mở rộng thanh menu" : "Thu gọn thanh menu"}
        >
          {collapsed ? (
            <PanelLeft className="size-4" />
          ) : (
            <>
              <PanelLeftClose className="size-4" />
              <span>Thu gọn menu</span>
            </>
          )}
        </button>

        {user && (
          <div className={`h-11 px-2 flex items-center bg-white rounded-xl border border-stone-200/60 transition-[gap] duration-300 ${collapsed ? "justify-center gap-0" : "gap-2"}`}>
            <div className="size-7 rounded-lg bg-amber-100 text-amber-700 font-bold text-xs flex items-center justify-center shrink-0">
              {user.fullName.charAt(0).toUpperCase()}
            </div>
            <div
              aria-hidden={collapsed}
              className={`flex flex-col overflow-hidden whitespace-nowrap transition-[max-width,opacity] duration-300 ${
                collapsed ? "max-w-0 opacity-0" : "max-w-44 opacity-100"
              }`}
            >
              <span className="text-xs font-semibold text-neutral-900 truncate">{user.fullName}</span>
              <span className="text-[10px] text-neutral-400 truncate">{user.email}</span>
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}
