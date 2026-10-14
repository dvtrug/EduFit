export interface ConnectionRequest {
  requestId: string;
  studentId: string;
  tutorId: string;
  goalId: string;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED" | "EXPIRED" | string;
  createdAt: string;
  expiresAt: string;
}

export interface StudentLink {
  linkId: string;
  studentId: string;
  status: string;
}

export interface SendConnectionRequestPayload {
  goalId: string;
  tutorId: string;
  desiredSlots: string[];
  message?: string;
}
