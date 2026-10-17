import { fetchApi, ApiResponse } from "@/lib/api";
import type {
  StudentDashboardData,
  ParentDashboardData,
  PlanItem,
} from "../types";

export const progressApi = {
  /**
   * Lấy dữ liệu dashboard tiến độ học tập của học sinh
   * GET /api/v1/progress/dashboard/student/{studentId}
   */
  async getStudentDashboard(
    studentId: string,
    classId?: string,
    range = "30D"
  ): Promise<ApiResponse<StudentDashboardData>> {
    const params = new URLSearchParams({ range });
    if (classId) {
      params.set("classId", classId);
    }
    return fetchApi<StudentDashboardData>(
      `/api/v1/progress/dashboard/student/${encodeURIComponent(studentId)}?${params.toString()}`,
      { method: "GET" }
    );
  },

  /**
   * Lấy dữ liệu dashboard tiến độ học tập cho phụ huynh
   * GET /api/v1/progress/dashboard/parent
   */
  async getParentDashboard(): Promise<ApiResponse<ParentDashboardData>> {
    return fetchApi<ParentDashboardData>(
      "/api/v1/progress/dashboard/parent",
      { method: "GET" }
    );
  },

  /**
   * Lấy kế hoạch học tập theo lớp học
   * GET /api/v1/progress/plans/{classId}
   */
  async getPlanByClassId(classId: string): Promise<ApiResponse<PlanItem>> {
    return fetchApi<PlanItem>(
      `/api/v1/progress/plans/${encodeURIComponent(classId)}`,
      { method: "GET" }
    );
  },
};
