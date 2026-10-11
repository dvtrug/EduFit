"use client";

import React, { useEffect, useState } from "react";
import {
  profileService,
  TutorProfileData,
  TutorAvailabilitySlot,
  UpdateTutorProfilePayload,
} from "@/services/profile";
import { catalogService, SubjectItem, EducationLevel } from "@/services/catalog";
import { AvailabilitySlotPicker } from "./AvailabilitySlotPicker";
import {
  normalizeAvailabilityTime,
  sortAvailabilitySlots,
  validateAvailabilitySlots,
} from "./availabilitySlotUtils";
import { useToast } from "@/shared/ui/Toast";
import {
  User,
  DollarSign,
  BookOpen,
  MapPin,
  Plus,
  Trash2,
  Save,
  Loader2,
  Briefcase,
} from "lucide-react";

/**
 * =========================================================================
 * SPRING BOOT API INTEGRATION:
 * =========================================================================
 * - Read: GET /api/v1/profiles/tutors/me (ProfileController#getMyTutorProfile)
 * - Update: PUT /api/v1/profiles/tutors/me (ProfileController#updateMyTutorProfile)
 *   Payload: UpdateTutorProfileRequest { displayName, headline, bio, teachingMode, area, pricePerSession, experienceYears, teachingMethod }
 * - Add Subject: POST /api/v1/profiles/tutors/me/subjects (ProfileController#addTutorSubject)
 * - Remove Subject: DELETE /api/v1/profiles/tutors/me/subjects (ProfileController#removeTutorSubject)
 * - Update Availability: PUT /api/v1/profiles/tutors/me/availability-slots (ProfileController#setAvailabilitySlots)
 * =========================================================================
 */

interface TutorProfileFormProps {
  initialProfile?: TutorProfileData;
  onSaved?: () => void;
}

