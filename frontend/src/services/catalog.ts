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

export const catalogService = {
  /**
   * Lấy toàn bộ danh mục dùng chung (Cấp học và Môn học)
   */
  async getCatalog(): Promise<CatalogResponse> {
    const res = await apiClient.get<CatalogResponse>("/api/v1/catalogs");
    return res.data as CatalogResponse;
  },

  /**
   * Lấy danh sách cấp học
   */
  async getEducationLevels(): Promise<EducationLevel[]> {
    const res = await apiClient.get<EducationLevel[]>("/api/v1/catalogs/education-levels");
    return (res.data || []) as EducationLevel[];
  },

  /**
   * Lấy danh sách môn học
   */
  async getSubjects(): Promise<SubjectItem[]> {
    const res = await apiClient.get<SubjectItem[]>("/api/v1/catalogs/subjects");
    return (res.data || []) as SubjectItem[];
  },
};
