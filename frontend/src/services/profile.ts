import { apiClient } from "@/lib/api";
import { catalogService } from "@/services/catalog";

export interface StudentProfileData {
  studentId: string;
  userId: string;
  educationLevelId?: number;
  educationLevelName?: string;
  area?: string;
  profileComplete: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateStudentProfilePayload {
  educationLevelId?: number;
  area?: string;
}

export type LearningGoalType =
  | "CONSOLIDATE"
  | "IMPROVE_GRADES"
  | "ADVANCED_STUDY"
  | "EXAM_PREPARATION"
  | "OTHER";

export interface LearningGoalAvailabilitySlot {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface LearningGoalData {
  goalId: string;
  subjectId: number;
  subjectName: string;
  educationLevelId: number;
  educationLevelName: string;
  currentLevel: number;
  desiredLevel: number;
  goalType: LearningGoalType;
  deadline: string;
  mode: "ONLINE" | "OFFLINE" | "BOTH";
  area?: string;
  budgetMin?: number;
  budgetMax: number;
  status: "ACTIVE" | "ARCHIVED";
  availabilitySlots: LearningGoalAvailabilitySlot[];
  readyForMatching?: boolean;
  readinessReasons?: string[];
}

export interface CreateLearningGoalPayload {
  subjectId: number;
  currentLevel: number;
  desiredLevel: number;
  goalType: LearningGoalType;
  deadline: string;
  mode: "ONLINE" | "OFFLINE" | "BOTH";
  area?: string;
  budgetMin?: number;
  budgetMax: number;
  availabilitySlots: LearningGoalAvailabilitySlot[];
}

export interface TutorSubjectItem {
  tutorSubjectId: string;
  subjectId: number;
  subjectName: string;
  educationLevelId: number;
  educationLevelName: string;
}

export interface TutorAvailabilitySlot {
  slotId?: string;
  dayOfWeek: number; // 1 = Monday .. 7 = Sunday
  startTime: string; // "HH:mm:ss" or "HH:mm"
  endTime: string;   // "HH:mm:ss" or "HH:mm"
}

export interface TutorProfileData {
  tutorId: string;
  userId: string;
  displayName: string;
  headline?: string;
  bio?: string;
  teachingMode: "ONLINE" | "OFFLINE" | "BOTH";
  area?: string;
  pricePerSession: number;
  experienceYears?: number;
  teachingMethod?: string;
  status: string;
  verifiedAt?: string;
  ratingAvg?: number;
  reviewCount?: number;
  subjects: TutorSubjectItem[];
  availabilitySlots: TutorAvailabilitySlot[];
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateTutorProfilePayload {
  displayName: string;
  headline?: string;
  bio?: string;
  teachingMode: "ONLINE" | "OFFLINE" | "BOTH";
  area?: string;
  pricePerSession: number;
  experienceYears?: number;
  teachingMethod?: string;
}

export interface GenerateTutorBioPayload {
  displayName?: string;
  subjects?: string[];
  educationLevels?: string[];
  experienceYears?: number;
  tone?: "friendly" | "professional" | "concise";
  focusArea?: string;
  customPrompt?: string;
  currentBio?: string;
}

export interface GenerateTutorBioResult {
  bio: string;
  isAiGenerated: boolean;
  remainingQuota: number;
}

export interface MyProfileComposite {
  userId: string;
  email: string;
  roles: string[];
  studentProfile?: StudentProfileData;
  tutorProfile?: TutorProfileData;
}

export const profileService = {
  /**
   * Lấy tổng hợp thông tin hồ sơ của tài khoản đang đăng nhập
   */
  async getMyProfile(): Promise<MyProfileComposite> {
    const res = await apiClient.get<MyProfileComposite>("/api/v1/profiles/me");
    return res.data as MyProfileComposite;
  },

  /**
   * Lấy hồ sơ học viên
   */
  async getMyStudentProfile(): Promise<StudentProfileData> {
    const res = await apiClient.get<StudentProfileData>("/api/v1/profiles/students/me");
    return res.data as StudentProfileData;
  },

  /**
   * Cập nhật hồ sơ học viên
   */
  async updateStudentProfile(payload: UpdateStudentProfilePayload): Promise<StudentProfileData> {
    const res = await apiClient.put<StudentProfileData>("/api/v1/profiles/students/me", payload);
    return res.data as StudentProfileData;
  },

  async getMyLearningGoals(): Promise<LearningGoalData[]> {
    const res = await apiClient.get<LearningGoalData[]>("/api/v1/profiles/students/me/goals");
    return profileService.labelGoals(res.data || []);
  },

  async getLinkedStudentGoals(studentId: string): Promise<LearningGoalData[]> {
    const res = await apiClient.get<LearningGoalData[]>(`/api/v1/discovery/students/${encodeURIComponent(studentId)}/goals`);
    return profileService.labelGoals(res.data || []);
  },

  async labelGoals(goals: LearningGoalData[]): Promise<LearningGoalData[]> {
    try {
      const catalog = await catalogService.getCatalog();
      return goals.map(goal => ({
        ...goal,
        subjectName: catalog.subjects.find(s => s.subjectId === goal.subjectId)?.name || `Môn ${goal.subjectId}`,
        educationLevelName: catalog.educationLevels.find(l => l.levelId === goal.educationLevelId)?.name || "Chưa chọn khối lớp",
      }));
    } catch {
      return goals;
    }
  },

  async createLearningGoal(payload: CreateLearningGoalPayload): Promise<LearningGoalData> {
    const { availabilitySlots, ...goal } = payload;
    const res = await apiClient.post<LearningGoalData>("/api/v1/profiles/students/me/goals", goal);
    const saved = res.data as LearningGoalData;
    try {
      const ready = await apiClient.put<LearningGoalData>(`/api/v1/profiles/students/me/goals/${saved.goalId}/availability`, { slots: availabilitySlots });
      return ready.data as LearningGoalData;
    } catch (error) {
      await apiClient.post(`/api/v1/profiles/students/me/goals/${saved.goalId}/archive`).catch(() => undefined);
      throw error;
    }
  },

  async updateLearningGoal(goalId: string, payload: CreateLearningGoalPayload): Promise<LearningGoalData> {
    const { availabilitySlots, ...goal } = payload;
    const res = await apiClient.put<LearningGoalData>(`/api/v1/profiles/students/me/goals/${encodeURIComponent(goalId)}`, goal);
    const saved = res.data as LearningGoalData;
    if (availabilitySlots) {
      const ready = await apiClient.put<LearningGoalData>(
        `/api/v1/profiles/students/me/goals/${encodeURIComponent(goalId)}/availability`,
        { slots: availabilitySlots }
      );
      return ready.data as LearningGoalData;
    }
    return saved;
  },

  /**
   * Lấy hồ sơ gia sư
   */
  async getMyTutorProfile(): Promise<TutorProfileData> {
    const res = await apiClient.get<TutorProfileData>("/api/v1/profiles/tutors/me");
    return res.data as TutorProfileData;
  },

  /**
   * Cập nhật hồ sơ gia sư
   */
  async updateTutorProfile(payload: UpdateTutorProfilePayload): Promise<TutorProfileData> {
    const res = await apiClient.put<TutorProfileData>("/api/v1/profiles/tutors/me", payload);
    return res.data as TutorProfileData;
  },

  /**
   * Thêm môn học giảng dạy cho gia sư
   */
  async addTutorSubject(subjectId: number, educationLevelId: number): Promise<TutorProfileData> {
    const res = await apiClient.post<TutorProfileData>("/api/v1/profiles/tutors/me/subjects", {
      subjectId,
      educationLevelId,
    });
    return res.data as TutorProfileData;
  },

  /**
   * Xóa môn học giảng dạy của gia sư
   */
  async removeTutorSubject(subjectId: number, educationLevelId: number): Promise<TutorProfileData> {
    const res = await apiClient.delete<TutorProfileData>(
      `/api/v1/profiles/tutors/me/subjects?subjectId=${subjectId}&educationLevelId=${educationLevelId}`
    );
    return res.data as TutorProfileData;
  },

  /**
   * Cập nhật danh sách khung giờ rảnh của gia sư
   */
  async setAvailabilitySlots(
    slots: Array<{ dayOfWeek: number; startTime: string; endTime: string }>
  ): Promise<TutorProfileData> {
    const res = await apiClient.put<TutorProfileData>("/api/v1/profiles/tutors/me/availability-slots", {
      slots,
    });
    return res.data as TutorProfileData;
  },

  /**
   * Sinh bản thảo tiểu sử gia sư (Bio) thông qua AI Assistant
   */
  async generateTutorBio(payload: GenerateTutorBioPayload): Promise<GenerateTutorBioResult> {
    const res = await apiClient.post<GenerateTutorBioResult>(
      "/api/v1/profiles/tutors/me/generate-bio",
      payload
    );
    return res.data as GenerateTutorBioResult;
  },
};
