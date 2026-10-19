export interface AdminDashboardStats {
  totalUsers: number;
  totalTutors: number;
  totalStudents: number;
  totalParents: number;
  activeClasses: number;
  completedSessions: number;
  pendingVerifications?: number;
  pendingKyc?: number;
}

export interface SubjectAdmin {
  id: number;
  name: string;
  code?: string;
  slug?: string;
  category?: string;
  description?: string;
  isActive: boolean;
  createdAt?: string;
}

export interface CreateSubjectPayload {
  name: string;
  category?: string;
  code?: string;
  slug?: string;
  description?: string;
}

export interface UpdateSubjectStatusPayload {
  isActive: boolean;
}

export interface AuditLogItem {
  id: string;
  actorUserId?: string;
  actor?: string;
  action: string;
  targetType?: string;
  targetId?: string;
  reason?: string;
  details?: string;
  ipAddress?: string;
  metadata?: Record<string, unknown>;
  createdAt: string;
}

export interface PagedAuditLogs {
  items: AuditLogItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditLogQueryParams {
  page?: number;
  size?: number;
  action?: string;
}

export interface ParentStudentAdminLink {
  id?: string;
  linkId?: string;
  parentId?: string;
  parentEmail?: string;
  parentName?: string;
  studentId?: string;
  studentEmail?: string;
  studentName?: string;
  status: string;
  createdAt?: string;
}
