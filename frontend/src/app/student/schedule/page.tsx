"use client";

import React, { useEffect, useState } from "react";
import { ScheduleCalendarView, ConnectedClass } from "@/features/scheduling";
import { connectionApi } from "@/features/connection/api/connectionApi";

export default function StudentSchedulePage() {
  const [classes, setClasses] = useState<ConnectedClass[]>([]);

  useEffect(() => {
    connectionApi
      .getStudentClasses()
      .then((data) => {
        const activeClasses = (data || []).filter((c) => c.status === "ACTIVE");
        const mapped: ConnectedClass[] = activeClasses.map((c) => ({
          classId: c.classId,
          title: c.subjectName ? `${c.tutorDisplayName || "Gia sư"} - ${c.subjectName}` : (c.tutorDisplayName || "Lớp học"),
          tutorId: c.tutorId,
          studentId: c.studentId,
          tutorName: c.tutorDisplayName ?? undefined,
          studentName: c.studentDisplayName ?? undefined,
        }));
        setClasses(mapped);
      })
      .catch(() => {
        setClasses([]);
      });
  }, []);

  return <ScheduleCalendarView role="STUDENT" availabilitySlots={[]} classes={classes} />;
}
