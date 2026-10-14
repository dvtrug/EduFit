import { apiClient } from "@/lib/api";
import type { ConnectionRequest, SendConnectionRequestPayload, StudentLink } from "../types";

export const connectionApi = {
  async getLinks(): Promise<StudentLink[]> {
    const res = await apiClient.get<StudentLink[]>("/api/v1/connections/parent-student/links");
    return (res.data || []).filter((link) => link.status === "CONFIRMED");
  },

  async getSentRequests(studentId?: string): Promise<ConnectionRequest[]> {
    const query = studentId ? `?studentId=${encodeURIComponent(studentId)}` : "";
    const res = await apiClient.get<ConnectionRequest[]>(`/api/v1/connections/requests${query}`);
    return res.data || [];
  },

  async sendRequest(payload: SendConnectionRequestPayload): Promise<ConnectionRequest> {
    const res = await apiClient.post<ConnectionRequest>("/api/v1/connections/requests", payload);
    return res.data as ConnectionRequest;
  },
};
