export interface ConnectionRequest {
  requestId: string;
  studentId: string;
  tutorId: string;
  goalId: string;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED" | "EXPIRED" | string;
  createdAt: string;
  expiresAt: string;
}

export interface StudentLink {
  linkId: string;
  studentId: string;
  status: string;
}

export interface SendConnectionRequestPayload {
  goalId: string;
  tutorId: string;
  desiredSlots: string[];
  message?: string;
}

export interface TutorInboxItem {
  requestId: string;
  goalType: string;
  subjectId: number;
  educationLevelId: number;
  desiredSlots: string[];
  message?: string | null;
  senderRole: "STUDENT" | "PARENT" | string;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED" | "EXPIRED" | string;
  expiresAt: string;
  deadline?: string | null;
  studentDisplayName?: string | null;
  currentLevel?: number | null;
  desiredLevel?: number | null;
  currentGrade?: string | null;
  targetScore?: string | null;
  budgetMin?: number | null;
  budgetMax?: number | null;
  area?: string | null;
  district?: string | null;
  city?: string | null;
  subjectName?: string | null;
  educationLevelName?: string | null;
  gradeLevelName?: string | null;
  hasSlotConflict?: boolean;
  conflictingSlots?: string[];
}

export interface TutoringClass {
  classId: string;
  studentId: string;
  tutorId: string;
  goalId: string;
  status: "ACTIVE" | "ENDED" | "COMPLETED" | "CANCELLED" | string;
  studentDisplayName?: string | null;
  tutorDisplayName?: string | null;
  subjectName?: string | null;
  educationLevelName?: string | null;
  scheduleStatus?: "PROPOSED" | "SCHEDULED" | "COMPLETED" | string;
  sessionCount: number;
  completedCount?: number;
  scheduledCount?: number;
  totalCount?: number;
  startedAt: string;
  endedAt?: string | null;
  endReason?: string | null;
}

export interface RespondRequestPayload {
  accept: boolean;
  reason?: string;
}

export interface RespondRequestResult {
  classId?: string;
  studentId?: string;
  tutorId?: string;
  goalId?: string;
}
