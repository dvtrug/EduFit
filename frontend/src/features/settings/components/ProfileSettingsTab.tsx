"use client";

import React from "react";
import { UserRole } from "@/shared/ui/Sidebar";
import {
  TutorProfileForm,
  StudentProfileForm,
  ParentProfileForm,
  AdminProfileForm,
} from "@/features/profile";

interface ProfileSettingsTabProps {
  role?: UserRole;
}

export function ProfileSettingsTab({ role = "STUDENT" }: ProfileSettingsTabProps) {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-stone-100">
        <h2 className="font-heading text-xl font-bold text-neutral-900">
          Hồ sơ cá nhân
        </h2>
        <p className="text-xs text-neutral-500 mt-1">
          {role === "TUTOR"
            ? "Thông tin chuyên môn, môn giảng dạy, khu vực và giới thiệu bản thân hiển thị tới phụ huynh & học sinh."
            : role === "STUDENT"
            ? "Thông tin học tập, khối lớp và mục tiêu rèn luyện cá nhân của bạn."
            : role === "PARENT"
            ? "Thông tin liên hệ phụ huynh và quản lý hồ sơ con em liên kết."
            : "Thông tin hồ sơ quản trị viên điều hành nền tảng."}
        </p>
      </div>

      {role === "TUTOR" && <TutorProfileForm />}
      {role === "STUDENT" && <StudentProfileForm />}
      {role === "PARENT" && <ParentProfileForm />}
      {role === "ADMIN" && <AdminProfileForm />}
    </div>
  );
}
