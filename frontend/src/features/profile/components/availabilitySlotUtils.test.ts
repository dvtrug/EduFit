import { describe, it, expect } from "vitest";
import {
  normalizeAvailabilityTime,
  sortAvailabilitySlots,
  validateAvailabilitySlots,
} from "./availabilitySlotUtils";

describe("availabilitySlotUtils", () => {
  it("normalizeAvailabilityTime converts an HH:mm value to the API format", () => {
    expect(normalizeAvailabilityTime("08:30")).toBe("08:30:00");
    expect(normalizeAvailabilityTime("19:00:00")).toBe("19:00:00");
  });

  it("validateAvailabilitySlots accepts adjacent ranges in the same day", () => {
    const result = validateAvailabilitySlots([
      { dayOfWeek: 1, startTime: "08:00", endTime: "09:30" },
      { dayOfWeek: 1, startTime: "09:30", endTime: "11:00" },
    ]);
    expect(result).toEqual({ valid: true });
  });

  it("validateAvailabilitySlots rejects an end time that is not after its start time", () => {
    const result = validateAvailabilitySlots([
      { dayOfWeek: 3, startTime: "10:00", endTime: "09:30" },
    ]);
    expect(result.valid).toBe(false);
    expect(result.message ?? "").toMatch(/sau giờ bắt đầu/i);
  });

  it("validateAvailabilitySlots rejects a range with a missing time", () => {
    const result = validateAvailabilitySlots([
      { dayOfWeek: 3, startTime: "", endTime: "10:30" },
    ]);
    expect(result.valid).toBe(false);
    expect(result.message ?? "").toMatch(/đầy đủ/i);
  });

  it("validateAvailabilitySlots rejects overlapping ranges in the same day", () => {
    const result = validateAvailabilitySlots([
      { dayOfWeek: 5, startTime: "08:00", endTime: "10:00" },
      { dayOfWeek: 5, startTime: "09:30", endTime: "11:00" },
    ]);
    expect(result.valid).toBe(false);
    expect(result.message ?? "").toMatch(/chồng lấn/i);
  });

  it("sortAvailabilitySlots orders ranges by weekday and start time", () => {
    const result = sortAvailabilitySlots([
      { dayOfWeek: 4, startTime: "19:00", endTime: "21:00" },
      { dayOfWeek: 2, startTime: "14:00", endTime: "16:00" },
      { dayOfWeek: 2, startTime: "08:00", endTime: "10:00" },
    ]);
    expect(result.map((slot) => `${slot.dayOfWeek}-${slot.startTime}`)).toEqual([
      "2-08:00",
      "2-14:00",
      "4-19:00",
    ]);
  });
});
