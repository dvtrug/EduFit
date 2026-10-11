"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { ScheduleCalendarView } from "@/features/schedule/components/ScheduleCalendarView";
import { profileService, TutorAvailabilitySlot } from "@/services/profile";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { UserRole } from "@/shared/ui/Sidebar";

export default function SchedulePage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();
  const [availabilitySlots, setAvailabilitySlots] = useState<TutorAvailabilitySlot[]>([]);

  useEffect(() => {
    if (isLoading) return;
    if (!user) {
      router.replace("/login");
      return;
    }

    if (user.role === "TUTOR") {
      profileService
        .getMyTutorProfile()
        .then((profile) => setAvailabilitySlots(profile.availabilitySlots || []))
        .catch(() => setAvailabilitySlots([]));
    }
  }, [isLoading, router, user]);

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  if (isLoading || !user) {
    return (
      <div className="grid min-h-screen place-items-center bg-stone-50">
        <span className="text-xs font-medium text-neutral-500">Đang tải lịch...</span>
      </div>
    );
  }

  const role = user.role as UserRole;

  return (
    <DashboardShell user={user} role={role} onLogout={handleLogout}>
      <ScheduleCalendarView role={role} availabilitySlots={availabilitySlots} />
    </DashboardShell>
  );
}
