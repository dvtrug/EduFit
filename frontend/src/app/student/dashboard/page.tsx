"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";
import { StudentDashboardView } from "@/features/dashboard";

export default function StudentDashboardPage() {
  const { user } = useAuth();
  if (!user) return null;
  return <StudentDashboardView user={user} />;
}
