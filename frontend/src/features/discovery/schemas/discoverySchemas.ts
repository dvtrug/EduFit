import { z } from "zod";

export const tutorSearchFilterSchema = z.object({
  subjectId: z.number().int().positive().optional(),
  levelId: z.number().int().positive().optional(),
  area: z.string().trim().optional(),
  mode: z.enum(["ONLINE", "OFFLINE", "BOTH"]).optional(),
  minPrice: z.number().nonnegative().optional(),
  maxPrice: z.number().nonnegative().optional(),
  minRating: z.number().min(0).max(5).optional(),
  keyword: z.string().trim().optional(),
  dayOfWeek: z.number().int().min(1).max(7).optional(),
  availableFrom: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Giờ bắt đầu không đúng định dạng (HH:mm)").optional(),
  availableTo: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Giờ kết thúc không đúng định dạng (HH:mm)").optional(),
  page: z.number().int().min(0).default(0),
  size: z.number().int().min(1).max(100).default(10),
  sortBy: z.enum(["rating", "relevance", "price", "experience"]).default("rating"),
  sortDirection: z.enum(["asc", "desc"]).default("desc"),
}).refine((data) => {
  if (data.minPrice !== undefined && data.maxPrice !== undefined) {
    return data.minPrice <= data.maxPrice;
  }
  return true;
}, {
  message: "Học phí tối thiểu không được lớn hơn học phí tối đa",
  path: ["minPrice"],
});

export const tutorMatchRequestSchema = z.object({
  goalId: z.string().uuid("Mã mục tiêu học tập (goalId) phải là định dạng UUID hợp lệ"),
  topN: z.number().int().min(1).max(10).default(5),
});

export type ValidatedSearchFilter = z.infer<typeof tutorSearchFilterSchema>;
export type ValidatedMatchRequest = z.infer<typeof tutorMatchRequestSchema>;
