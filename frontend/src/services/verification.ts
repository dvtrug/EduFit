import { apiClient } from "@/lib/api";

export type VerificationStatus = "DRAFT" | "PENDING" | "VERIFIED" | "REJECTED";

export interface VerificationRequestDetail {
  id: string;
  tutorId: string;
  status: VerificationStatus;
  rejectionReason?: string;
  submittedAt?: string;
  reviewedAt?: string;
  documents?: Array<{
    id: string;
    documentType: string;
    fileUrl: string;
    fileName: string;
  }>;
}

export interface SubmitVerificationPayload {
  institutionName: string;
  graduationYear?: number;
  documentType: string;
  notes?: string;
}

export const verificationService = {
  /**
   * Lấy trạng thái hồ sơ xác minh mới nhất của gia sư
   */
  async getMyRequest(): Promise<VerificationRequestDetail | null> {
    try {
      const res = await apiClient.get<VerificationRequestDetail>("/api/v1/verifications/my-request");
      return (res.data || null) as VerificationRequestDetail | null;
    } catch {
      return null;
    }
  },

  /**
   * Nộp hồ sơ xác minh KYC (gửi Multipart form kèm file)
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
};
