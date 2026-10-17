"use client";

import React, { useMemo } from "react";
import { Clock, Video, MapPin, Calendar, ChevronRight, Sparkles } from "lucide-react";
import { EmptyState } from "@/shared/ui/EmptyState";
import type { CalendarItem } from "../types";

export interface ScheduleTimelineViewProps {
  sessions: CalendarItem[];
  onSelectSession: (sessionId: string) => void;
  onProposeNew: () => void;
}

export function ScheduleTimelineView({
  sessions,
  onSelectSession,
  onProposeNew,
}: ScheduleTimelineViewProps) {
  // Sort sessions chronologically
  const sortedSessions = useMemo(() => {
    return [...sessions].sort(
      (a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime()
    );
  }, [sessions]);

  // Group by date (YYYY-MM-DD)
  const groupedSessions = useMemo(() => {
    const map = new Map<string, CalendarItem[]>();
    for (const session of sortedSessions) {
      const dateKey = session.startAt ? session.startAt.slice(0, 10) : "unknown";
      const existing = map.get(dateKey) || [];
      existing.push(session);
      map.set(dateKey, existing);
    }
    return Array.from(map.entries());
  }, [sortedSessions]);

  const formatDateHeader = (dateStr: string) => {
    try {
      const d = new Date(`${dateStr}T00:00:00`);
      return new Intl.DateTimeFormat("vi-VN", {
        weekday: "long",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      }).format(d);
    } catch {
      return dateStr;
    }
  };

  const formatTimeRange = (startIso: string, endIso: string) => {
    try {
      const s = new Date(startIso);
      const e = new Date(endIso);
      const startStr = `${String(s.getHours()).padStart(2, "0")}:${String(s.getMinutes()).padStart(2, "0")}`;
      const endStr = `${String(e.getHours()).padStart(2, "0")}:${String(e.getMinutes()).padStart(2, "0")}`;
      return `${startStr} - ${endStr}`;
    } catch {
      return `${startIso} - ${endIso}`;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PROPOSED":
        return { text: "Chờ xác nhận", bg: "bg-amber-100 text-amber-800 border-amber-200" };
      case "SCHEDULED":
        return { text: "Đã xếp lịch", bg: "bg-sky-100 text-sky-800 border-sky-200" };
      case "COMPLETED":
        return { text: "Đã hoàn thành", bg: "bg-emerald-100 text-emerald-800 border-emerald-200" };
      case "CANCELLED":
        return { text: "Đã hủy", bg: "bg-rose-100 text-rose-800 border-rose-200" };
      case "REJECTED":
        return { text: "Từ chối", bg: "bg-stone-200 text-stone-700 border-stone-300" };
      case "EXPIRED":
        return { text: "Hết hạn", bg: "bg-stone-200 text-stone-700 border-stone-300" };
      default:
        return { text: status, bg: "bg-stone-100 text-stone-700 border-stone-200" };
    }
  };

  if (sessions.length === 0) {
    return (
      <div className="rounded-3xl border border-dashed border-stone-200 bg-white p-8">
        <EmptyState
          icon={Calendar}
          theme="sky"
          compact
          title="Không có buổi học nào trong khoảng thời gian này"
          description="Bạn chưa có buổi học hoặc đề xuất nào trong tuần đã chọn. Bấm nút dưới đây để đề xuất buổi học mới."
          action={{
            label: "Đề xuất lịch mới ngay",
            onClick: onProposeNew,
            icon: Sparkles,
          }}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {groupedSessions.map(([dateKey, items]) => (
        <section key={dateKey} className="space-y-3">
          {/* Ngày trong tuần Header */}
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-sky-500" />
            <h3 className="font-heading text-sm font-bold capitalize text-neutral-800">
              {formatDateHeader(dateKey)}
            </h3>
            <span className="text-xs font-semibold text-neutral-400">({items.length} buổi)</span>
          </div>

          {/* Danh sách thẻ buổi học */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((session) => {
              const badge = getStatusBadge(session.status);
              return (
                <div
                  key={session.sessionId}
                  onClick={() => onSelectSession(session.sessionId)}
                  className="group relative flex flex-col justify-between rounded-2xl border border-stone-200 bg-white p-5 shadow-2xs hover:border-sky-300 hover:shadow-md transition-all cursor-pointer"
                >
                  <div>
                    {/* Top row: Time + Status badge */}
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-900">
                        <Clock className="size-3.5 text-sky-600" />
                        {formatTimeRange(session.startAt, session.endAt)}
                      </div>
                      <span
                        className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[10px] font-bold ${badge.bg}`}
                      >
                        {badge.text}
                      </span>
                    </div>

                    {/* Mode and place */}
                    <div className="mt-3 flex items-center gap-2 text-xs text-neutral-600">
                      {session.mode === "ONLINE" ? (
                        <>
                          <Video className="size-3.5 text-sky-500 shrink-0" />
                          <span className="truncate">Học trực tuyến (Online)</span>
                        </>
                      ) : (
                        <>
                          <MapPin className="size-3.5 text-amber-500 shrink-0" />
                          <span className="truncate">{session.placeOrLink || "Dạy trực tiếp (Offline)"}</span>
                        </>
                      )}
                    </div>

                    {/* Class id or late cancel note */}
                    <div className="mt-2 text-[11px] text-neutral-400">
                      Lớp: {session.classId.slice(0, 8)}...
                    </div>
                  </div>

                  {/* Bottom row: Action link */}
                  <div className="mt-4 pt-3 border-t border-stone-100 flex items-center justify-between text-xs font-bold text-sky-700 group-hover:text-sky-900">
                    <span>Xem chi tiết / Thao tác</span>
                    <ChevronRight className="size-4 transition-transform group-hover:translate-x-0.5" />
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}
