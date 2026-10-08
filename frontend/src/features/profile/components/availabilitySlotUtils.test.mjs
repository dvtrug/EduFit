import assert from "node:assert/strict";
import test from "node:test";

import {
  normalizeAvailabilityTime,
  sortAvailabilitySlots,
  validateAvailabilitySlots,
} from "./availabilitySlotUtils.ts";

test("normalizeAvailabilityTime converts an HH:mm value to the API format", () => {
  assert.equal(normalizeAvailabilityTime("08:30"), "08:30:00");
  assert.equal(normalizeAvailabilityTime("19:00:00"), "19:00:00");
});

test("validateAvailabilitySlots accepts adjacent ranges in the same day", () => {
  const result = validateAvailabilitySlots([
    { dayOfWeek: 1, startTime: "08:00", endTime: "09:30" },
    { dayOfWeek: 1, startTime: "09:30", endTime: "11:00" },
  ]);

  assert.deepEqual(result, { valid: true });
});

test("validateAvailabilitySlots rejects an end time that is not after its start time", () => {
  const result = validateAvailabilitySlots([
    { dayOfWeek: 3, startTime: "10:00", endTime: "09:30" },
  ]);

  assert.equal(result.valid, false);
  assert.match(result.message ?? "", /sau giờ bắt đầu/i);
});

test("validateAvailabilitySlots rejects a range with a missing time", () => {
  const result = validateAvailabilitySlots([
    { dayOfWeek: 3, startTime: "", endTime: "10:30" },
  ]);

  assert.equal(result.valid, false);
  assert.match(result.message ?? "", /đầy đủ/i);
});

test("validateAvailabilitySlots rejects overlapping ranges in the same day", () => {
  const result = validateAvailabilitySlots([
    { dayOfWeek: 5, startTime: "08:00", endTime: "10:00" },
    { dayOfWeek: 5, startTime: "09:30", endTime: "11:00" },
  ]);

  assert.equal(result.valid, false);
  assert.match(result.message ?? "", /chồng lấn/i);
});

test("sortAvailabilitySlots orders ranges by weekday and start time", () => {
  const result = sortAvailabilitySlots([
    { dayOfWeek: 4, startTime: "19:00", endTime: "21:00" },
    { dayOfWeek: 2, startTime: "14:00", endTime: "16:00" },
    { dayOfWeek: 2, startTime: "08:00", endTime: "10:00" },
  ]);

  assert.deepEqual(
    result.map((slot) => `${slot.dayOfWeek}-${slot.startTime}`),
    ["2-08:00", "2-14:00", "4-19:00"]
  );
});
