import { z } from "zod";

/**
 * Schema xác thực đăng ký tài khoản (UC1.1 - BR-01, BR-02, BR-03)
 */
export const registerSchema = z.object({
  email: z.string().trim().toLowerCase().email("Email không hợp lệ"),
  password: z
    .string()
    .min(8, "Mật khẩu tối thiểu 8 ký tự")
    .max(64, "Mật khẩu tối đa 64 ký tự")
    .regex(/[A-Za-z]/, "Phải chứa ít nhất 1 chữ cái")
    .regex(/[0-9]/, "Phải chứa ít nhất 1 chữ số"),
  fullName: z.string().trim().min(2, "Họ tên quá ngắn"),
  role: z.enum(["STUDENT", "PARENT", "TUTOR"]),
});

/**
 * Schema xác thực đăng nhập (UC1.2 - NFR-06)
 */
export const loginSchema = z.object({
  email: z.string().trim().toLowerCase().email("Email không hợp lệ"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});

/**
 * Schema đổi mật khẩu (UC1.4 - BR-02)
 */
export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
  newPassword: z
    .string()
    .min(8, "Mật khẩu tối thiểu 8 ký tự")
    .max(64, "Mật khẩu tối đa 64 ký tự")
    .regex(/[A-Za-z]/, "Phải chứa ít nhất 1 chữ cái")
    .regex(/[0-9]/, "Phải chứa ít nhất 1 chữ số"),
});

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

export type RegisterSchemaInput = z.infer<typeof registerSchema>;
export type LoginSchemaInput = z.infer<typeof loginSchema>;
export type ChangePasswordSchemaInput = z.infer<typeof changePasswordSchema>;
export type BookingFormSchemaInput = z.infer<typeof bookingFormSchema>;
