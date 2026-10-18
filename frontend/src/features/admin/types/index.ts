export type AccountRole = "STUDENT" | "TUTOR" | "PARENT" | "ADMIN";
export type AccountStatus = "ACTIVE" | "DEACTIVATED";

export interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  role: AccountRole;
  status: AccountStatus;
  avatarUrl?: string | null;
  createdAt: string;
  lastLoginAt?: string | null;
}

export interface AdminUserListResponse {
  users: AdminUser[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AdminUserStats {
  totalUsers: number;
  totalStudents: number;
  totalTutors: number;
  totalParents: number;
  totalActive: number;
  totalDeactivated: number;
  activeStudents?: number;
  deactivatedStudents?: number;
  activeParents?: number;
  deactivatedParents?: number;
  activeTutors?: number;
  deactivatedTutors?: number;
}

export interface GetUsersParams {
  role?: string;
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

export type FamilyLinkStatus = "ALL" | "CONFIRMED" | "REVOKED";

export interface AdminParentStudentLink {
  linkId: string;
  parentUserId: string;
  parentFullName: string;
  parentEmail: string;
  studentId: string;
  studentUserId: string;
  studentFullName: string;
  studentEmail: string;
  status: "CONFIRMED" | "REVOKED";
  confirmedAt: string;
  revokedAt?: string | null;
  revokeReason?: string | null;
}

export interface AdminParentStudentLinkListResponse {
  items: AdminParentStudentLink[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AdminParentStudentLinkStats {
  totalLinks: number;
  activeLinks: number;
  revokedLinks: number;
  uniqueParents: number;
  uniqueStudents: number;
}

export interface GetFamilyLinksParams {
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

