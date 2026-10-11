"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";
import { TutorDashboardView } from "@/features/dashboard";

export default function TutorDashboardPage() {
  const { user } = useAuth();
  if (!user) return null;
  return <TutorDashboardView user={user} />;
}
