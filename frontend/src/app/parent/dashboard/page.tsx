"use client";

import React from "react";
import { useAuth } from "@/context/AuthContext";
import { ParentDashboardView } from "@/features/dashboard";

export default function ParentDashboardPage() {
  const { user } = useAuth();
  if (!user) return null;
  return <ParentDashboardView user={user} />;
}
