"use client";

import React from "react";
import { SettingsView } from "@/features/settings";

export default function ParentProfilePage() {
  return <SettingsView role="PARENT" defaultTab="profile" />;
}
