"use client";

import React, { useMemo, useState } from "react";
import { CalendarClock, ChevronLeft, ChevronRight, Clock3, Handshake } from "lucide-react";
import { TutorAvailabilitySlot } from "@/services/profile";
import { UserRole } from "@/shared/ui/Sidebar";

interface ScheduleCalendarViewProps {
  role: UserRole;
  availabilitySlots?: TutorAvailabilitySlot[];
}

const HOURS = Array.from({ length: 15 }, (_, index) => index + 7);
const DAY_LABELS = ["Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"];

function startOfWeek(date: Date) {
  const result = new Date(date);
  const currentDay = result.getDay() || 7;
  result.setDate(result.getDate() - currentDay + 1);
  result.setHours(0, 0, 0, 0);
  return result;
}

function addDays(date: Date, days: number) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function toMinutes(value: string) {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function formatDate(date: Date) {
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(date);
}

export function ScheduleCalendarView({ role, availabilitySlots = [] }: ScheduleCalendarViewProps) {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const weekDays = useMemo(
    () => DAY_LABELS.map((label, index) => ({ label, date: addDays(weekStart, index) })),
    [weekStart]
  );

  const weekEnd = weekDays[6].date;
  const roleDescription = role === "TUTOR"
    ? "Lịch rảnh định kỳ của bạn được tô xanh. Buổi học chỉ trở thành lịch chính thức sau khi bên còn lại xác nhận."
    : "Buổi học chỉ được đưa vào lịch chính thức sau khi gia sư và học viên hoặc phụ huynh cùng xác nhận.";

  const isAvailable = (dayOfWeek: number, hour: number) => availabilitySlots.some((slot) => {
    const start = toMinutes(slot.startTime);
    const end = toMinutes(slot.endTime);
    const cellStart = hour * 60;
    return slot.dayOfWeek === dayOfWeek && start <= cellStart && end > cellStart;
  });

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-2 inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1 text-xs font-bold text-sky-800">
            <CalendarClock className="size-3.5" />
            Múi giờ Việt Nam UTC+7
          </div>
          <h1 className="font-heading text-2xl font-extrabold tracking-tight text-neutral-900 sm:text-3xl">
            Lịch học và đề xuất thời gian
          </h1>
          <p className="mt-1 max-w-3xl text-sm text-neutral-500">{roleDescription}</p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setWeekStart(startOfWeek(new Date()))}
            className="h-10 rounded-xl border border-stone-200 bg-white px-4 text-xs font-bold text-neutral-700 hover:bg-stone-50"
          >
            Hôm nay
          </button>
          <div className="flex h-10 items-center rounded-xl border border-stone-200 bg-white">
            <button
              type="button"
              onClick={() => setWeekStart((current) => addDays(current, -7))}
              className="grid size-10 place-items-center rounded-l-xl text-neutral-500 hover:bg-stone-50"
              aria-label="Tuần trước"
            >
              <ChevronLeft className="size-4" />
            </button>
            <span className="min-w-32 px-2 text-center text-xs font-bold text-neutral-800">
              {formatDate(weekStart)} - {formatDate(weekEnd)}
            </span>
            <button
              type="button"
              onClick={() => setWeekStart((current) => addDays(current, 7))}
              className="grid size-10 place-items-center rounded-r-xl text-neutral-500 hover:bg-stone-50"
              aria-label="Tuần sau"
            >
              <ChevronRight className="size-4" />
            </button>
          </div>
        </div>
      </header>

      <section className="overflow-hidden rounded-3xl border border-stone-200 bg-white shadow-xs">
        <div className="overflow-x-auto">
          <div className="min-w-[860px]">
            <div className="grid grid-cols-[72px_repeat(7,minmax(108px,1fr))] border-b border-stone-200 bg-stone-50/80">
              <div className="border-r border-stone-200 p-3 text-center text-[10px] font-bold uppercase text-neutral-400">
                Giờ
              </div>
              {weekDays.map(({ label, date }) => (
                <div key={label} className="border-r border-stone-200 p-3 text-center last:border-r-0">
                  <div className="text-[11px] font-bold text-neutral-500">{label}</div>
                  <div className="mt-0.5 text-sm font-extrabold text-neutral-900">{formatDate(date)}</div>
                </div>
              ))}
            </div>

            {HOURS.map((hour) => (
              <div key={hour} className="grid min-h-14 grid-cols-[72px_repeat(7,minmax(108px,1fr))] border-b border-stone-100 last:border-b-0">
                <div className="border-r border-stone-200 px-2 py-2 text-right text-[11px] font-semibold text-neutral-400">
                  {String(hour).padStart(2, "0")}:00
                </div>
                {weekDays.map((day, index) => {
                  const available = role === "TUTOR" && isAvailable(index + 1, hour);
                  return (
                    <div
                      key={`${day.label}-${hour}`}
                      className={`border-r border-stone-100 p-1 last:border-r-0 ${available ? "bg-emerald-50" : "bg-white"}`}
                    >
                      {available && (
                        <div className="flex h-full items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-100/70 px-2 text-[10px] font-bold text-emerald-800">
                          <Clock3 className="size-3" /> Có thể dạy
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-3">
        {[
          ["1", "Đề xuất thời gian", "Một bên chọn giờ trong phạm vi lớp đã kết nối."],
          ["2", "Kiểm tra và phản hồi", "Hệ thống kiểm tra trùng lịch; bên còn lại xác nhận hoặc từ chối."],
          ["3", "Đưa vào lịch chính thức", "Chỉ lịch đã được hai bên đồng thuận mới xuất hiện như một buổi học."],
        ].map(([step, title, description]) => (
          <div key={step} className="rounded-2xl border border-stone-200 bg-white p-5">
            <div className="mb-3 flex items-center gap-2">
              <span className="grid size-7 place-items-center rounded-full bg-sky-100 text-xs font-black text-sky-800">{step}</span>
              <h2 className="text-sm font-bold text-neutral-900">{title}</h2>
            </div>
            <p className="text-xs leading-relaxed text-neutral-500">{description}</p>
          </div>
        ))}
      </section>

      <section className="rounded-3xl border border-dashed border-stone-300 bg-white px-6 py-10 text-center">
        <Handshake className="mx-auto size-8 text-neutral-300" />
        <h2 className="mt-3 text-sm font-bold text-neutral-900">Chưa có đề xuất lịch học</h2>
        <p className="mx-auto mt-1 max-w-xl text-xs leading-relaxed text-neutral-500">
          Khi có lớp 1-1 đang hoạt động, gia sư hoặc học viên có thể đề xuất thời gian. Lịch chỉ được xác nhận
          sau khi bên còn lại đồng ý và hệ thống kiểm tra không có xung đột.
        </p>
      </section>
    </div>
  );
}
