import React from "react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { AdminKycQueueView } from "@/features/verification/components/AdminKycQueueView";
import { verificationService } from "@/services/verification";
import { ToastProvider } from "@/shared/ui/Toast";

vi.mock("@/services/verification", () => ({
  verificationService: {
    getPendingQueue: vi.fn(),
    getVerificationDetail: vi.fn(),
    reviewVerification: vi.fn(),
    getCredentialFileUrl: vi.fn((fileId: string) => `/api/v1/verifications/credentials/files/${fileId}`),
  },
}));

describe("AdminKycQueueView Component Tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("hiển thị trạng thái đang tải (Loading) khi mới vào trang", () => {
    vi.mocked(verificationService.getPendingQueue).mockReturnValue(new Promise(() => {}));

    render(
      <ToastProvider>
        <AdminKycQueueView />
      </ToastProvider>
    );

    expect(screen.getByText("Đang tải danh sách hàng đợi từ máy chủ...")).toBeInTheDocument();
  });

  it("hiển thị EmptyState khi không có hồ sơ nào trong hàng đợi", async () => {
    vi.mocked(verificationService.getPendingQueue).mockResolvedValue({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    render(
      <ToastProvider>
        <AdminKycQueueView />
      </ToastProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("Không có hồ sơ nào đang chờ duyệt")).toBeInTheDocument();
    });
    expect(screen.getByText("HÀNG ĐỢI HOÀN TẤT")).toBeInTheDocument();
  });

  it("hiển thị bảng danh sách hồ sơ khi có dữ liệu và mở modal khi bấm Thẩm định", async () => {
    const mockItems = [
      {
        requestId: "req-123456",
        tutorId: "tutor-999",
        status: "PENDING",
        submittedAt: "2026-10-14T08:00:00Z",
      },
    ];

    vi.mocked(verificationService.getPendingQueue).mockResolvedValue({
      items: mockItems,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

    vi.mocked(verificationService.getVerificationDetail).mockResolvedValue({
      requestId: "req-123456",
      tutorId: "tutor-999",
      status: "PENDING",
      credentials: [
        {
          credentialId: "c1",
          type: "DEGREE",
          institution: "Đại học Sư phạm",
          year: 2023,
          note: "Loại giỏi",
          files: [
            {
              fileId: "f1",
              fileName: "bang_dh.pdf",
              storageKey: "k1",
              contentType: "application/pdf",
              sizeBytes: 2048,
              uploadedAt: "2026-10-14T08:00:00Z",
            },
          ],
        },
      ],
    });

    render(
      <ToastProvider>
        <AdminKycQueueView />
      </ToastProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("req-123456")).toBeInTheDocument();
      expect(screen.getByText("tutor-999")).toBeInTheDocument();
      expect(screen.getByText("PENDING")).toBeInTheDocument();
    });

    // Bấm nút thẩm định
    const reviewButton = screen.getByRole("button", { name: /Thẩm định/i });
    fireEvent.click(reviewButton);

    // Modal mở ra
    await waitFor(() => {
      expect(screen.getByText("Thẩm Định Hồ Sơ Xác Minh KYC")).toBeInTheDocument();
      expect(screen.getByText(/Mã yêu cầu: req-123456/)).toBeInTheDocument();
    });
  });
});