export function TutorProfileForm({ initialProfile, onSaved }: TutorProfileFormProps) {
  const { showToast } = useToast();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Catalogs
  const [subjectsCatalog, setSubjectsCatalog] = useState<SubjectItem[]>([]);
  const [levelsCatalog, setLevelsCatalog] = useState<EducationLevel[]>([]);

  // Tutor Profile Form State
  const [profile, setProfile] = useState<TutorProfileData | null>(initialProfile || null);
  const [displayName, setDisplayName] = useState(initialProfile?.displayName || "");
  const [headline, setHeadline] = useState(initialProfile?.headline || "");
  const [bio, setBio] = useState(initialProfile?.bio || "");
  const [teachingMode, setTeachingMode] = useState<"ONLINE" | "OFFLINE" | "BOTH">(
    initialProfile?.teachingMode || "ONLINE"
  );
  const [area, setArea] = useState(initialProfile?.area || "");
  const [pricePerSession, setPricePerSession] = useState<number>(
    initialProfile?.pricePerSession || 200000
  );
  const [experienceYears, setExperienceYears] = useState<number>(
    initialProfile?.experienceYears || 1
  );
  const [teachingMethod, setTeachingMethod] = useState(initialProfile?.teachingMethod || "");

  // Subjects & Slots State
  const [availabilitySlots, setAvailabilitySlots] = useState<TutorAvailabilitySlot[]>(
    initialProfile?.availabilitySlots || []
  );

  // New Subject Selector State
  const [newSubjectId, setNewSubjectId] = useState<number | "">("");
  const [newLevelId, setNewLevelId] = useState<number | "">("");
  const [addingSubject, setAddingSubject] = useState(false);

  // Tải dữ liệu ban đầu
  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const [subjects, levels, currentProfile] = await Promise.all([
          catalogService.getSubjects(),
          catalogService.getEducationLevels(),
          initialProfile ? Promise.resolve(initialProfile) : profileService.getMyTutorProfile().catch(() => null),
        ]);

        setSubjectsCatalog(subjects);
        setLevelsCatalog(levels);

        if (currentProfile) {
          setProfile(currentProfile);
          setDisplayName(currentProfile.displayName || "");
          setHeadline(currentProfile.headline || "");
          setBio(currentProfile.bio || "");
          setTeachingMode(currentProfile.teachingMode || "ONLINE");
          setArea(currentProfile.area || "");
          setPricePerSession(currentProfile.pricePerSession || 200000);
          setExperienceYears(currentProfile.experienceYears || 1);
          setTeachingMethod(currentProfile.teachingMethod || "");
          setAvailabilitySlots(currentProfile.availabilitySlots || []);
        }
      } catch (err) {
        console.error("Lỗi khi tải hồ sơ gia sư:", err);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [initialProfile]);

  // Lưu thông tin chung & Lịch rảnh
  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();

    const availabilityValidation = validateAvailabilitySlots(availabilitySlots);
    if (!availabilityValidation.valid) {
      showToast(availabilityValidation.message || "Lịch rảnh chưa hợp lệ", "error");
      return;
    }

    setSaving(true);

    try {
      const payload: UpdateTutorProfilePayload = {
        displayName: displayName.trim(),
        headline: headline.trim() || undefined,
        bio: bio.trim() || undefined,
        teachingMode,
        area: area.trim() || undefined,
        pricePerSession,
        experienceYears,
        teachingMethod: teachingMethod.trim() || undefined,
      };

      // 1. Cập nhật hồ sơ gia sư
      const updatedProfile = await profileService.updateTutorProfile(payload);

      // 2. Cập nhật khung giờ rảnh
      const formattedSlots = sortAvailabilitySlots(availabilitySlots).map((s) => ({
        dayOfWeek: s.dayOfWeek,
        startTime: normalizeAvailabilityTime(s.startTime),
        endTime: normalizeAvailabilityTime(s.endTime),
      }));
      await profileService.setAvailabilitySlots(formattedSlots);

      setProfile(updatedProfile);
      showToast("Lưu hồ sơ và lịch rảnh gia sư thành công!", "success");
      onSaved?.();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi lưu hồ sơ gia sư";
      showToast(message, "error");
    } finally {
      setSaving(false);
    }
  };

  // Thêm môn học mới
  const handleAddSubject = async () => {
    if (!newSubjectId || !newLevelId) {
      showToast("Vui lòng chọn cả môn học và cấp lớp!", "error");
      return;
    }

    setAddingSubject(true);
    try {
      const updated = await profileService.addTutorSubject(Number(newSubjectId), Number(newLevelId));
      setProfile(updated);
      setNewSubjectId("");
      setNewLevelId("");
      showToast("Thêm môn học giảng dạy thành công!", "success");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Không thể thêm môn học";
      showToast(message, "error");
    } finally {
      setAddingSubject(false);
    }
  };

  // Xóa môn học
  const handleRemoveSubject = async (subjectId: number, levelId: number) => {
    try {
      const updated = await profileService.removeTutorSubject(subjectId, levelId);
      setProfile(updated);
      showToast("Đã xóa môn học khỏi danh sách!", "info");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Không thể xóa môn học";
      showToast(message, "error");
    }
  };

  if (loading) {
    return (
      <div className="p-12 flex flex-col items-center justify-center gap-3 bg-white rounded-3xl border border-stone-200">
        <Loader2 className="size-6 text-amber-500 animate-spin" />
        <span className="text-xs text-neutral-500 font-medium">Đang tải hồ sơ gia sư...</span>
      </div>
    );
  }

  return (
    <form onSubmit={handleSaveProfile} className="space-y-8">
      {/* 1. Phần Thông Tin Cá Nhân & Học Phí */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-100">
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-sky-50 text-sky-900 rounded-full text-xs font-bold mb-2">
              <Briefcase className="size-3.5 text-sky-600" />
              <span>Hồ Sơ Gia Sư Chuyên Nghiệp</span>
            </div>
            <h3 className="font-heading text-xl font-bold text-neutral-900">
              Thông tin giảng dạy & Học phí
            </h3>
            <p className="text-xs text-neutral-500 mt-0.5">
              Hiển thị trên thẻ gia sư công khai và hồ sơ tìm kiếm của học sinh / phụ huynh
            </p>
          </div>

          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-sky-600 hover:bg-sky-700 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all disabled:opacity-50 shrink-0 cursor-pointer"
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
            <span>{saving ? "Đang lưu..." : "Lưu hồ sơ"}</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Tên hiển thị (Display Name) <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <User className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Thầy Nguyễn Văn A"
                maxLength={100}
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-medium"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Tiêu đề hồ sơ (Headline)
            </label>
            <input
              type="text"
              value={headline}
              onChange={(e) => setHeadline(e.target.value)}
              placeholder="VD: Cử nhân ĐH Sư Phạm Hà Nội · 5 năm kinh nghiệm luyện thi vào 10"
              maxLength={200}
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Học phí theo buổi (VND / buổi 90 phút) <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <DollarSign className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="number"
                min={0}
                step={10000}
                required
                value={pricePerSession}
                onChange={(e) => setPricePerSession(Number(e.target.value))}
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-bold"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Hình thức giảng dạy
            </label>
            <select
              value={teachingMode}
              onChange={(e) => setTeachingMode(e.target.value as "ONLINE" | "OFFLINE" | "BOTH")}
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-medium"
            >
              <option value="ONLINE">Học Online (Google Meet / Zoom)</option>
              <option value="OFFLINE">Dạy kèm tại nhà (Offline)</option>
              <option value="BOTH">Cả Online & Offline</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Số năm kinh nghiệm giảng dạy
            </label>
            <input
              type="number"
              min={0}
              max={50}
              value={experienceYears}
              onChange={(e) => setExperienceYears(Number(e.target.value))}
              className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1.5">
              Khu vực hỗ trợ gia sư Offline
            </label>
            <div className="relative">
              <MapPin className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={area}
                onChange={(e) => setArea(e.target.value)}
                placeholder="VD: Quận Cầu Giấy, Nam Từ Liêm, Hà Nội"
                className="w-full pl-10 pr-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-medium"
              />
            </div>
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-neutral-700 mb-1.5">
            Tiểu sử & Giới thiệu bản thân (Bio)
          </label>
          <textarea
            rows={4}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            placeholder="Chia sẻ về phong cách giảng dạy, phương pháp sư phạm và thành tích học viên trước đây..."
            className="w-full p-3.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-sky-500 focus:ring-2 focus:ring-sky-500/20 text-neutral-900 font-medium leading-relaxed"
          />
        </div>
      </div>

      {/* 2. Quản Lý Danh Sách Môn Học Giảng Dạy (Tutor Subjects) */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs space-y-5">
        <div className="flex items-center gap-2">
          <BookOpen className="size-4 text-indigo-600" />
          <h4 className="text-xs font-bold text-neutral-900 uppercase tracking-wider">
            Môn học & Khối lớp đảm nhận
          </h4>
        </div>

        {/* Bảng môn học hiện có */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {(profile?.subjects || []).map((sub) => (
            <div
              key={sub.tutorSubjectId}
              className="p-3.5 rounded-2xl border border-stone-200/80 bg-stone-50/50 flex items-center justify-between gap-3 group hover:border-indigo-300 transition-colors"
            >
              <div>
                <span className="text-xs font-bold text-neutral-900 block">{sub.subjectName}</span>
                <span className="text-[11px] text-neutral-500">{sub.educationLevelName}</span>
              </div>
              <button
                type="button"
                onClick={() => handleRemoveSubject(sub.subjectId, sub.educationLevelId)}
                className="p-1.5 text-neutral-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors"
                title="Xóa môn học này"
              >
                <Trash2 className="size-4" />
              </button>
            </div>
          ))}
          {(profile?.subjects || []).length === 0 && (
            <div className="col-span-full py-4 text-center text-xs text-neutral-400 border border-dashed border-stone-200 rounded-2xl">
              Chưa có môn học nào được đăng ký. Hãy chọn môn bên dưới để thêm.
            </div>
          )}
        </div>

        {/* Thanh thêm môn học mới */}
        <div className="p-4 bg-stone-50 rounded-2xl border border-stone-200 flex flex-col sm:flex-row sm:items-center gap-3">
          <select
            value={newSubjectId}
            onChange={(e) => setNewSubjectId(e.target.value ? Number(e.target.value) : "")}
            className="flex-1 px-3 py-2 text-xs bg-white border border-stone-200 rounded-xl text-neutral-800"
          >
            <option value="">-- Chọn môn học --</option>
            {subjectsCatalog.map((s) => (
              <option key={s.subjectId} value={s.subjectId}>
                {s.name}
              </option>
            ))}
          </select>

          <select
            value={newLevelId}
            onChange={(e) => setNewLevelId(e.target.value ? Number(e.target.value) : "")}
            className="flex-1 px-3 py-2 text-xs bg-white border border-stone-200 rounded-xl text-neutral-800"
          >
            <option value="">-- Chọn cấp học --</option>
            {levelsCatalog.map((lvl) => (
              <option key={lvl.levelId} value={lvl.levelId}>
                {lvl.name}
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={handleAddSubject}
            disabled={addingSubject}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-colors shrink-0 disabled:opacity-50"
          >
            {addingSubject ? <Loader2 className="size-3.5 animate-spin" /> : <Plus className="size-3.5" />}
            <span>Thêm môn</span>
          </button>
        </div>
      </div>

      {/* 3. Lịch rảnh định kỳ của gia sư */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs">
        <AvailabilitySlotPicker slots={availabilitySlots} onChange={setAvailabilitySlots} />
      </div>
    </form>
  );
}
