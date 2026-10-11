"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { EduFitLogo } from "@/shared/ui/EduFitLogo";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/shared/ui/Toast";
import { profileService } from "@/services/profile";
import { catalogService, EducationLevel, SubjectItem } from "@/services/catalog";
import { verificationService } from "@/services/verification";
import {
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  Sparkles,
  FileText,
  Upload,
  Check,
  ShieldCheck,
  DollarSign,
  MapPin,
  Clock,
  Loader2,
  Trash2,
} from "lucide-react";

const MAX_CREDENTIAL_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_CREDENTIAL_EXTENSIONS = [".pdf", ".jpg", ".jpeg", ".png"];

export function TutorOnboardingWizard() {
  const router = useRouter();
  const { user, refreshUser } = useAuth();
  const { showToast } = useToast();

  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [lastSaved, setLastSaved] = useState<string>("Vừa cập nhật");

  // Catalogs
  const [subjectsCatalog, setSubjectsCatalog] = useState<SubjectItem[]>([]);
  const [educationLevels, setEducationLevels] = useState<EducationLevel[]>([]);

  // Form State - Step 1: Chuyên môn
  const [displayName, setDisplayName] = useState(user?.fullName || "");
  const [selectedSubjectIds, setSelectedSubjectIds] = useState<number[]>([]);
  const [selectedLevelId, setSelectedLevelId] = useState<number>(0);
  const [teachingMode, setTeachingMode] = useState<"ONLINE" | "OFFLINE" | "BOTH">("ONLINE");
  const [pricePerSession, setPricePerSession] = useState<number>(0);
  const [experienceYears, setExperienceYears] = useState<number>(0);
  const [area, setArea] = useState<string>("");

  // Form State - Step 2: CV & Bio
  const [headline, setHeadline] = useState("");
  const [bio, setBio] = useState("");
  const [teachingMethod, setTeachingMethod] = useState("");
  const [credentialType, setCredentialType] = useState<"DEGREE" | "CERTIFICATE" | "IDENTITY_CARD">("DEGREE");
  const [credentialInstitution, setCredentialInstitution] = useState("");
  const [credentialYear, setCredentialYear] = useState<number>(new Date().getFullYear());
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);

  // AI Bio Tool state
  const [showAiModal, setShowAiModal] = useState(false);
  const [aiTone, setAiTone] = useState<"friendly" | "professional" | "concise">("friendly");
  const [aiAgreed, setAiAgreed] = useState(true);
  const [generatingAi, setGeneratingAi] = useState(false);
  const [aiDraft, setAiDraft] = useState<string | null>(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const [levels, subjects, myProfile] = await Promise.all([
          catalogService.getEducationLevels(),
          catalogService.getSubjects(),
          profileService.getMyTutorProfile().catch(() => null),
        ]);

        setEducationLevels(levels);
        setSubjectsCatalog(subjects);

        if (myProfile) {
          if (myProfile.displayName) setDisplayName(myProfile.displayName);
          if (myProfile.headline) setHeadline(myProfile.headline);
          if (myProfile.bio) setBio(myProfile.bio);
          if (myProfile.teachingMethod) setTeachingMethod(myProfile.teachingMethod);
          if (myProfile.pricePerSession) setPricePerSession(myProfile.pricePerSession);
          if (myProfile.experienceYears) setExperienceYears(myProfile.experienceYears);
          if (myProfile.area) setArea(myProfile.area);
          if (myProfile.teachingMode) setTeachingMode(myProfile.teachingMode);
          if (myProfile.subjects && myProfile.subjects.length > 0) {
            setSelectedSubjectIds(myProfile.subjects.map((s) => s.subjectId));
            setSelectedLevelId(myProfile.subjects[0].educationLevelId);
          }
        }
      } catch (err) {
        console.error("Lỗi khi tải dữ liệu onboarding gia sư:", err);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, []);

  const toggleSubject = (id: number) => {
    setSelectedSubjectIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleNextFromStep1 = () => {
    if (selectedSubjectIds.length === 0) {
      showToast("Vui lòng chọn ít nhất 1 môn giảng dạy!", "error");
      return;
    }
    if (!selectedLevelId) {
      showToast("Vui lòng chọn khối lớp giảng dạy!", "error");
      return;
    }
    if (!area.trim()) {
      showToast("Vui lòng nhập khu vực giảng dạy của bạn!", "error");
      return;
    }
    setStep(2);
    setLastSaved(new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }));
  };

  const handleNextFromStep2 = () => {
    if (!bio.trim() || bio.trim().length < 30) {
      showToast("Vui lòng viết phần giới thiệu bản thân tối thiểu 30 ký tự!", "error");
      return;
    }
    if (!credentialInstitution.trim()) {
      showToast("Vui lòng nhập đơn vị cấp bằng hoặc giấy tờ!", "error");
      return;
    }
    if (selectedFiles.length === 0) {
      showToast("Vui lòng chọn ít nhất một tệp minh chứng thật!", "error");
      return;
    }
    setStep(3);
    setLastSaved(new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }));
  };

  const handleGenerateAiBio = async () => {
    if (!aiAgreed) {
      showToast("Vui lòng đồng ý điều khoản xử lý nội dung để tạo bio!", "error");
      return;
    }
    setGeneratingAi(true);
    await new Promise((resolve) => setTimeout(resolve, 800));

    const selectedSubjNames = subjectsCatalog
      .filter((s) => selectedSubjectIds.includes(s.subjectId))
      .map((s) => s.name)
      .join(", ");

    let toneBio = "";
    if (aiTone === "friendly") {
      toneBio = `Chào các em và quý phụ huynh! Tôi là ${displayName || "Gia sư"}, đồng hành cùng học sinh khối ${
        educationLevels.find((l) => l.levelId === selectedLevelId)?.name || "THCS"
      } môn ${selectedSubjNames || "Toán"}. Với ${experienceYears} năm kinh nghiệm, tôi luôn kiên nhẫn giảng dạy bằng phương pháp trực quan, biến kiến thức phức tạp thành những bài học gần gũi, giúp các em tự tin bứt phá điểm số!`;
    } else if (aiTone === "professional") {
      toneBio = `Cử nhân Sư phạm với ${experienceYears} năm kinh nghiệm chuyên sâu môn ${selectedSubjNames || "Toán"} cấp ${
        educationLevels.find((l) => l.levelId === selectedLevelId)?.name || "THCS"
      }. Lộ trình giảng dạy được chuẩn hóa theo năng lực từng học sinh, cam kết nắm chắc kiến thức căn bản, rèn tư duy giải toán nâng cao và đạt điểm cao trong các kỳ thi chuyển cấp.`;
    } else {
      toneBio = `Gia sư ${selectedSubjNames || "Toán"} (${experienceYears} năm KN). Tập trung lấp lỗ hổng kiến thức, bám sát cấu trúc đề thi, rèn kỹ năng làm bài nhanh và chính xác. Tận tâm, trách nhiệm, tiến bộ rõ sau từng tuần.`;
    }

    setAiDraft(toneBio);
    setGeneratingAi(false);
  };

  const handleApplyAiDraft = () => {
    if (aiDraft) {
      setBio(aiDraft);
      setShowAiModal(false);
      showToast("Đã áp dụng bản nháp bio thành công!", "success");
    }
  };

  const handleFileSelection = (event: React.ChangeEvent<HTMLInputElement>) => {
    const incomingFiles = Array.from(event.target.files || []);
    const invalidFile = incomingFiles.find((file) => {
      const lowerName = file.name.toLowerCase();
      return !ALLOWED_CREDENTIAL_EXTENSIONS.some((extension) => lowerName.endsWith(extension));
    });
    if (invalidFile) {
      showToast(`Tệp ${invalidFile.name} không đúng định dạng được hỗ trợ.`, "error");
      event.target.value = "";
      return;
    }

    const oversizedFile = incomingFiles.find((file) => file.size > MAX_CREDENTIAL_FILE_SIZE);
    if (oversizedFile) {
      showToast(`Tệp ${oversizedFile.name} vượt quá giới hạn 10 MB.`, "error");
      event.target.value = "";
      return;
    }

    setSelectedFiles((currentFiles) => {
      const knownFiles = new Set(
        currentFiles.map((file) => `${file.name}:${file.size}:${file.lastModified}`)
      );
      return [
        ...currentFiles,
        ...incomingFiles.filter(
          (file) => !knownFiles.has(`${file.name}:${file.size}:${file.lastModified}`)
        ),
      ];
    });
    event.target.value = "";
  };

  const handleRemoveDoc = (index: number) => {
    setSelectedFiles((currentFiles) => currentFiles.filter((_, fileIndex) => fileIndex !== index));
  };

  const handleComplete = async () => {
    setSubmitting(true);
    try {
      // 1. Cập nhật hồ sơ gia sư lên Backend
      await profileService.updateTutorProfile({
        displayName: displayName.trim() || user?.fullName || "Gia sư EduFit",
        headline: headline.trim(),
        bio: bio.trim(),
        teachingMode,
        area: area.trim(),
        pricePerSession: Number(pricePerSession) || 200000,
        experienceYears: Number(experienceYears) || 1,
        teachingMethod: teachingMethod.trim(),
      });

      // 2. Cập nhật môn học giảng dạy
      if (selectedSubjectIds.length > 0) {
        for (const subId of selectedSubjectIds) {
          try {
            await profileService.addTutorSubject(subId, selectedLevelId);
          } catch {
            // bỏ qua nếu đã tồn tại
          }
        }
      }

      // 3. Gửi đúng các tệp người dùng đã chọn. Lỗi phiên hoặc lỗi upload phải được hiển thị,
      // không được nuốt lỗi rồi đánh dấu hồ sơ là đã nộp.
      await verificationService.submitVerification(
        {
          credentials: [
            {
              type: credentialType,
              institution: credentialInstitution.trim(),
              year: credentialYear,
              note: `Hồ sơ đăng ký gia sư chuyên môn ${selectedSubjectIds.length} môn`,
              fileIndexes: selectedFiles.map((_, index) => index),
            },
          ],
        },
        selectedFiles
      );

      // 4. Lưu trạng thái hoàn tất vào Local Storage
      localStorage.setItem(
        "edufit_tutor_onboarding_completed",
        JSON.stringify({
          completedAt: new Date().toISOString(),
          displayName,
          subjectsCount: selectedSubjectIds.length,
          step: "COMPLETED",
        })
      );

      showToast("Hoàn tất thiết lập hồ sơ gia sư! Hồ sơ đã được gửi để duyệt KYC.", "success");
      await refreshUser?.();
      router.push("/dashboard");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Có lỗi xảy ra khi lưu hồ sơ gia sư";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  const progressPercent = step === 1 ? 33 : step === 2 ? 66 : 100;

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="size-8 text-amber-500 animate-spin" />
          <span className="text-xs text-neutral-500 font-medium">Đang tải hồ sơ gia sư...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 font-sans flex flex-col justify-between">
      {/* 1. Header Bar */}
      <header className="bg-white border-b border-stone-200 sticky top-0 z-30 px-4 sm:px-8 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <EduFitLogo href="/" />
          <div className="hidden sm:flex items-center gap-2 pl-6 border-l border-stone-200 text-xs text-neutral-500">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 bg-amber-50 text-amber-700 font-semibold rounded-full">
              HỒ SƠ GIA SƯ
            </span>
            <span>Hoàn thiện hồ sơ và giới thiệu bản thân</span>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="hidden md:flex items-center gap-2 text-xs text-neutral-400">
            <Clock className="size-3.5" />
            <span>Đã lưu nháp lúc {lastSaved}</span>
          </div>
          <Link
            href="/dashboard"
            className="text-xs text-neutral-500 hover:text-neutral-900 transition-colors"
          >
            Bỏ qua để vào sau
          </Link>
        </div>
      </header>

      {/* 2. Main Wizard Container */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 sm:p-8 space-y-6">
        {/* Progress & Stepper Bar */}
        <div className="bg-white rounded-3xl p-5 sm:p-6 border border-stone-200/90 shadow-xs space-y-4">
          <div className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <span className="font-bold text-neutral-800">Hồ sơ của bạn</span>
              <span className="text-neutral-400">•</span>
              <span className="text-neutral-500">
                {step === 1
                  ? "Nhập chuyên môn và học phí"
                  : step === 2
                  ? "Hoàn thiện CV và bio để gửi xác minh"
                  : "Kiểm tra và gửi xác minh KYC"}
              </span>
            </div>
            <span className="font-bold text-amber-600">{progressPercent}%</span>
          </div>

          <div className="w-full bg-stone-100 rounded-full h-2 overflow-hidden">
            <div
              className="bg-amber-500 h-2 rounded-full transition-all duration-300"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <div className="grid grid-cols-3 gap-2 pt-2 border-t border-stone-100 text-xs">
            <div
              className={`flex items-center gap-2 ${
                step >= 1 ? "text-amber-600 font-bold" : "text-neutral-400"
              }`}
            >
              <div
                className={`size-6 rounded-full flex items-center justify-center text-[11px] ${
                  step > 1
                    ? "bg-amber-500 text-white"
                    : step === 1
                    ? "border-2 border-amber-500 text-amber-600"
                    : "border border-neutral-300 text-neutral-400"
                }`}
              >
                {step > 1 ? <Check className="size-3" /> : "1"}
              </div>
              <span className="hidden sm:inline">Thông tin chuyên môn</span>
            </div>

            <div
              className={`flex items-center gap-2 ${
                step >= 2 ? "text-amber-600 font-bold" : "text-neutral-400"
              }`}
            >
              <div
                className={`size-6 rounded-full flex items-center justify-center text-[11px] ${
                  step > 2
                    ? "bg-amber-500 text-white"
                    : step === 2
                    ? "border-2 border-amber-500 text-amber-600"
                    : "border border-neutral-300 text-neutral-400"
                }`}
              >
                {step > 2 ? <Check className="size-3" /> : "2"}
              </div>
              <span className="hidden sm:inline">CV & giới thiệu</span>
            </div>

            <div
              className={`flex items-center gap-2 ${
                step === 3 ? "text-amber-600 font-bold" : "text-neutral-400"
              }`}
            >
              <div
                className={`size-6 rounded-full flex items-center justify-center text-[11px] ${
                  step === 3
                    ? "border-2 border-amber-500 text-amber-600"
                    : "border border-neutral-300 text-neutral-400"
                }`}
              >
                3
              </div>
              <span className="hidden sm:inline">Gửi xác minh</span>
            </div>
          </div>
        </div>

        {/* STEP 1: THÔNG TIN CHUYÊN MÔN */}
        {step === 1 && (
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-stone-200/90 shadow-xs space-y-6">
            <div className="space-y-1">
              <span className="text-[11px] font-bold text-amber-600 uppercase tracking-wider">
                BƯỚC 1 / 3
              </span>
              <h2 className="font-heading text-2xl font-bold text-neutral-900">
                Thông tin chuyên môn & Học phí
              </h2>
              <p className="text-xs text-neutral-500">
                Lựa chọn môn dạy, khối lớp phụ trách và mức học phí mong muốn để học sinh dễ dàng tìm kiếm.
              </p>
            </div>

            <div className="space-y-5 pt-2">
              {/* Họ tên hiển thị */}
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">
                  Họ và tên gia sư hiển thị <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="VD: Cô Nguyễn Hoàng Lan"
                  className="w-full px-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-900 font-medium"
                />
              </div>

              {/* Môn học giảng dạy */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-neutral-700">
                    Môn học bạn nhận dạy <span className="text-rose-500">*</span>
                  </label>
                  <span className="text-[11px] text-neutral-400">Chọn 1 hoặc nhiều môn</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {subjectsCatalog.map((subj) => {
                    const isSelected = selectedSubjectIds.includes(subj.subjectId);
                    return (
                      <button
                        key={subj.subjectId}
                        type="button"
                        onClick={() => toggleSubject(subj.subjectId)}
                        className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                          isSelected
                            ? "bg-amber-500 text-white shadow-xs"
                            : "bg-stone-100 hover:bg-stone-200 text-neutral-700"
                        }`}
                      >
                        {isSelected && <Check className="size-3 inline-block mr-1.5" />}
                        {subj.name}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Khối lớp giảng dạy & Kinh nghiệm */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-neutral-700 mb-1.5">
                    Khối lớp học sinh trọng tâm
                  </label>
                  <select
                    value={selectedLevelId}
                    onChange={(e) => setSelectedLevelId(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 text-neutral-800"
                  >
                    {educationLevels.map((lvl) => (
                      <option key={lvl.levelId} value={lvl.levelId}>
                        {lvl.name} (Chương trình THCS)
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-neutral-700 mb-1.5">
                    Số năm kinh nghiệm gia sư
                  </label>
                  <input
                    type="number"
                    min={0}
                    max={30}
                    value={experienceYears}
                    onChange={(e) => setExperienceYears(Number(e.target.value))}
                    className="w-full px-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 text-neutral-900"
                  />
                </div>
              </div>

              {/* Học phí & Hình thức dạy */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-neutral-700 mb-1.5">
                    Mức học phí đề xuất (VNĐ / buổi học 90p)
                  </label>
                  <div className="relative">
                    <DollarSign className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="number"
                      step={10000}
                      min={100000}
                      value={pricePerSession}
                      onChange={(e) => setPricePerSession(Number(e.target.value))}
                      className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 text-neutral-900 font-semibold"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-neutral-700 mb-1.5">
                    Hình thức giảng dạy
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    {[
                      { id: "ONLINE", label: "Online" },
                      { id: "OFFLINE", label: "Trực tiếp" },
                      { id: "BOTH", label: "Cả hai" },
                    ].map((mode) => (
                      <button
                        key={mode.id}
                        type="button"
                        onClick={() => setTeachingMode(mode.id as "ONLINE" | "OFFLINE" | "BOTH")}
                        className={`py-2 text-xs font-semibold rounded-xl border text-center transition-all cursor-pointer ${
                          teachingMode === mode.id
                            ? "border-amber-500 bg-amber-50 text-amber-800"
                            : "border-stone-200 hover:bg-stone-50 text-neutral-600"
                        }`}
                      >
                        {mode.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Khu vực nhận dạy */}
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">
                  Khu vực hoạt động chính (Quận / Huyện, Thành phố) <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <MapPin className="size-4 text-neutral-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={area}
                    onChange={(e) => setArea(e.target.value)}
                    placeholder="VD: Cầu Giấy, Nam Từ Liêm, Hà Nội"
                    className="w-full pl-10 pr-4 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 text-neutral-900"
                  />
                </div>
              </div>
            </div>

            <div className="flex items-center justify-end pt-4 border-t border-stone-100">
              <button
                type="button"
                onClick={handleNextFromStep1}
                className="inline-flex items-center gap-2 px-6 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all cursor-pointer"
              >
                <span>Lưu và tiếp tục</span>
                <ArrowRight className="size-4" />
              </button>
            </div>
          </div>
        )}

        {/* STEP 2: CV, GIỚI THIỆU & PHƯƠNG PHÁP (Figma 36:1990) */}
        {step === 2 && (
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-stone-200/90 shadow-xs space-y-6">
            <div className="space-y-1">
              <span className="text-[11px] font-bold text-amber-600 uppercase tracking-wider">
                BƯỚC 2 / 3
              </span>
              <h2 className="font-heading text-2xl font-bold text-neutral-900">
                CV, giới thiệu bản thân & phương pháp giảng dạy
              </h2>
              <p className="text-xs text-neutral-500">
                Tải CV, viết bio và mô tả phương pháp giảng dạy. AI sẽ giúp bạn tạo bản nháp giới thiệu bản thân nhanh chóng.
              </p>
            </div>

            {/* Tài liệu đính kèm */}
            <div className="space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label htmlFor="credential-type" className="text-xs font-bold text-neutral-700">
                    Loại giấy tờ
                  </label>
                  <select
                    id="credential-type"
                    value={credentialType}
                    onChange={(event) => setCredentialType(event.target.value as typeof credentialType)}
                    className="mt-1.5 w-full px-3 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500"
                  >
                    <option value="DEGREE">Bằng cấp</option>
                    <option value="CERTIFICATE">Chứng chỉ</option>
                    <option value="IDENTITY_CARD">Giấy tờ định danh</option>
                  </select>
                </div>
                <div className="sm:col-span-2">
                  <label htmlFor="credential-institution" className="text-xs font-bold text-neutral-700">
                    Đơn vị cấp
                  </label>
                  <input
                    id="credential-institution"
                    type="text"
                    value={credentialInstitution}
                    onChange={(event) => setCredentialInstitution(event.target.value)}
                    placeholder="Ví dụ: Trường Đại học Sư phạm Hà Nội"
                    maxLength={200}
                    className="mt-1.5 w-full px-3 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500"
                  />
                </div>
                <div>
                  <label htmlFor="credential-year" className="text-xs font-bold text-neutral-700">
                    Năm cấp
                  </label>
                  <input
                    id="credential-year"
                    type="number"
                    min={1950}
                    max={new Date().getFullYear() + 10}
                    value={credentialYear}
                    onChange={(event) => setCredentialYear(Number(event.target.value))}
                    className="mt-1.5 w-full px-3 py-2.5 text-xs bg-stone-50 border border-stone-200 rounded-xl focus:bg-white focus:outline-hidden focus:border-amber-500"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-neutral-700">
                  Bằng cấp / Minh chứng chuyên môn (có thể tải nhiều tệp)
                </label>
                <span className="text-[11px] text-neutral-400">PDF, JPG hoặc PNG • Tối đa 10 MB</span>
              </div>

              <div className="space-y-2">
                {selectedFiles.map((file, index) => (
                  <div
                    key={`${file.name}-${file.size}-${file.lastModified}`}
                    className="flex items-center justify-between p-3.5 bg-stone-50 border border-stone-200 rounded-2xl text-xs"
                  >
                    <div className="flex items-center gap-3">
                      <div className="size-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold">
                        <FileText className="size-4" />
                      </div>
                      <div>
                        <p className="font-bold text-neutral-800">{file.name}</p>
                        <p className="text-[11px] text-neutral-400">
                          {(file.size / 1024 / 1024).toFixed(2)} MB • Sẵn sàng tải lên
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => handleRemoveDoc(index)}
                        className="text-stone-400 hover:text-rose-500 p-1"
                        aria-label={`Xóa tệp ${file.name}`}
                      >
                        <Trash2 className="size-3.5" />
                      </button>
                    </div>
                  </div>
                ))}

                <label
                  htmlFor="tutor-credential-files"
                  className="w-full py-3 border-2 border-dashed border-stone-200 hover:border-amber-400 rounded-2xl flex items-center justify-center gap-2 text-xs font-semibold text-neutral-600 hover:text-amber-600 transition-colors bg-stone-50/50 cursor-pointer"
                >
                  <Upload className="size-4" />
                  <span>Thêm CV hoặc minh chứng khác</span>
                  <input
                    id="tutor-credential-files"
                    type="file"
                    multiple
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={handleFileSelection}
                    className="sr-only"
                  />
                </label>
              </div>
            </div>

            {/* Giới thiệu bản thân + Nút AI Assistant */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-neutral-700">
                  Giới thiệu bản thân (Bio) <span className="text-rose-500">*</span>
                </label>
                <div className="flex items-center gap-3">
                  <span className="text-[11px] text-neutral-400">
                    {bio.length} / 1.500 ký tự
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowAiModal(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-50 hover:bg-amber-100 text-amber-700 font-bold text-[11px] rounded-full transition-colors cursor-pointer"
                  >
                    <Sparkles className="size-3 text-amber-500" />
                    <span>Viết bio với AI từ CV</span>
                  </button>
                </div>
              </div>

              <textarea
                rows={4}
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Mô tả quá trình giảng dạy, thế mạnh chuyên môn và sự tận tâm dành cho học trò..."
                className="w-full p-4 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-800 leading-relaxed resize-none font-sans"
              />
            </div>

            {/* Phương pháp giảng dạy */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-neutral-700">
                  Phương pháp giảng dạy
                </label>
                <span className="text-[11px] text-neutral-400">Hiển thị công khai</span>
              </div>
              <textarea
                rows={3}
                value={teachingMethod}
                onChange={(e) => setTeachingMethod(e.target.value)}
                placeholder="Mô tả cách tổ chức buổi học, cách giải thích và hỗ trợ học sinh tiến bộ..."
                className="w-full p-4 text-xs bg-stone-50 border border-stone-200 rounded-2xl focus:bg-white focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 text-neutral-800 leading-relaxed resize-none font-sans"
              />
              <p className="text-[11px] text-neutral-400">
                Gợi ý: Mô tả cách tổ chức buổi học, cách phát hiện lỗ hổng kiến thức và động viên học sinh.
              </p>
            </div>

            {/* AI Assistant Modal / Sub-card */}
            {showAiModal && (
              <div className="p-5 bg-gradient-to-br from-amber-50/80 to-orange-50/50 border border-amber-200 rounded-3xl space-y-4 animate-in fade-in">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Sparkles className="size-4 text-amber-600" />
                    <h4 className="text-xs font-bold text-neutral-900">
                      Viết bio với AI từ CV (Tạo bản nháp, không tự lưu)
                    </h4>
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowAiModal(false)}
                    className="text-neutral-400 hover:text-neutral-700 text-xs"
                  >
                    Đóng
                  </button>
                </div>

                <p className="text-[11px] text-neutral-600 leading-relaxed">
                  AI sẽ dùng thông tin chuyên môn ({subjectsCatalog.filter((s) => selectedSubjectIds.includes(s.subjectId)).map((s) => s.name).join(", ")}) và CV đã nhập để tạo bản thảo giới thiệu thu hút.
                </p>

                <div className="space-y-2">
                  <label className="text-xs font-bold text-neutral-700">Giọng văn (Tone)</label>
                  <div className="flex gap-2">
                    {[
                      { id: "friendly", label: "Thân thiện & Gần gũi" },
                      { id: "professional", label: "Chuyên nghiệp & Chuẩn mực" },
                      { id: "concise", label: "Ngắn gọn & Trọng tâm" },
                    ].map((tone) => (
                      <button
                        key={tone.id}
                        type="button"
                        onClick={() => setAiTone(tone.id as typeof aiTone)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                          aiTone === tone.id
                            ? "bg-amber-600 text-white shadow-xs"
                            : "bg-white border border-stone-200 text-neutral-700 hover:bg-stone-50"
                        }`}
                      >
                        {tone.label}
                      </button>
                    ))}
                  </div>
                </div>

                <label className="flex items-center gap-2 text-xs text-neutral-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={aiAgreed}
                    onChange={(e) => setAiAgreed(e.target.checked)}
                    className="rounded text-amber-600 focus:ring-amber-500"
                  />
                  <span>Tôi đồng ý để EduFit xử lý nội dung để tạo bio nháp.</span>
                </label>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={generatingAi}
                    onClick={handleGenerateAiBio}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold rounded-xl shadow-xs transition-all disabled:opacity-50 cursor-pointer"
                  >
                    {generatingAi ? (
                      <Loader2 className="size-3.5 animate-spin" />
                    ) : (
                      <Sparkles className="size-3.5" />
                    )}
                    <span>{generatingAi ? "AI đang tạo..." : "Tạo bio với AI"}</span>
                  </button>
                </div>

                {aiDraft && (
                  <div className="p-4 bg-white border border-amber-200 rounded-2xl space-y-3">
                    <div className="flex items-center justify-between text-[11px] text-amber-800 font-bold">
                      <span>BẢN NHÁP AI GỢI Ý</span>
                      <span className="text-neutral-400 font-normal">Vừa tạo</span>
                    </div>
                    <p className="text-xs text-neutral-800 leading-relaxed italic bg-stone-50/80 p-3 rounded-xl border border-stone-100">
                      &ldquo;{aiDraft}&rdquo;
                    </p>
                    <div className="flex items-center gap-2 pt-1">
                      <button
                        type="button"
                        onClick={handleApplyAiDraft}
                        className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
                      >
                        Dùng bản nháp này
                      </button>
                      <button
                        type="button"
                        onClick={handleGenerateAiBio}
                        className="px-3 py-1.5 bg-stone-100 hover:bg-stone-200 text-neutral-700 font-medium text-xs rounded-xl cursor-pointer"
                      >
                        Tạo lại
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-stone-100">
              <button
                type="button"
                onClick={() => setStep(1)}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-neutral-600 hover:text-neutral-900 cursor-pointer"
              >
                <ArrowLeft className="size-4" />
                <span>Quay lại</span>
              </button>

              <button
                type="button"
                onClick={handleNextFromStep2}
                className="inline-flex items-center gap-2 px-6 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all cursor-pointer"
              >
                <span>Lưu và tiếp tục</span>
                <ArrowRight className="size-4" />
              </button>
            </div>
          </div>
        )}

        {/* STEP 3: GỬI XÁC MINH KYC */}
        {step === 3 && (
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-stone-200/90 shadow-xs space-y-6">
            <div className="space-y-1">
              <span className="text-[11px] font-bold text-amber-600 uppercase tracking-wider">
                BƯỚC 3 / 3
              </span>
              <h2 className="font-heading text-2xl font-bold text-neutral-900">
                Xác thực KYC & Gửi duyệt hồ sơ
              </h2>
              <p className="text-xs text-neutral-500">
                Kiểm tra lại toàn bộ thông tin đã nhập trước khi gửi đến đội ngũ Quản trị viên EduFit kiểm duyệt.
              </p>
            </div>

            {/* Summary Review Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 bg-stone-50 border border-stone-200 rounded-2xl space-y-2">
                <span className="text-[11px] font-bold text-neutral-400 uppercase">
                  Chuyên môn & Học phí
                </span>
                <p className="text-sm font-bold text-neutral-900">{displayName}</p>
                <p className="text-xs text-neutral-600">
                  Môn dạy:{" "}
                  <strong>
                    {subjectsCatalog
                      .filter((s) => selectedSubjectIds.includes(s.subjectId))
                      .map((s) => s.name)
                      .join(", ") || "Chưa chọn"}
                  </strong>
                </p>
                <p className="text-xs text-neutral-600">
                  Cấp học:{" "}
                  {educationLevels.find((l) => l.levelId === selectedLevelId)?.name || "Chưa chọn"}
                </p>
                <p className="text-xs text-amber-700 font-semibold">
                  Học phí: {pricePerSession.toLocaleString("vi-VN")} đ / buổi
                </p>
              </div>

              <div className="p-4 bg-stone-50 border border-stone-200 rounded-2xl space-y-2">
                <span className="text-[11px] font-bold text-neutral-400 uppercase">
                  Địa điểm & Hình thức
                </span>
                <p className="text-xs text-neutral-700">
                  Khu vực: <strong>{area}</strong>
                </p>
                <p className="text-xs text-neutral-700">
                  Hình thức:{" "}
                  <strong>
                    {teachingMode === "BOTH"
                      ? "Trực tuyến & Trực tiếp"
                      : teachingMode === "ONLINE"
                      ? "Trực tuyến (Online)"
                      : "Trực tiếp (Tại nhà)"}
                  </strong>
                </p>
                <p className="text-xs text-neutral-700">
                  Kinh nghiệm: <strong>{experienceYears} năm</strong>
                </p>
                <p className="text-xs text-emerald-700 font-semibold flex items-center gap-1">
                  <ShieldCheck className="size-3.5" />
                  Đính kèm {selectedFiles.length} tệp minh chứng
                </p>
              </div>
            </div>

            <div className="p-4 bg-amber-50/60 border border-amber-200 rounded-2xl space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-amber-900">
                <ShieldCheck className="size-4 text-amber-600" />
                <span>Quy trình kiểm duyệt an toàn của EduFit</span>
              </div>
              <p className="text-[11px] text-amber-800 leading-relaxed">
                Sau khi gửi hồ sơ, Admin EduFit sẽ xem xét chứng chỉ và thông tin của bạn trong vòng 24-48 giờ làm việc. Trong thời gian này, bạn vẫn có thể truy cập Bảng điều khiển để cập nhật lịch rảnh và chuẩn bị giáo án.
              </p>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-stone-100">
              <button
                type="button"
                onClick={() => setStep(2)}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-neutral-600 hover:text-neutral-900 cursor-pointer"
              >
                <ArrowLeft className="size-4" />
                <span>Quay lại</span>
              </button>

              <button
                type="button"
                disabled={submitting}
                onClick={handleComplete}
                className="inline-flex items-center gap-2 px-7 py-3 bg-emerald-600 hover:bg-emerald-700 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-xs transition-all disabled:opacity-50 cursor-pointer"
              >
                {submitting ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <CheckCircle2 className="size-4" />
                )}
                <span>{submitting ? "Đang gửi hồ sơ..." : "Hoàn tất & Gửi duyệt KYC"}</span>
              </button>
            </div>
          </div>
        )}
      </main>

      {/* 3. Footer */}
      <footer className="border-t border-stone-200 py-4 px-6 text-center text-[11px] text-neutral-400">
        © 2026 EduFit Platform. Bảo mật thông tin gia sư & cam kết chất lượng giáo dục.
      </footer>
    </div>
  );
}
