import { apiClient } from "@/lib/api";

export interface StudentProfileData {
  id: string;
  gradeLevel?: string;
  targetSubjects?: string[];
  learningGoals?: string;
  preferredSchedule?: string;
}

export interface TutorProfileData {
  id: string;
  headline?: string;
  bio?: string;
  hourlyRate?: number;
  teachingSubjects?: Array<{
    subjectId: number;
    subjectName: string;
    educationLevelName: string;
  }>;
  availabilitySlots?: Array<{
    dayOfWeek: number;
    startTime: string;
    endTime: string;
  }>;
  verificationStatus?: string;
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
  async updateStudentProfile(payload: Partial<StudentProfileData>): Promise<StudentProfileData> {
    const res = await apiClient.put<StudentProfileData>("/api/v1/profiles/students/me", payload);
    return res.data as StudentProfileData;
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
  async updateTutorProfile(payload: Partial<TutorProfileData>): Promise<TutorProfileData> {
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
};
