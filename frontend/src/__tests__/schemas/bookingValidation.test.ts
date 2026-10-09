import { describe, it, expect } from "vitest";
import { bookingFormSchema } from "@/lib/validation";
import bookingFixtures from "../fixtures/booking-form-fixtures.json";

describe("Booking Form Validation Schema (Data-Driven Testing)", () => {
  it.each(bookingFixtures.bookingCases)(
    "Kiểm tra đặt lịch: $desc -> valid: $valid",
    ({ payload, valid, expectedErrorField }) => {
      const result = bookingFormSchema.safeParse(payload);
      expect(result.success).toBe(valid);

      if (!valid && expectedErrorField) {
        const errorPaths = result.error?.issues.map((i) => i.path[0]);
        expect(errorPaths).toContain(expectedErrorField);
      }
    }
  );

  it("Từ chối ghi chú vượt quá 5000 ký tự (BVA)", () => {
    const longNotes = "a".repeat(5001);
    const result = bookingFormSchema.safeParse({
      tutorId: "tutor-uuid-001",
      subject: "Toán",
      startTime: "09:00",
      endTime: "11:00",
      notes: longNotes,
    });
    expect(result.success).toBe(false);
  });

  it("Chấp nhận ghi chú đúng 5000 ký tự (BVA)", () => {
    const maxNotes = "a".repeat(5000);
    const result = bookingFormSchema.safeParse({
      tutorId: "tutor-uuid-001",
      subject: "Toán",
      startTime: "09:00",
      endTime: "11:00",
      notes: maxNotes,
    });
    expect(result.success).toBe(true);
  });
});
