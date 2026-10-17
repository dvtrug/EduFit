"use client";

import React, { useEffect, useState } from "react";
import { ScheduleCalendarView, ConnectedClass } from "@/features/scheduling";
import { connectionApi } from "@/features/connection/api/connectionApi";

export default function ParentSchedulePage() {
  const [classes, setClasses] = useState<ConnectedClass[]>([]);

  useEffect(() => {
    connectionApi
      .getStudentClasses()
      .then((data) => {
        const activeClasses = (data || []).filter((c) => c.status === "ACTIVE");
        const mapped: ConnectedClass[] = activeClasses.map((c) => ({
          classId: c.classId,
          title: `${c.studentDisplayName || "Học sinh"} - ${c.subjectName || "Lớp học"}`,
          tutorId: c.tutorId,
          studentId: c.studentId,
          studentName: c.studentDisplayName ?? undefined,
          tutorName: c.tutorDisplayName ?? undefined,
        }));
        setClasses(mapped);
      })
      .catch(() => {
        setClasses([]);
      });
  }, []);

  return <ScheduleCalendarView role="PARENT" availabilitySlots={[]} classes={classes} />;
}
