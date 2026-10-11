import { describe, it, expect } from "vitest";
import {
  credentialUploadSchema,
  kycReviewSchema,
  validateCredentialFile,
  MAX_FILE_SIZE,
} from "@/features/verification/schemas/kycSchemas";

describe("KYC Zod Schemas & File Validation Tests", () => {
  describe("credentialUploadSchema", () => {
    it("chấp nhận thông tin nộp bằng cấp hợp lệ", () => {
      const result = credentialUploadSchema.safeParse({
        documentType: "DEGREE",
        institutionName: "Đại học Sư phạm Hà Nội",
        graduationYear: 2023,
        notes: "Tốt nghiệp loại Giỏi",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.institutionName).toBe("Đại học Sư phạm Hà Nội");
        expect(result.data.graduationYear).toBe(2023);
      }
    });

    it("từ chối khi tên trường dưới 2 ký tự", () => {
      const result = credentialUploadSchema.safeParse({
        documentType: "DEGREE",
        institutionName: "A",
        graduationYear: 2023,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe(
          "Tên trường / đơn vị cấp bằng tối thiểu 2 ký tự"
        );
      }
    });

    it("từ chối khi năm tốt nghiệp trước 1970", () => {
      const result = credentialUploadSchema.safeParse({
        documentType: "CERTIFICATE",
        institutionName: "IELTS British Council",
        graduationYear: 1960,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe(
          "Năm tốt nghiệp không thể trước 1970"
        );
      }
    });
  });

  describe("kycReviewSchema", () => {
    it("chấp nhận phê duyệt hồ sơ (VERIFIED)", () => {
      const result = kycReviewSchema.safeParse({
        decision: "VERIFIED",
      });
      expect(result.success).toBe(true);
    });

    it("chấp nhận từ chối hồ sơ kèm lý do đầy đủ", () => {
      const result = kycReviewSchema.safeParse({
        decision: "REJECTED",
        rejectionReason: "Ảnh chụp bằng cấp bị mờ, không rõ con dấu",
      });
      expect(result.success).toBe(true);
    });

    it("từ chối khi REJECTED nhưng thiếu lý do hoặc lý do quá ngắn", () => {
      const result = kycReviewSchema.safeParse({
        decision: "REJECTED",
        rejectionReason: "Mờ",
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe(
          "Vui lòng nhập lý do từ chối rõ ràng (tối thiểu 5 ký tự)"
        );
      }
    });
  });

  describe("validateCredentialFile", () => {
    it("chấp nhận tệp PDF, JPG, PNG hợp lệ và dung lượng < 10MB", () => {
      expect(validateCredentialFile({ name: "bang_dai_hoc.pdf", size: 1024 * 1024 }).valid).toBe(true);
      expect(validateCredentialFile({ name: "chung_chi.jpg", size: 2 * 1024 * 1024 }).valid).toBe(true);
      expect(validateCredentialFile({ name: "cccd.png", size: 500 * 1024 }).valid).toBe(true);
    });

    it("từ chối định dạng tệp không được hỗ trợ", () => {
      const res = validateCredentialFile({ name: "virus.exe", size: 1024 });
      expect(res.valid).toBe(false);
      expect(res.error).toContain("không đúng định dạng PDF, JPG hoặc PNG");
    });

    it("từ chối tệp vượt quá 10MB", () => {
      const res = validateCredentialFile({ name: "tai_lieu_lon.pdf", size: MAX_FILE_SIZE + 1 });
      expect(res.valid).toBe(false);
      expect(res.error).toContain("vượt quá giới hạn 10 MB");
    });
  });
});
