export interface AvailabilitySlotLike {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface AvailabilityValidationResult {
  valid: boolean;
  message?: string;
}

export interface DayOfWeekOption {
  day: number;
  name: string;
}

export const DAYS_OF_WEEK: DayOfWeekOption[] = [
  { day: 1, name: "Thứ 2" },
  { day: 2, name: "Thứ 3" },
  { day: 3, name: "Thứ 4" },
  { day: 4, name: "Thứ 5" },
  { day: 5, name: "Thứ 6" },
  { day: 6, name: "Thứ 7" },
  { day: 7, name: "Chủ Nhật" },
];

export const DAY_LABELS: Record<number, string> = {
  1: "Thứ 2",
  2: "Thứ 3",
  3: "Thứ 4",
  4: "Thứ 5",
  5: "Thứ 6",
  6: "Thứ 7",
  7: "Chủ Nhật",
};

export interface SlotPreset {
  id: string;
  label: string;
  slots: AvailabilitySlotLike[];
}

export const SLOT_PRESETS: SlotPreset[] = [
  {
    id: "tue-fri-evening",
    label: "Tối Thứ 3 & Thứ 6 · 19:00–21:00",
    slots: [
      { dayOfWeek: 2, startTime: "19:00:00", endTime: "21:00:00" },
      { dayOfWeek: 5, startTime: "19:00:00", endTime: "21:00:00" },
    ],
  },
  {
    id: "mon-wed-evening",
    label: "Tối Thứ 2 & Thứ 4 · 19:00–21:00",
    slots: [
      { dayOfWeek: 1, startTime: "19:00:00", endTime: "21:00:00" },
      { dayOfWeek: 3, startTime: "19:00:00", endTime: "21:00:00" },
    ],
  },
  {
    id: "weekend-morning",
    label: "Sáng Thứ 7 & Chủ Nhật · 08:00–10:00",
    slots: [
      { dayOfWeek: 6, startTime: "08:00:00", endTime: "10:00:00" },
      { dayOfWeek: 7, startTime: "08:00:00", endTime: "10:00:00" },
    ],
  },
];

export function toMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

export function isValidTime(value: string): boolean {
  return /^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(value);
}

export function normalizeAvailabilityTime(value: string): string {
  return value.length === 5 ? `${value}:00` : value;
}

export function sortAvailabilitySlots<T extends AvailabilitySlotLike>(slots: T[]): T[] {
  return [...slots].sort((left, right) => {
    if (left.dayOfWeek !== right.dayOfWeek) {
      return left.dayOfWeek - right.dayOfWeek;
    }
    return toMinutes(left.startTime) - toMinutes(right.startTime);
  });
}

export function validateAvailabilitySlots(
  slots: AvailabilitySlotLike[]
): AvailabilityValidationResult {
  const sortedSlots = sortAvailabilitySlots(slots);

  for (let index = 0; index < sortedSlots.length; index += 1) {
    const current = sortedSlots[index];

    if (!isValidTime(current.startTime) || !isValidTime(current.endTime)) {
      return {
        valid: false,
        message: "Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc.",
      };
    }

    const start = toMinutes(current.startTime);
    const end = toMinutes(current.endTime);

    if (end <= start) {
      return {
        valid: false,
        message: "Giờ kết thúc phải sau giờ bắt đầu.",
      };
    }

    const next = sortedSlots[index + 1];
    if (
      next &&
      next.dayOfWeek === current.dayOfWeek &&
      toMinutes(next.startTime) < end
    ) {
      return {
        valid: false,
        message: "Các khoảng giờ trong cùng một ngày không được chồng lấn.",
      };
    }
  }

  return { valid: true };
}

export function formatMatchingSlots(slots: AvailabilitySlotLike[]): string {
  if (slots.length === 0) return "Chưa chọn khung giờ";
  const sorted = sortAvailabilitySlots(slots);
  const first = sorted[0];
  const sameTime = sorted.every(
    (slot) => slot.startTime === first.startTime && slot.endTime === first.endTime
  );
  const days = sorted.map((slot) => DAY_LABELS[slot.dayOfWeek] || `Ngày ${slot.dayOfWeek}`).join(", ");
  if (!sameTime) {
    return sorted
      .map(
        (slot) =>
          `${DAY_LABELS[slot.dayOfWeek] || `Ngày ${slot.dayOfWeek}`} ${slot.startTime.slice(0, 5)}–${slot.endTime.slice(0, 5)}`
      )
      .join(" · ");
  }
  return `${days} · ${first.startTime.slice(0, 5)}–${first.endTime.slice(0, 5)}`;
}
