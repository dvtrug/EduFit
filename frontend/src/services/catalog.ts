import { apiClient } from "@/lib/api";

export interface EducationLevel {
  levelId: number;
  name: string;
  sortOrder: number;
}

export interface SubjectItem {
  subjectId: number;
  name: string;
}

export interface CatalogResponse {
  educationLevels: EducationLevel[];
  subjects: SubjectItem[];
}

export const DEFAULT_EDUCATION_LEVELS: EducationLevel[] = [
  { levelId: 1, name: "Lớp 6", sortOrder: 1 },
  { levelId: 2, name: "Lớp 7", sortOrder: 2 },
  { levelId: 3, name: "Lớp 8", sortOrder: 3 },
  { levelId: 4, name: "Lớp 9", sortOrder: 4 },
];

export const DEFAULT_SUBJECTS: SubjectItem[] = [
  { subjectId: 1, name: "Toán" },
  { subjectId: 2, name: "Ngữ Văn" },
  { subjectId: 3, name: "Tiếng Anh" },
  { subjectId: 4, name: "Vật Lý" },
  { subjectId: 5, name: "Hóa Học" },
  { subjectId: 6, name: "Sinh Học" },
];

export const catalogService = {
  /**
   * Lấy toàn bộ danh mục dùng chung (Cấp học và Môn học)
   */
  async getCatalog(): Promise<CatalogResponse> {
    try {
      const res = await apiClient.get<CatalogResponse>("/api/v1/catalogs");
      return res.data as CatalogResponse;
    } catch {
      return {
        educationLevels: DEFAULT_EDUCATION_LEVELS,
        subjects: DEFAULT_SUBJECTS,
      };
    }
  },

  /**
   * Lấy danh sách cấp học
   */
  async getEducationLevels(): Promise<EducationLevel[]> {
    try {
      const res = await apiClient.get<EducationLevel[]>("/api/v1/catalogs/education-levels");
      return (res.data || []) as EducationLevel[];
    } catch {
      return DEFAULT_EDUCATION_LEVELS;
    }
  },

  /**
   * Lấy danh sách môn học
   */
  async getSubjects(): Promise<SubjectItem[]> {
    try {
      const res = await apiClient.get<SubjectItem[]>("/api/v1/catalogs/subjects");
      return (res.data || []) as SubjectItem[];
    } catch {
      return DEFAULT_SUBJECTS;
    }
  },
};
