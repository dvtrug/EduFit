"use client";

import React, { useEffect, useState } from "react";
import { ScheduleCalendarView, ConnectedClass } from "@/features/scheduling";
import { useAuth } from "@/context/AuthContext";
import { progressApi } from "@/features/progress/api/progressApi";

export default function StudentSchedulePage() {
  const { user } = useAuth();
  const [classes, setClasses] = useState<ConnectedClass[]>([]);

  useEffect(() => {
    if (user?.id) {
      progressApi
        .getStudentDashboard(user.id)
        .then((res) => {
          if (res.success && res.data?.classes) {
            const mapped: ConnectedClass[] = res.data.classes.map((c) => ({
              classId: c.classId,
              title: c.title,
              tutorId: res.data?.tutorId || "",
              studentId: res.data?.studentId || user.id,
              tutorName: res.data?.tutorName ?? undefined,
              studentName: res.data?.studentName,
            }));
            setClasses(mapped);
          }
        })
        .catch(() => {
          // Fallback gracefully if dashboard is empty
        });
    }
  }, [user?.id]);

  return <ScheduleCalendarView role="STUDENT" availabilitySlots={[]} classes={classes} />;
}
