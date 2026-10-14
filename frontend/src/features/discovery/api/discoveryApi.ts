import { fetchApi, ApiResponse } from "@/lib/api";
import type {
  TutorDiscoveryCard,
  TutorDetail,
  TutorSearchParams,
  SpringPage,
  TutorMatchRequest,
  TutorMatchResult,
} from "../types";

export const discoveryApi = {
  /**
   * Tìm kiếm gia sư công khai với bộ lọc đa tiêu chí và phân trang (GET /api/v1/discovery/tutors)
   */
  async searchTutors(params: TutorSearchParams = {}): Promise<ApiResponse<SpringPage<TutorDiscoveryCard>>> {
    const searchParams = new URLSearchParams();

    if (params.subjectId !== undefined) searchParams.set("subjectId", String(params.subjectId));
    if (params.levelId !== undefined) searchParams.set("levelId", String(params.levelId));
    if (params.area && params.area.trim()) searchParams.set("area", params.area.trim());
    if (params.mode) searchParams.set("mode", params.mode);
    if (params.minPrice !== undefined) searchParams.set("minPrice", String(params.minPrice));
    if (params.maxPrice !== undefined) searchParams.set("maxPrice", String(params.maxPrice));
    if (params.minRating !== undefined) searchParams.set("minRating", String(params.minRating));
    if (params.keyword && params.keyword.trim()) searchParams.set("keyword", params.keyword.trim());
    if (params.dayOfWeek !== undefined) searchParams.set("dayOfWeek", String(params.dayOfWeek));
    if (params.availableFrom) searchParams.set("availableFrom", params.availableFrom);
    if (params.availableTo) searchParams.set("availableTo", params.availableTo);
    if (params.page !== undefined) searchParams.set("page", String(params.page));
    if (params.size !== undefined) searchParams.set("size", String(params.size));
    if (params.sortBy) searchParams.set("sortBy", params.sortBy);
    if (params.sortDirection) searchParams.set("sortDirection", params.sortDirection);

    const qs = searchParams.toString();
    const endpoint = `/api/v1/discovery/tutors${qs ? `?${qs}` : ""}`;

    return fetchApi<SpringPage<TutorDiscoveryCard>>(endpoint, {
      method: "GET",
    });
  },

  /**
   * Lấy chi tiết hồ sơ công khai của gia sư đã xác minh KYC (GET /api/v1/discovery/tutors/{tutorId})
   */
  async getTutorDetail(tutorId: string): Promise<ApiResponse<TutorDetail>> {
    return fetchApi<TutorDetail>(`/api/v1/discovery/tutors/${encodeURIComponent(tutorId)}`, {
      method: "GET",
    });
  },

  /**
   * Thực hiện ghép cặp gia sư theo mục tiêu học tập (POST /api/v1/discovery/match)
   */
  async matchTutors(payload: TutorMatchRequest): Promise<ApiResponse<TutorMatchResult[]>> {
    return fetchApi<TutorMatchResult[]>("/api/v1/discovery/match", {
      method: "POST",
      body: JSON.stringify({
        goalId: payload.goalId,
        topN: payload.topN ?? 5,
      }),
    });
  },
};
