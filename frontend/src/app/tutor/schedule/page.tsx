"use client";

import React, { useEffect, useState } from "react";
import { ScheduleCalendarView, ConnectedClass } from "@/features/scheduling";
import { connectionApi } from "@/features/connection/api/connectionApi";
import { profileService, TutorAvailabilitySlot } from "@/services/profile";

export default function TutorSchedulePage() {
  const [availabilitySlots, setAvailabilitySlots] = useState<TutorAvailabilitySlot[]>([]);
  const [classes, setClasses] = useState<ConnectedClass[]>([]);

  useEffect(() => {
    profileService
      .getMyTutorProfile()
      .then((profile) => setAvailabilitySlots(profile.availabilitySlots || []))
      .catch(() => setAvailabilitySlots([]));

    connectionApi
      .getTutorClasses()
      .then((data) => {
        const activeClasses = (data || []).filter((c) => c.status === "ACTIVE");
        const mapped: ConnectedClass[] = activeClasses.map((c) => ({
          classId: c.classId,
          title: c.subjectName ? `${c.studentDisplayName || "Học sinh"} - ${c.subjectName}` : (c.studentDisplayName || "Lớp học"),
          tutorId: c.tutorId,
          studentId: c.studentId,
          tutorName: c.tutorDisplayName ?? undefined,
          studentName: c.studentDisplayName ?? undefined,
        }));
        setClasses(mapped);
      })
      .catch(() => setClasses([]));
  }, []);

  return (
    <ScheduleCalendarView role="TUTOR" availabilitySlots={availabilitySlots} classes={classes} />
  );
}
