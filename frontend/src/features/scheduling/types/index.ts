import type { TutorAvailabilitySlot } from "@/services/profile";
import type { UserRole } from "@/shared/ui/Sidebar";

export interface ScheduleCalendarViewProps {
  role: UserRole;
  availabilitySlots?: TutorAvailabilitySlot[];
}
