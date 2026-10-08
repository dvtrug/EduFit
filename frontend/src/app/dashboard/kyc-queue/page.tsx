"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { AdminKycQueueView } from "@/features/verification/components/AdminKycQueueView";

/**
 * =========================================================================
 * SPRING BOOT API INTEGRATION:
 * =========================================================================
 * GET  /api/v1/admin/verifications/pending
 * POST /api/v1/admin/verifications/{id}/review
 * Security: @PreAuthorize("hasRole('ADMIN')")
 * =========================================================================
 */
export default function AdminKycQueuePage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  useEffect(() => {
    if (!isLoading) {
      if (!user) {
        router.push("/login");
      } else if (user.role !== "ADMIN") {
        router.push("/dashboard");
      }
    }
  }, [user, isLoading, router]);

  const handleLogout = async () => {
    if (user) {
      await logout();
    }
    router.push("/login");
  };

  if (isLoading || !user || user.role !== "ADMIN") {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="size-8 border-3 border-purple-500/30 border-t-purple-600 rounded-full animate-spin" />
      </div>
    );
  }

  const effectiveUser = user;

  return (
    <DashboardShell user={effectiveUser} role="ADMIN" onLogout={handleLogout}>
      <div className="max-w-6xl mx-auto">
        <AdminKycQueueView />
      </div>
    </DashboardShell>
  );
}
