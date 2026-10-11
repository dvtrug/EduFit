import { z } from "zod";
export {
  registerSchema,
  loginSchema,
  changePasswordSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  evaluatePasswordCriteria,
  type RegisterInput,
  type LoginInput,
  type ForgotPasswordInput,
  type ResetPasswordInput,
  type ChangePasswordInput,
  type PasswordCriteriaResult,
} from "@/features/auth/schemas/authSchemas";

import {
  registerSchema,
  loginSchema,
  changePasswordSchema,
} from "@/features/auth/schemas/authSchemas";

/**
 * Backward compatibility types
 */
export type RegisterSchemaInput = z.infer<typeof registerSchema>;
export type LoginSchemaInput = z.infer<typeof loginSchema>;
export type ChangePasswordSchemaInput = z.infer<typeof changePasswordSchema>;

/**
 * Schema đặt lịch học gia sư (Scheduling / Booking Form - BR-41..48)
 */
export const bookingFormSchema = z.object({
  tutorId: z.string().min(1, "Vui lòng chọn gia sư"),
  subject: z.string().min(1, "Vui lòng chọn môn học"),
  startTime: z.string().regex(/^([01]\d|2[0-3]):([0-5]\d)$/, "Định dạng giờ bắt đầu không hợp lệ (HH:mm)"),
  endTime: z.string().regex(/^([01]\d|2[0-3]):([0-5]\d)$/, "Định dạng giờ kết thúc không hợp lệ (HH:mm)"),
  notes: z.string().max(5000, "Ghi chú không được vượt quá 5000 ký tự").optional().default(""),
});

export type BookingFormSchemaInput = z.infer<typeof bookingFormSchema>;
