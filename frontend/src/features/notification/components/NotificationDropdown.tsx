"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  Bell,
  Check,
  CheckCheck,
  Calendar,
  UserCheck,
  Star,
  Clock,
  Sparkles,
  Inbox,
  AlertCircle,
  ExternalLink,
} from "lucide-react";
import { notificationApi } from "../api/notificationApi";
import type { NotificationItem } from "../types";

interface NotificationDropdownProps {
  onClose?: () => void;
  onUnreadCountChange?: (count: number) => void;
}

export function NotificationDropdown({
  onClose,
  onUnreadCountChange,
}: NotificationDropdownProps) {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [filterUnread, setFilterUnread] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Load notifications
  const loadNotifications = async (unreadOnly = filterUnread) => {
    try {
      setIsLoading(true);
      setError(null);
      const res = await notificationApi.getNotifications(unreadOnly, 0, 20);
      if (res.success && res.data) {
        setNotifications(res.data.items || []);
        setUnreadCount(res.data.unreadCount || 0);
        onUnreadCountChange?.(res.data.unreadCount || 0);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể tải thông báo";
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications(filterUnread);
  }, [filterUnread]);

  // Click outside listener
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        onClose?.();
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [onClose]);

  // Mark single notification read
  const handleMarkRead = async (id: string, e?: React.MouseEvent) => {
    e?.stopPropagation();
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.notificationId === id ? { ...n, isRead: true } : n))
      );
      setUnreadCount((prev) => {
        const next = Math.max(0, prev - 1);
        onUnreadCountChange?.(next);
        return next;
      });
    } catch {
      // Ignored for smooth UX
    }
  };

  // Mark all read
  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
      onUnreadCountChange?.(0);
    } catch {
      // Ignored for smooth UX
    }
  };

  // Helper relative time
  const formatRelativeTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / (1000 * 60));
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

      if (diffMins < 1) return "Vừa xong";
      if (diffMins < 60) return `${diffMins} phút trước`;
      if (diffHours < 24) return `${diffHours} giờ trước`;
      if (diffDays < 7) return `${diffDays} ngày trước`;
      return date.toLocaleDateString("vi-VN");
    } catch {
      return "";
    }
  };

  // Notification icon by type
  const renderNotificationIcon = (type: string) => {
    switch (type) {
      case "LINK_INVITED":
      case "LINK_CONFIRMED":
      case "LINK_REVOKED":
      case "CONNECTION_REQUEST":
        return <UserCheck className="size-4 text-blue-500" />;
      case "SESSION_PROPOSAL":
      case "SESSION_UPDATE":
        return <Calendar className="size-4 text-emerald-500" />;
      case "REVIEW_RECEIVED":
        return <Star className="size-4 text-amber-500 fill-amber-500" />;
      default:
        return <Bell className="size-4 text-neutral-400" />;
    }
  };

  return (
    <div
      ref={dropdownRef}
      className="absolute right-0 top-full mt-2 w-80 sm:w-96 bg-white dark:bg-stone-900 rounded-2xl shadow-2xl border border-stone-200 dark:border-stone-800 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-150"
    >
      {/* Header */}
      <div className="p-4 border-b border-stone-100 dark:border-stone-800 flex items-center justify-between bg-stone-50/50 dark:bg-stone-800/40">
        <div className="flex items-center gap-2">
          <Bell className="size-4.5 text-neutral-700 dark:text-stone-300" />
          <h3 className="font-heading font-bold text-sm text-neutral-900 dark:text-stone-100">
            Thông báo
          </h3>
          {unreadCount > 0 && (
            <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500 text-white">
              {unreadCount}
            </span>
          )}
        </div>

        {unreadCount > 0 && (
          <button
            type="button"
            onClick={handleMarkAllRead}
            className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-600 hover:text-amber-700 dark:text-amber-400 dark:hover:text-amber-300 transition-colors cursor-pointer"
          >
            <CheckCheck className="size-3.5" />
            <span>Đọc tất cả</span>
          </button>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex border-b border-stone-100 dark:border-stone-800 text-xs px-3 pt-2 gap-2 bg-stone-50/30 dark:bg-stone-800/20">
        <button
          type="button"
          onClick={() => setFilterUnread(false)}
          className={`pb-2 px-2 font-semibold border-b-2 transition-colors cursor-pointer ${
            !filterUnread
              ? "border-amber-500 text-amber-600 dark:text-amber-400"
              : "border-transparent text-neutral-500 dark:text-stone-400 hover:text-neutral-800"
          }`}
        >
          Tất cả
        </button>
        <button
          type="button"
          onClick={() => setFilterUnread(true)}
          className={`pb-2 px-2 font-semibold border-b-2 transition-colors cursor-pointer ${
            filterUnread
              ? "border-amber-500 text-amber-600 dark:text-amber-400"
              : "border-transparent text-neutral-500 dark:text-stone-400 hover:text-neutral-800"
          }`}
        >
          Chưa đọc ({unreadCount})
        </button>
      </div>

      {/* Notification List */}
      <div className="max-h-96 overflow-y-auto divide-y divide-stone-100 dark:divide-stone-800">
        {isLoading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-2 text-neutral-400">
            <div className="size-6 border-2 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
            <span className="text-xs">Đang tải thông báo...</span>
          </div>
        ) : error ? (
          <div className="py-8 px-4 text-center text-xs text-rose-500 flex flex-col items-center gap-1">
            <AlertCircle className="size-5 text-rose-500" />
            <span>{error}</span>
          </div>
        ) : notifications.length === 0 ? (
          <div className="py-12 px-4 text-center flex flex-col items-center justify-center gap-2 text-neutral-400 dark:text-stone-500">
            <Inbox className="size-8 stroke-1" />
            <span className="text-xs">
              {filterUnread ? "Không có thông báo chưa đọc" : "Chưa có thông báo nào"}
            </span>
          </div>
        ) : (
          notifications.map((item) => (
            <div
              key={item.notificationId}
              onClick={() => !item.isRead && handleMarkRead(item.notificationId)}
              className={`p-3.5 flex gap-3 text-left transition-colors cursor-pointer ${
                item.isRead
                  ? "bg-white dark:bg-stone-900 opacity-80 hover:bg-stone-50 dark:hover:bg-stone-800/60"
                  : "bg-amber-50/40 dark:bg-amber-950/20 hover:bg-amber-50/70 dark:hover:bg-amber-950/30"
              }`}
            >
              <div className="size-8 rounded-full bg-stone-100 dark:bg-stone-800 flex items-center justify-center shrink-0 mt-0.5">
                {renderNotificationIcon(item.type)}
              </div>

              <div className="flex-1 min-w-0 space-y-0.5">
                <div className="flex items-center justify-between gap-1">
                  <h4
                    className={`text-xs truncate ${
                      item.isRead
                        ? "font-medium text-neutral-800 dark:text-stone-200"
                        : "font-bold text-neutral-900 dark:text-stone-100"
                    }`}
                  >
                    {item.title}
                  </h4>
                  <span className="text-[10px] text-neutral-400 dark:text-stone-500 whitespace-nowrap">
                    {formatRelativeTime(item.createdAt)}
                  </span>
                </div>

                <p className="text-[11px] text-neutral-600 dark:text-stone-400 line-clamp-2 leading-relaxed">
                  {item.shortText}
                </p>
              </div>

              {!item.isRead && (
                <div className="flex items-center shrink-0">
                  <span
                    title="Đánh dấu đã đọc"
                    onClick={(e) => handleMarkRead(item.notificationId, e)}
                    className="size-2 rounded-full bg-amber-500"
                  />
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
