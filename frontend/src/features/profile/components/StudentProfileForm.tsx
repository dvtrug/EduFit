"use client";

import React, { useEffect, useState } from "react";
import { profileService, StudentProfileData } from "@/services/profile";
import { catalogService, EducationLevel } from "@/services/catalog";
import { useToast } from "@/shared/ui/Toast";
import { GraduationCap, MapPin, Loader2, Save, Sparkles, CheckCircle2, Target } from "lucide-react";

/**
 * =========================================================================
 * SPRING BOOT API INTEGRATION:
 * =========================================================================
 * - Read Profile: GET /api/v1/profiles/students/me (ProfileController#getMyStudentProfile)
 * - Update Profile: PUT /api/v1/profiles/students/me (ProfileController#updateMyStudentProfile)
 *   Payload: UpdateStudentProfileRequest { educationLevelId, area }
 * - Education Levels: GET /api/v1/catalogs/education-levels (CatalogController#getEducationLevels)
 * =========================================================================
 */

interface StudentProfileFormProps {
  initialProfile?: StudentProfileData;
  onSaved?: () => void;
}

export function StudentProfileForm({ initialProfile, onSaved }: StudentProfileFormProps) {
  const { showToast } = useToast();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [educationLevels, setEducationLevels] = useState<EducationLevel[]>([]);

  // Form State: Khối lớp & Khu vực
  const [selectedLevelId, setSelectedLevelId] = useState<number | undefined>(
    initialProfile?.educationLevelId
  );
  const [area, setArea] = useState<string>(initialProfile?.area || "");

  // Form State: Mục tiêu học tập cá nhân
  const [goalTitle, setGoalTitle] = useState("Chuẩn bị cho mục tiêu vào lớp 10");
  const [targetSubjects, setTargetSubjects] = useState("Toán, Tiếng Anh");
  const [targetScore, setTargetScore] = useState("8.5+ điểm");
  const [learningNote, setLearningNote] = useState("");

  // Tải danh mục cấp lớp và hồ sơ
  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const [levels, currentProfile] = await Promise.all([
          catalogService.getEducationLevels(),
          initialProfile ? Promise.resolve(initialProfile) : profileService.getMyStudentProfile().catch(() => null),
        ]);

        setEducationLevels(levels);
        if (currentProfile) {
          setSelectedLevelId(currentProfile.educationLevelId);
          setArea(currentProfile.area || "");
        }

        // Tải mục tiêu đã thiết lập trước đó từ storage
        const savedGoalStr = localStorage.getItem("edufit_student_onboarding_goal");
        if (savedGoalStr) {
          try {
            const savedGoal = JSON.parse(savedGoalStr);
            if (savedGoal.title) setGoalTitle(savedGoal.title);
            if (savedGoal.subjects) {
              setTargetSubjects(
                Array.isArray(savedGoal.subjects)
                  ? savedGoal.subjects.join(", ")
                  : savedGoal.subjects
              );
            }
            if (savedGoal.targetScore) setTargetScore(savedGoal.targetScore);
            if (savedGoal.learningNote) setLearningNote(savedGoal.learningNote);
          } catch {
            // ignore
          }
        }
      } catch (err) {
        console.error("Lỗi khi tải dữ liệu hồ sơ học sinh:", err);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [initialProfile]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);

    try {
      // 1. Cập nhật hồ sơ học viên lên Backend
      await profileService.updateStudentProfile({
        educationLevelId: selectedLevelId,
        area: area.trim() || undefined,
      });

      // 2. Lưu mục tiêu học tập vào localStorage để đồng bộ tức thì với Dashboard
      const selectedLevelName =
        educationLevels.find((lvl) => lvl.levelId === selectedLevelId)?.name || "Lớp 9";
      localStorage.setItem(
        "edufit_student_onboarding_goal",
        JSON.stringify({
          grade: selectedLevelName,
          title: goalTitle.trim(),
          subjects: targetSubjects.split(",").map((s) => s.trim()).filter(Boolean),
          targetScore: targetScore.trim(),
          learningNote: learningNote.trim(),
          area: area.trim(),
          updatedAt: new Date().toISOString(),
        })
      );

      showToast("Cập nhật hồ sơ và mục tiêu học tập thành công!", "success");
      onSaved?.();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi lưu hồ sơ";
      showToast(message, "error");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-12 flex flex-col items-center justify-center gap-3 bg-white rounded-3xl border border-stone-200">
        <Loader2 className="size-6 text-amber-500 animate-spin" />
        <span className="text-xs text-neutral-500 font-medium">Đang tải thông tin hồ sơ học viên...</span>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6 bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-100">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-50 text-amber-900 rounded-full text-xs font-bold mb-2">
            <GraduationCap className="size-3.5 text-amber-600" />
            <span>Hồ Sơ Học Tập (Student Profile)</span>
          </div>
          <h3 className="font-heading text-xl font-bold text-neutral-900">
            Thông tin khối lớp & Khu vực học
          </h3>
          <p className="text-xs text-neutral-500 mt-0.5">
            Giúp hệ thống AI Match gợi ý gia sư chính xác theo khối học và vị trí gần bạn
          </p>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all disabled:opacity-50 shrink-0 cursor-pointer"
        >
          {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
          <span>{saving ? "Đang lưu..." : "Lưu thay đổi"}</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Chọn Khối lớp (Education Level) */}
        <div className="space-y-2">
          <label className="block text-xs font-bold text-neutral-700">
            Khối lớp hiện tại (Cấp học) <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <select
              value={selectedLevelId || ""}
              onChange={(e) => setSelectedLevelId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 font-medium transition-all"
            >
              <option value="">-- Chọn khối lớp theo học --</option>
              {educationLevels.map((lvl) => (
                <option key={lvl.levelId} value={lvl.levelId}>
                  {lvl.name}
                </option>
              ))}
            </select>
          </div>
          <p className="text-[11px] text-neutral-400">
            Dữ liệu đồng bộ trực tiếp từ danh mục quốc gia (Catalog) của EduFit
          </p>
        </div>

        {/* Nhập Khu vực học (Area) */}
        <div className="space-y-2">
          <label className="block text-xs font-bold text-neutral-700">
            Khu vực / Quận Huyện sinh sống
          </label>
          <div className="relative">
            <MapPin className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={area}
              onChange={(e) => setArea(e.target.value)}
              placeholder="VD: Cầu Giấy, Hà Nội hoặc Quận 1, TP.HCM"
              maxLength={100}
              className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 placeholder:text-neutral-400 transition-all"
            />
          </div>
          <p className="text-[11px] text-neutral-400">
            Tối đa 100 ký tự (Dùng để tìm gia sư dạy Offline tại nhà)
          </p>
        </div>
      </div>

      {/* 2. CHỈNH SỬA MỤC TIÊU HỌC TẬP (LEARNING GOALS) */}
      <div className="pt-6 border-t border-stone-100 space-y-4">
        <div className="flex items-center gap-2">
          <Target className="size-4 text-amber-600" />
          <h4 className="text-sm font-bold text-neutral-900">
            Mục tiêu học tập & Kế hoạch cá nhân
          </h4>
        </div>
        <p className="text-xs text-neutral-500">
          EduFit sẽ dựa trên mục tiêu này để đồng bộ lộ trình tiến độ tại Bảng điều khiển và gợi ý gia sư phù hợp.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Tiêu đề mục tiêu học tập
            </label>
            <input
              type="text"
              value={goalTitle}
              onChange={(e) => setGoalTitle(e.target.value)}
              placeholder="VD: Ôn thi vào lớp 10 THPT chuyên, Nâng cao điểm số môn Toán..."
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 font-medium transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Môn học trọng tâm cần kèm cặp
            </label>
            <input
              type="text"
              value={targetSubjects}
              onChange={(e) => setTargetSubjects(e.target.value)}
              placeholder="VD: Toán, Tiếng Anh, Ngữ văn"
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 font-medium transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Điểm số / Mức độ kỳ vọng
            </label>
            <input
              type="text"
              value={targetScore}
              onChange={(e) => setTargetScore(e.target.value)}
              placeholder="VD: Đạt 8.5+ điểm, Lấy lại căn bản sau 2 tháng..."
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 font-medium transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Ghi chú thêm cho gia sư
            </label>
            <input
              type="text"
              value={learningNote}
              onChange={(e) => setLearningNote(e.target.value)}
              placeholder="VD: Cần kèm dạng bài Hình học và Phương trình..."
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 font-medium transition-all"
            />
          </div>
        </div>
      </div>

      {/* Thông tin hỗ trợ & Cam kết minh bạch */}
      <div className="p-4 rounded-2xl bg-stone-50 border border-stone-200/80 flex items-start gap-3">
        <Sparkles className="size-4.5 text-amber-500 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <p className="font-bold text-neutral-800">Cá nhân hóa mục tiêu học tập cùng EduFit</p>
          <p className="text-neutral-500 leading-relaxed">
            Học sinh có thể liên kết tài khoản cùng Phụ huynh để nhận báo cáo định kỳ sau từng buổi dạy kèm và theo dõi lộ trình tiến bộ minh bạch.
          </p>
        </div>
      </div>
    </form>
  );
}
