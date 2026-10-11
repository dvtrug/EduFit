"use client";

import React from "react";
import { ScheduleCalendarView } from "@/features/scheduling";

export default function StudentSchedulePage() {
  return <ScheduleCalendarView role="STUDENT" availabilitySlots={[]} />;
}
