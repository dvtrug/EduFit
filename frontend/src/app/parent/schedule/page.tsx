"use client";

import React, { useEffect, useState } from "react";
import { ScheduleCalendarView, ConnectedClass } from "@/features/scheduling";
import { progressApi } from "@/features/progress/api/progressApi";

export default function ParentSchedulePage() {
  const [classes, setClasses] = useState<ConnectedClass[]>([]);

  useEffect(() => {
    progressApi
      .getParentDashboard()
      .then((res) => {
        if (res.success && res.data?.students) {
          const list: ConnectedClass[] = [];
          for (const s of res.data.students) {
            for (const c of s.classes || []) {
              list.push({
                classId: c.classId,
                title: `${s.studentName} - ${c.title}`,
                tutorId: s.activeClassDashboard?.tutorId || "",
                studentId: s.studentId,
                studentName: s.studentName,
                tutorName: s.activeClassDashboard?.tutorName ?? undefined,
              });
            }
          }
          setClasses(list);
        }
      })
      .catch(() => {
        // Fallback gracefully
      });
  }, []);

  return <ScheduleCalendarView role="PARENT" availabilitySlots={[]} classes={classes} />;
}
