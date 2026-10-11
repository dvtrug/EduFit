"use client";

import React, { useEffect, useState } from "react";
import { ScheduleCalendarView } from "@/features/scheduling";
import { profileService, TutorAvailabilitySlot } from "@/services/profile";

export default function TutorSchedulePage() {
  const [availabilitySlots, setAvailabilitySlots] = useState<TutorAvailabilitySlot[]>([]);

  useEffect(() => {
    profileService
      .getMyTutorProfile()
      .then((profile) => setAvailabilitySlots(profile.availabilitySlots || []))
      .catch(() => setAvailabilitySlots([]));
  }, []);

  return (
    <ScheduleCalendarView role="TUTOR" availabilitySlots={availabilitySlots} />
  );
}
