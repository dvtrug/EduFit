export interface AvailabilitySlotLike {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface AvailabilityValidationResult {
  valid: boolean;
  message?: string;
}

function toMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function isValidTime(value: string): boolean {
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
