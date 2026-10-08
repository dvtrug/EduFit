"use client";

import React, { useState } from "react";
import { usePathname } from "next/navigation";
import { Sidebar, UserRole } from "./Sidebar";
import { TopHeader } from "./TopHeader";

interface DashboardShellProps {
  user: {
    fullName: string;
    email: string;
    role: string;
  };
  role: UserRole;
  onLogout: () => void;
  children: React.ReactNode;
}

export function DashboardShell({ user, role, onLogout, children }: DashboardShellProps) {
  const [collapsed, setCollapsed] = useState(false);
  const currentPath = usePathname();

  const toggleCollapse = () => setCollapsed((prev) => !prev);

  return (
    <div className="min-h-screen bg-stone-50 flex font-sans text-neutral-900 selection:bg-amber-200">
      {/* 1. Unified Dynamic Sidebar */}
      <Sidebar
        role={role}
        collapsed={collapsed}
        onToggleCollapse={toggleCollapse}
        currentPath={currentPath}
        user={user}
      />

      {/* 2. Main Right Pane */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Unified Dynamic TopHeader */}
        <TopHeader
          role={role}
          user={user}
          onLogout={onLogout}
          onToggleSidebar={toggleCollapse}
        />

        {/* Scrollable Dashboard Workspace Content */}
        <main id="dashboard-top" className="flex-1 scroll-mt-20 p-6 sm:p-8 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
