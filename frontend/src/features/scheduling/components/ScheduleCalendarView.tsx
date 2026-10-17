"use client";

import React, { useMemo, useState, useEffect, useCallback } from "react";
import {
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Calendar,
  ListFilter,
  CalendarPlus,
  RotateCw,
  AlertCircle,
  Video,
  MapPin,
} from "lucide-react";
import { schedulingApi } from "../api/schedulingApi";
import { ProposeSessionModal } from "./ProposeSessionModal";
import { SessionDetailModal } from "./SessionDetailModal";
import { ScheduleTimelineView } from "./ScheduleTimelineView";
import { useAuth } from "@/context/AuthContext";
import type { CalendarItem, ScheduleViewMode, ScheduleCalendarViewProps } from "../types";

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

function formatFullDate(date: Date) {
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

export function ScheduleCalendarView({
  role,
  availabilitySlots = [],
  classes = [],
  initialStudentId,
}: ScheduleCalendarViewProps) {
  const { user } = useAuth();

  // Current view mode: "grid" or "timeline"
  const [viewMode, setViewMode] = useState<ScheduleViewMode>("grid");

  // Selected week starting date
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));

  // Calendar sessions data state
  const [sessions, setSessions] = useState<CalendarItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Modals state
  const [isProposeOpen, setIsProposeOpen] = useState(false);
  const [proposeInitialDate, setProposeInitialDate] = useState<Date | undefined>(undefined);
  const [proposeInitialHour, setProposeInitialHour] = useState<number>(8);

  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  // 7 days of the selected week
  const weekDays = useMemo(
    () => DAY_LABELS.map((label, index) => ({ label, date: addDays(weekStart, index) })),
    [weekStart]
  );
  const weekEnd = weekDays[6].date;

  // End of week date at 23:59:59.999
  const weekEndDateTime = useMemo(() => {
    const end = new Date(weekDays[6].date);
    end.setHours(23, 59, 59, 999);
    return end;
  }, [weekDays]);

  // Fetch sessions from API
  const fetchSessions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await schedulingApi.getCalendar({
        from: weekStart.toISOString(),
        to: weekEndDateTime.toISOString(),
        studentId: initialStudentId,
      });
      if (res.success && res.data) {
        setSessions(res.data);
      } else {
        setSessions([]);
      }
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Không thể tải danh sách lịch học. Vui lòng thử lại sau."
      );
    } finally {
      setLoading(false);
    }
  }, [weekStart, weekEndDateTime, initialStudentId]);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  // Map availability slots (for Tutor)
  const isAvailable = (dayOfWeek: number, hour: number) =>
    availabilitySlots.some((slot) => {
      const start = toMinutes(slot.startTime);
      const end = toMinutes(slot.endTime);
      const cellStart = hour * 60;
      return slot.dayOfWeek === dayOfWeek && start <= cellStart && end > cellStart;
    });

  // Get sessions that belong to a specific day index (1-based: 1=Monday...7=Sunday) and hour
  const getSessionsForCell = (date: Date, hour: number): CalendarItem[] => {
    const targetDateStr = date.toISOString().slice(0, 10);
    return sessions.filter((s) => {
      if (!s.startAt) return false;
      const sDateStr = s.startAt.slice(0, 10);
      if (sDateStr !== targetDateStr) return false;
      const sessionDate = new Date(s.startAt);
      const sessionHour = sessionDate.getHours();
      return sessionHour === hour;
    });
  };

  // Open propose modal from clicking empty cell
  const handleCellClick = (date: Date, hour: number) => {
    setProposeInitialDate(date);
    setProposeInitialHour(hour);
    setIsProposeOpen(true);
  };

  // Open detail modal from clicking a session
  const handleSessionClick = (e: React.MouseEvent, sessionId: string) => {
    e.stopPropagation();
    setSelectedSessionId(sessionId);
    setIsDetailOpen(true);
  };

  const roleDescription =
    role === "TUTOR"
      ? "Lịch rảnh định kỳ của bạn được tô xanh. Buổi học chỉ chính thức sau khi hai bên cùng đồng thuận."
      : "Buổi học được bảo vệ chống trùng lịch và chỉ được kích hoạt khi hai bên cùng xác nhận.";

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-2 inline-flex items-center gap-2 rounded-full bg-sky-50 px-3 py-1 text-xs font-bold text-sky-800">
            <CalendarClock className="size-3.5" />
            Múi giờ Việt Nam UTC+7
          </div>
          <h1 className="font-heading text-2xl font-extrabold tracking-tight text-neutral-900 sm:text-3xl">
            Lịch học & Điều phối thời gian
          </h1>
          <p className="mt-1 max-w-3xl text-sm text-neutral-500">{roleDescription}</p>
        </div>

        {/* Toolbar: Navigation, View Toggle, and Propose Action */}
        <div className="flex flex-wrap items-center gap-2">
          {/* View mode toggle: Grid vs Timeline */}
          <div className="flex h-10 items-center rounded-xl border border-stone-200 bg-white p-0.5 shadow-2xs">
            <button
              type="button"
              onClick={() => setViewMode("grid")}
              className={`flex items-center gap-1.5 h-full px-3 rounded-lg text-xs font-bold transition-colors cursor-pointer ${
                viewMode === "grid"
                  ? "bg-sky-600 text-white shadow-xs"
                  : "text-neutral-600 hover:text-neutral-900"
              }`}
              title="Xem dạng Lưới lịch tuần"
            >
              <Calendar className="size-3.5" />
              Lưới tuần
            </button>
            <button
              type="button"
              onClick={() => setViewMode("timeline")}
              className={`flex items-center gap-1.5 h-full px-3 rounded-lg text-xs font-bold transition-colors cursor-pointer ${
                viewMode === "timeline"
                  ? "bg-sky-600 text-white shadow-xs"
                  : "text-neutral-600 hover:text-neutral-900"
              }`}
              title="Xem dạng Dòng thời gian"
            >
              <ListFilter className="size-3.5" />
              Dòng thời gian
            </button>
          </div>

          {/* Week Navigation */}
          <button
            type="button"
            onClick={() => setWeekStart(startOfWeek(new Date()))}
            className="h-10 rounded-xl border border-stone-200 bg-white px-3.5 text-xs font-bold text-neutral-700 hover:bg-stone-50 cursor-pointer shadow-2xs"
          >
            Hôm nay
          </button>

          <div className="flex h-10 items-center rounded-xl border border-stone-200 bg-white shadow-2xs">
            <button
              type="button"
              onClick={() => setWeekStart((current) => addDays(current, -7))}
              className="grid size-10 place-items-center rounded-l-xl text-neutral-500 hover:bg-stone-50 cursor-pointer"
              aria-label="Tuần trước"
            >
              <ChevronLeft className="size-4" />
            </button>
            <span className="min-w-32 px-2 text-center text-xs font-bold text-neutral-800">
              {formatDate(weekStart)} - {formatFullDate(weekEnd)}
            </span>
            <button
              type="button"
              onClick={() => setWeekStart((current) => addDays(current, 7))}
              className="grid size-10 place-items-center rounded-r-xl text-neutral-500 hover:bg-stone-50 cursor-pointer"
              aria-label="Tuần sau"
            >
              <ChevronRight className="size-4" />
            </button>
          </div>

          {/* Đề xuất lịch mới Button */}
          <button
            type="button"
            onClick={() => {
              setProposeInitialDate(undefined);
              setProposeInitialHour(8);
              setIsProposeOpen(true);
            }}
            className="flex h-10 items-center gap-2 rounded-xl bg-sky-600 px-4 text-xs font-bold text-white shadow-xs hover:bg-sky-700 cursor-pointer"
          >
            <CalendarPlus className="size-4" />
            Đề xuất lịch mới
          </button>
        </div>
      </header>

      {/* Error State Banner */}
      {error && (
        <div
          role="alert"
          className="flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-900"
        >
          <div className="flex items-center gap-2.5">
            <AlertCircle className="size-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={fetchSessions}
            className="flex items-center gap-1 rounded-lg bg-rose-100 px-2.5 py-1 font-bold text-rose-800 hover:bg-rose-200 cursor-pointer"
          >
            <RotateCw className="size-3" /> Thử lại
          </button>
        </div>
      )}

      {/* Main Content: Grid View or Timeline View */}
      {viewMode === "grid" ? (
        <section className="overflow-hidden rounded-3xl border border-stone-200 bg-white shadow-xs">
          <div className="overflow-x-auto">
            <div className="min-w-[880px]">
              {/* Header Days Row */}
              <div className="grid grid-cols-[72px_repeat(7,minmax(112px,1fr))] border-b border-stone-200 bg-stone-50/80">
                <div className="border-r border-stone-200 p-3 text-center text-[10px] font-bold uppercase text-neutral-400">
                  Giờ
                </div>
                {weekDays.map(({ label, date }) => {
                  const isToday =
                    date.toISOString().slice(0, 10) === new Date().toISOString().slice(0, 10);
                  return (
                    <div
                      key={label}
                      className={`border-r border-stone-200 p-3 text-center last:border-r-0 ${
                        isToday ? "bg-sky-50/50" : ""
                      }`}
                    >
                      <div
                        className={`text-[11px] font-bold ${
                          isToday ? "text-sky-700 font-extrabold" : "text-neutral-500"
                        }`}
                      >
                        {label} {isToday && "· Hôm nay"}
                      </div>
                      <div
                        className={`mt-0.5 text-sm font-extrabold ${
                          isToday ? "text-sky-900" : "text-neutral-900"
                        }`}
                      >
                        {formatDate(date)}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Loading Skeleton Rows */}
              {loading && sessions.length === 0 ? (
                <div className="p-12 text-center text-xs font-semibold text-neutral-400 animate-pulse">
                  Đang đồng bộ dữ liệu lịch học...
                </div>
              ) : (
                /* Hours Rows */
                HOURS.map((hour) => (
                  <div
                    key={hour}
                    className="grid min-h-16 grid-cols-[72px_repeat(7,minmax(112px,1fr))] border-b border-stone-100 last:border-b-0"
                  >
                    <div className="border-r border-stone-200 px-2 py-2 text-right text-[11px] font-semibold text-neutral-400">
                      {String(hour).padStart(2, "0")}:00
                    </div>

                    {weekDays.map((day, index) => {
                      const available = role === "TUTOR" && isAvailable(index + 1, hour);
                      const cellSessions = getSessionsForCell(day.date, hour);

                      return (
                        <div
                          key={`${day.label}-${hour}`}
                          onClick={() => cellSessions.length === 0 && handleCellClick(day.date, hour)}
                          className={`relative border-r border-stone-100 p-1 last:border-r-0 transition-colors group ${
                            available ? "bg-emerald-50/40" : "bg-white"
                          } ${cellSessions.length === 0 ? "hover:bg-sky-50/30 cursor-pointer" : ""}`}
                          title={
                            cellSessions.length === 0
                              ? `Bấm để đề xuất lịch lúc ${String(hour).padStart(2, "0")}:00 ${day.label}`
                              : undefined
                          }
                        >
                          {/* If availability slot */}
                          {available && cellSessions.length === 0 && (
                            <div className="absolute inset-1 pointer-events-none flex items-center justify-center opacity-60">
                              <span className="text-[9px] font-bold text-emerald-700 flex items-center gap-0.5">
                                <Clock3 className="size-2.5" /> Khung rảnh
                              </span>
                            </div>
                          )}

                          {/* Sessions inside this cell */}
                          {cellSessions.map((s) => {
                            const isProposed = s.status === "PROPOSED";
                            const isScheduled = s.status === "SCHEDULED";
                            const isCompleted = s.status === "COMPLETED";
                            const isCancelled = s.status === "CANCELLED";

                            const badgeColor = isScheduled
                              ? "border-sky-300 bg-sky-50 text-sky-900 hover:bg-sky-100"
                              : isProposed
                              ? "border-amber-300 bg-amber-50 text-amber-900 hover:bg-amber-100"
                              : isCompleted
                              ? "border-emerald-300 bg-emerald-50 text-emerald-900 hover:bg-emerald-100"
                              : isCancelled
                              ? "border-rose-300 bg-rose-50 text-rose-800 line-through opacity-70"
                              : "border-stone-300 bg-stone-50 text-stone-700";

                            return (
                              <button
                                key={s.sessionId}
                                type="button"
                                onClick={(e) => handleSessionClick(e, s.sessionId)}
                                className={`w-full rounded-xl border p-1.5 text-left text-[10px] font-bold shadow-2xs transition-all cursor-pointer ${badgeColor}`}
                              >
                                <div className="flex items-center justify-between">
                                  <span className="truncate">
                                    {s.startAt.slice(11, 16)} - {s.endAt.slice(11, 16)}
                                  </span>
                                  {s.mode === "ONLINE" ? (
                                    <Video className="size-3 shrink-0" />
                                  ) : (
                                    <MapPin className="size-3 shrink-0" />
                                  )}
                                </div>
                                <div className="truncate text-[9px] font-semibold mt-0.5">
                                  {isProposed && "⚠️ Chờ xác nhận"}
                                  {isScheduled && "✓ Đã xếp lịch"}
                                  {isCompleted && "✓ Đã hoàn thành"}
                                  {isCancelled && "✕ Đã hủy"}
                                </div>
                              </button>
                            );
                          })}
                        </div>
                      );
                    })}
                  </div>
                ))
              )}
            </div>
          </div>
        </section>
      ) : (
        /* Timeline View */
        <ScheduleTimelineView
          sessions={sessions}
          onSelectSession={(id) => {
            setSelectedSessionId(id);
            setIsDetailOpen(true);
          }}
          onProposeNew={() => {
            setProposeInitialDate(undefined);
            setProposeInitialHour(8);
            setIsProposeOpen(true);
          }}
        />
      )}

      {/* Guide Cards */}
      <section className="grid gap-4 lg:grid-cols-3">
        {[
          ["1", "Đề xuất thời gian", "Bấm vào bất kỳ ô trống nào trên lịch để đề xuất khung giờ học."],
          [
            "2",
            "Kiểm tra & Chống trùng GiST",
            "Hệ thống tự động phát hiện xung đột lịch và bẫy lỗi 409 nếu có ca học khác cùng giờ.",
          ],
          [
            "3",
            "Xác nhận & Đưa vào lịch",
            "Chỉ khi bên còn lại xác nhận, buổi học mới chính thức khóa lịch và hiển thị xanh.",
          ],
        ].map(([step, title, description]) => (
          <div key={step} className="rounded-2xl border border-stone-200 bg-white p-5 shadow-2xs">
            <div className="mb-3 flex items-center gap-2">
              <span className="grid size-7 place-items-center rounded-full bg-sky-100 text-xs font-black text-sky-800">
                {step}
              </span>
              <h2 className="text-sm font-bold text-neutral-900">{title}</h2>
            </div>
            <p className="text-xs leading-relaxed text-neutral-500">{description}</p>
          </div>
        ))}
      </section>

      {/* Modals */}
      <ProposeSessionModal
        isOpen={isProposeOpen}
        onClose={() => setIsProposeOpen(false)}
        onSuccess={() => {
          fetchSessions();
        }}
        role={role}
        currentUserId={user?.id}
        initialDate={proposeInitialDate}
        initialHour={proposeInitialHour}
        connectedClasses={classes}
      />

      <SessionDetailModal
        sessionId={selectedSessionId}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false);
          setSelectedSessionId(null);
        }}
        onUpdated={() => {
          fetchSessions();
        }}
        role={role}
        currentUserId={user?.id}
      />
    </div>
  );
}
