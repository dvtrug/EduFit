import { z } from "zod";

export const ALLOWED_EXTENSIONS = [".pdf", ".jpg", ".jpeg", ".png"] as const;
export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

export const credentialTypeSchema = z.enum([
  "DEGREE",
  "CERTIFICATE",
  "IDENTITY_CARD",
]);

export const credentialUploadSchema = z.object({
  documentType: credentialTypeSchema,
  institutionName: z
    .string()
    .trim()
    .min(2, "Tên trường / đơn vị cấp bằng tối thiểu 2 ký tự"),
  graduationYear: z
    .number({ invalid_type_error: "Năm tốt nghiệp phải là số" })
    .int("Năm tốt nghiệp phải là số nguyên")
    .min(1970, "Năm tốt nghiệp không thể trước 1970")
    .max(new Date().getFullYear() + 10, "Năm tốt nghiệp không hợp lệ"),
  notes: z
    .string()
    .trim()
    .max(500, "Ghi chú không được vượt quá 500 ký tự")
    .optional(),
});

export const kycReviewSchema = z
  .object({
    decision: z.enum(["VERIFIED", "REJECTED"]),
    rejectionReason: z.string().trim().optional(),
  })
  .refine(
    (data) => {
      if (data.decision === "REJECTED") {
        return Boolean(data.rejectionReason && data.rejectionReason.length >= 5);
      }
      return true;
    },
    {
      message: "Vui lòng nhập lý do từ chối rõ ràng (tối thiểu 5 ký tự)",
      path: ["rejectionReason"],
    }
  );

export function validateCredentialFile(file: { name: string; size: number }): {
  valid: boolean;
  error?: string;
} {
  const lowerName = file.name.toLowerCase();
  const hasValidExt = ALLOWED_EXTENSIONS.some((ext) => lowerName.endsWith(ext));
  if (!hasValidExt) {
    return {
      valid: false,
      error: `Tệp ${file.name} không đúng định dạng PDF, JPG hoặc PNG.`,
    };
  }
  if (file.size > MAX_FILE_SIZE) {
    return {
      valid: false,
      error: `Tệp ${file.name} vượt quá giới hạn 10 MB.`,
    };
  }
  return { valid: true };
}
