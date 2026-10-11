"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";

export default function StudentLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (user.role?.toUpperCase() !== "STUDENT") {
      const targetRole = user.role?.toLowerCase() || "student";
      router.replace(`/${targetRole}/dashboard`);
    }
  }, [user, isLoading, router]);

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  if (isLoading || !user || user.role?.toUpperCase() !== "STUDENT") {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 border-3 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
          <span className="text-xs text-neutral-500 font-medium">Đang tải không gian học sinh...</span>
        </div>
      </div>
    );
  }

  return (
    <DashboardShell user={user} role="STUDENT" onLogout={handleLogout}>
      {children}
    </DashboardShell>
  );
}
