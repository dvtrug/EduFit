"use client";

import React from "react";
import { Clock, Check } from "lucide-react";
import { TutorAvailabilitySlot } from "@/services/profile";

/**
 * =========================================================================
 * SPRING BOOT API CONNECTION SPECIFICATION:
 * =========================================================================
 * Endpoint: PUT /api/v1/profiles/tutors/me/availability-slots
 * Controller: vn.edufit.profile.web.ProfileController#setAvailabilitySlots
 * Request Body: SetAvailabilitySlotsRequest { slots: List<SlotItem> }
 * Constraint: dayOfWeek: Short (1 = Thứ Hai ... 7 = Chủ Nhật)
 *             startTime: LocalTime, endTime: LocalTime
 * =========================================================================
 */

export interface MatrixTimeSlot {
  id: string; // e.g. "MORNING", "AFTERNOON", "EVENING"
  label: string;
  startTime: string; // "08:00:00"
  endTime: string;   // "11:30:00"
  badge: string;
}

export const TIME_SLOT_DEFINITIONS: MatrixTimeSlot[] = [
  { id: "MORNING", label: "Ca Sáng", startTime: "08:00:00", endTime: "11:30:00", badge: "08:00 - 11:30" },
  { id: "AFTERNOON", label: "Ca Chiều", startTime: "14:00:00", endTime: "17:30:00", badge: "14:00 - 17:30" },
  { id: "EVENING", label: "Ca Tối", startTime: "18:30:00", endTime: "21:00:00", badge: "18:30 - 21:00" },
];

export const DAYS_OF_WEEK = [
  { day: 1, name: "Thứ 2", short: "T2" },
  { day: 2, name: "Thứ 3", short: "T3" },
  { day: 3, name: "Thứ 4", short: "T4" },
  { day: 4, name: "Thứ 5", short: "T5" },
  { day: 5, name: "Thứ 6", short: "T6" },
  { day: 6, name: "Thứ 7", short: "T7" },
  { day: 7, name: "Chủ Nhật", short: "CN" },
];

interface AvailabilitySlotPickerProps {
  slots: TutorAvailabilitySlot[];
  onChange: (slots: TutorAvailabilitySlot[]) => void;
}

export function AvailabilitySlotPicker({ slots, onChange }: AvailabilitySlotPickerProps) {
  // Kiểm tra xem một ca cụ thể có đang được bật hay không
  const isSelected = (day: number, timeDef: MatrixTimeSlot): boolean => {
    return slots.some(
      (s) => s.dayOfWeek === day && s.startTime.startsWith(timeDef.startTime.substring(0, 5))
    );
  };

  // Bật/tắt ca học trong lưới ma trận
  const toggleSlot = (day: number, timeDef: MatrixTimeSlot) => {
    const existing = isSelected(day, timeDef);
    if (existing) {
      // Bỏ chọn ca này
      const filtered = slots.filter(
        (s) => !(s.dayOfWeek === day && s.startTime.startsWith(timeDef.startTime.substring(0, 5)))
      );
      onChange(filtered);
    } else {
      // Thêm ca mới vào danh sách
      const newSlot: TutorAvailabilitySlot = {
        dayOfWeek: day,
        startTime: timeDef.startTime,
        endTime: timeDef.endTime,
      };
      onChange([...slots, newSlot]);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <Clock className="size-4 text-amber-600" />
          <h4 className="text-xs font-bold text-neutral-900 uppercase tracking-wider">
            Lưới Lịch Rảnh Giảng Dạy (Availability Matrix)
          </h4>
        </div>
        <span className="text-[11px] text-neutral-500 font-medium">
          Đã chọn: <strong className="text-amber-600 font-bold">{slots.length}</strong> khung giờ trong tuần
        </span>
      </div>

      <div className="border border-stone-200/90 rounded-2xl overflow-hidden bg-white shadow-2xs">
        {/* Header 7 ngày trong tuần */}
        <div className="grid grid-cols-8 border-b border-stone-200 bg-stone-50/80 text-center text-xs font-bold text-neutral-700">
          <div className="p-2.5 sm:p-3 border-r border-stone-200 text-neutral-400 font-semibold text-[11px] flex items-center justify-center">
            Ca học
          </div>
          {DAYS_OF_WEEK.map((d) => (
            <div key={d.day} className="p-2.5 sm:p-3 border-r last:border-r-0 border-stone-200">
              <span className="hidden sm:inline">{d.name}</span>
              <span className="sm:hidden">{d.short}</span>
            </div>
          ))}
        </div>

        {/* 3 Hàng Ca Học: Sáng, Chiều, Tối */}
        {TIME_SLOT_DEFINITIONS.map((timeDef, idx) => (
          <div
            key={timeDef.id}
            className={`grid grid-cols-8 items-center text-center text-xs ${
              idx !== TIME_SLOT_DEFINITIONS.length - 1 ? "border-b border-stone-100" : ""
            }`}
          >
            {/* Cột nhãn Ca học */}
            <div className="p-2 sm:p-3 border-r border-stone-200 bg-stone-50/40 flex flex-col items-center justify-center">
              <span className="font-bold text-neutral-800 text-xs">{timeDef.label}</span>
              <span className="text-[10px] text-neutral-400 font-medium hidden sm:inline">
                {timeDef.badge}
              </span>
            </div>

            {/* 7 Ô Checkbox tương tác của 7 ngày */}
            {DAYS_OF_WEEK.map((d) => {
              const active = isSelected(d.day, timeDef);
              return (
                <div
                  key={d.day}
                  className="p-1.5 sm:p-2 border-r last:border-r-0 border-stone-100 flex items-center justify-center"
                >
                  <button
                    type="button"
                    onClick={() => toggleSlot(d.day, timeDef)}
                    aria-label={`${timeDef.label} - ${d.name}`}
                    className={`size-8 sm:size-9 rounded-xl flex items-center justify-center transition-all ${
                      active
                        ? "bg-amber-500 text-white font-bold shadow-xs hover:bg-amber-600 scale-95"
                        : "bg-stone-50 hover:bg-stone-100 text-neutral-300 border border-stone-200/60 hover:border-amber-300"
                    }`}
                  >
                    {active ? (
                      <Check className="size-4 stroke-[3]" />
                    ) : (
                      <span className="text-[10px] text-neutral-400 opacity-60">✕</span>
                    )}
                  </button>
                </div>
              );
            })}
          </div>
        ))}
      </div>

      <div className="flex items-center gap-4 text-[11px] text-neutral-500 px-1">
        <div className="flex items-center gap-1.5">
          <div className="size-3 rounded-md bg-amber-500" />
          <span>Khung giờ có thể nhận lịch dạy</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="size-3 rounded-md bg-stone-100 border border-stone-200" />
          <span>Khung giờ bận</span>
        </div>
      </div>
    </div>
  );
}
