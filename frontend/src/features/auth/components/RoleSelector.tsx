"use client";

import React from "react";
import { GraduationCap, Users, BookOpen } from "lucide-react";

export type RoleType = "STUDENT" | "TUTOR" | "PARENT";

interface RoleSelectorProps {
  selectedRole: RoleType;
  onChange: (role: RoleType) => void;
}

export function RoleSelector({ selectedRole, onChange }: RoleSelectorProps) {
  const roles: { id: RoleType; label: string; desc: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { id: "STUDENT", label: "Học sinh", desc: "Tìm gia sư & học tập", icon: GraduationCap },
    { id: "TUTOR", label: "Gia sư", desc: "Giảng dạy & nhận lớp", icon: BookOpen },
    { id: "PARENT", label: "Phụ huynh", desc: "Theo dõi tiến độ con", icon: Users },
  ];

  return (
    <div className="space-y-1.5">
      <label className="block text-xs font-semibold text-neutral-700">Tôi muốn đăng ký vai trò:</label>
      <div className="grid grid-cols-3 gap-2">
        {roles.map((r) => {
          const Icon = r.icon;
          const isSelected = selectedRole === r.id;
          return (
            <button
              key={r.id}
              type="button"
              onClick={() => onChange(r.id)}
              className={`p-2.5 rounded-2xl border text-center flex flex-col items-center justify-center gap-1 transition-all ${
                isSelected
                  ? "border-amber-500 bg-amber-50/50 text-amber-950 font-semibold shadow-xs ring-2 ring-amber-500/20"
                  : "border-stone-200 hover:border-stone-300 text-neutral-600 bg-white"
              }`}
            >
              <Icon className={`size-4.5 ${isSelected ? "text-amber-600" : "text-neutral-400"}`} />
              <span className="text-xs font-bold leading-none">{r.label}</span>
              <span className="text-[9px] text-neutral-400 leading-none">{r.desc}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
