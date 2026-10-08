import { apiClient, ApiError } from "@/lib/api";

export type VerificationStatus = "DRAFT" | "PENDING" | "APPROVED" | "VERIFIED" | "REJECTED";

export interface CredentialFileResponse {
  fileId: string;
  fileName: string;
  storageKey: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface CredentialResponse {
  credentialId: string;
  type: "DEGREE" | "CERTIFICATE" | "IDENTITY_CARD";
  institution: string;
  year?: number;
  note?: string;
  files: CredentialFileResponse[];
}

export interface VerificationRequestDetail {
  requestId: string;
  tutorId: string;
  status: VerificationStatus;
  rejectionReason?: string;
  submittedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  credentials: CredentialResponse[];
}

export interface PendingQueueItem {
  requestId: string;
  tutorId: string;
  status: string;
  submittedAt: string;
}

export interface PendingQueueResponse {
  items: PendingQueueItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SubmitVerificationPayload {
  credentials: Array<{
    type: "DEGREE" | "CERTIFICATE" | "IDENTITY_CARD";
    institution: string;
    year?: number;
    note?: string;
    fileIndexes: number[];
  }>;
}

export const verificationService = {
  /**
   * Lấy trạng thái hồ sơ xác minh mới nhất của gia sư (TUTOR)
   */
  async getMyRequest(): Promise<VerificationRequestDetail | null> {
    try {
      const res = await apiClient.get<VerificationRequestDetail>("/api/v1/verifications/my-request");
      return (res.data || null) as VerificationRequestDetail | null;
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        return null;
      }
      throw error;
    }
  },

  /**
   * Nộp hồ sơ xác minh KYC kèm tệp tin thật (TUTOR)
   */
  async submitVerification(payload: SubmitVerificationPayload, files: File[]): Promise<VerificationRequestDetail> {
    const formData = new FormData();
    const payloadBlob = new Blob([JSON.stringify(payload)], { type: "application/json" });
    formData.append("payload", payloadBlob);

    files.forEach((file) => {
      formData.append("files", file);
    });

    const res = await apiClient.post<VerificationRequestDetail>("/api/v1/verifications", formData);
    return res.data as VerificationRequestDetail;
  },

  /**
   * Lấy hàng đợi hồ sơ KYC đang chờ duyệt (ADMIN)
   */
  async getPendingQueue(page = 0, size = 20): Promise<PendingQueueResponse> {
    const res = await apiClient.get<PendingQueueResponse>(
      `/api/v1/admin/verifications/pending?page=${page}&size=${size}`
    );
    return res.data as PendingQueueResponse;
  },

  /**
   * Duyệt hoặc từ chối hồ sơ xác minh của gia sư (ADMIN)
   */
  async reviewVerification(
    requestId: string,
    decision: "APPROVED" | "REJECTED",
    rejectionReason?: string
  ): Promise<VerificationRequestDetail> {
    const res = await apiClient.post<VerificationRequestDetail>(
      `/api/v1/admin/verifications/${requestId}/review`,
      {
        decision,
        rejectionReason: rejectionReason || undefined,
      }
    );
    return res.data as VerificationRequestDetail;
  },
};
