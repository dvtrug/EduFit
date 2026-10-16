export type MilestoneStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED";

export interface MilestoneItem {
  milestoneId: string;
  title: string;
  description?: string;
  orderIndex: number;
  status: MilestoneStatus;
  completedAt?: string;
}

export interface PlanItem {
  planId: string;
  classId: string;
  title: string;
  description?: string;
  milestones: MilestoneItem[];
  totalMilestones: number;
  completedMilestones: number;
  progressPercentage: number;
}

export interface ClassSessionStats {
  totalSessions: number;
  completedSessions: number;
  upcomingSessions: number;
  cancelledSessions: number;
}

export interface ProgressSeriesPoint {
  date: string;
  progressPercent: number;
}

export interface SessionNoteItem {
  sessionNoteId: string;
  sessionId: string;
  sessionDate?: string;
  summary: string;
  homeworkAssigned?: string;
  studentAttitude?: string;
  nextSteps?: string;
  createdAt: string;
}

export interface ClassSelectorItem {
  classId: string;
  title: string;
  status: string;
  progressPercentage?: number;
}

export interface StudentDashboardData {
  studentId: string;
  studentName: string;
  classes: ClassSelectorItem[];
  selectedClassId?: string;
  tutorId?: string;
  tutorName?: string;
  goalId?: string;
  goalTitle?: string;
  progressPercentage?: number;
  plan?: PlanItem;
  sessionStats?: ClassSessionStats;
  progressSeries?: ProgressSeriesPoint[];
  recentNotes?: SessionNoteItem[];
  hasMoreNotes?: boolean;
}

export interface ParentStudentSummary {
  studentId: string;
  studentName: string;
  classes: ClassSelectorItem[];
  activeClassDashboard?: StudentDashboardData;
}

export interface ParentDashboardData {
  students: ParentStudentSummary[];
}
