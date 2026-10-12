"use client";

import React, { useState } from "react";
import {
  Sparkles,
  X,
  Check,
  RotateCcw,
  Copy,
  BookOpen,
  GraduationCap,
  Clock,
  Loader2,
  Smile,
  Award,
  Zap,
} from "lucide-react";
import { useToast } from "@/shared/ui/Toast";

export interface AiBioAssistantModalProps {
  isOpen: boolean;
  onClose: () => void;
  displayName?: string;
  subjects?: string[];
  educationLevels?: string[];
  experienceYears?: number;
  currentBio?: string;
  onApplyBio: (bio: string) => void;
}

export type AiTone = "friendly" | "professional" | "concise";

export function AiBioAssistantModal({
  isOpen,
  onClose,
  displayName = "Gia sư",
  subjects = [],
  educationLevels = [],
  experienceYears = 1,
  currentBio = "",
  onApplyBio,
}: AiBioAssistantModalProps) {
  const { showToast } = useToast();

  const [tone, setTone] = useState<AiTone>("friendly");
  const [focusArea, setFocusArea] = useState<string>("exam"); // "exam" | "foundation" | "mindset"
  const [isGenerating, setIsGenerating] = useState(false);
  const [draftBio, setDraftBio] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [agreed, setAgreed] = useState(true);

  if (!isOpen) return null;

  const subjectNamesText = subjects.length > 0 ? subjects.join(", ") : "các môn học";
  const levelsText = educationLevels.length > 0 ? educationLevels.join(", ") : "các khối lớp";

  const handleGenerate = async () => {
    if (!agreed) {
      showToast("Vui lòng đồng ý điều khoản để tạo bản thảo Bio!", "error");
      return;
    }

    setIsGenerating(true);
    // Tạo độ trễ ngắn mang lại cảm giác AI đang phân tích dữ liệu chuyên môn
    await new Promise((resolve) => setTimeout(resolve, 600));

    const name = displayName.trim() || "Gia sư";
    const exp = experienceYears > 0 ? `${experienceYears} năm` : "nhiều năm";

    let generatedText = "";

    if (tone === "friendly") {
      if (focusArea === "foundation") {
        generatedText = `Chào các em học sinh và quý phụ huynh! Thầy/Cô là ${name}, chuyên giảng dạy ${subjectNamesText} (${levelsText}). Với ${exp} kinh nghiệm, phong cách giảng dạy của tôi luôn lấy sự kiên nhẫn và thấu hiểu làm trọng tâm, giúp các em xóa bỏ nỗi sợ mất gốc, tìm lại niềm vui và sự tự tin trong từng buổi học!`;
      } else if (focusArea === "mindset") {
        generatedText = `Chào các em và quý phụ huynh! Tôi là ${name}, đồng hành cùng học sinh ${levelsText} môn ${subjectNamesText}. Với ${exp} giảng dạy, tôi không chỉ truyền đạt kiến thức mà còn rèn luyện tư duy tự học, phương pháp giải quyết vấn đề logic để các em chủ động bứt phá thành tích lâu dài.`;
      } else {
        generatedText = `Chào các em và quý phụ huynh! Tôi là ${name}, gia sư chuyên trách môn ${subjectNamesText} (${levelsText}). Với ${exp} đồng hành cùng nhiều lứa học sinh, tôi luôn kiên nhẫn giảng giải trực quan, biến kiến thức phức tạp thành bài học gần gũi, giúp các em tự tin vượt qua các kỳ thi quan trọng với điểm số cao nhất!`;
      }
    } else if (tone === "professional") {
      if (focusArea === "exam") {
        generatedText = `Cử nhân chuyên ngành Sư phạm với ${exp} kinh nghiệm luyện thi môn ${subjectNamesText} cấp ${levelsText}. Giáo trình giảng dạy được thiết kế chuẩn hóa, bám sát ma trận đề thi chính thức, chú trọng rèn luyện kỹ năng phân tích đề, xử lý câu hỏi phân hóa và cam kết mục tiêu điểm 8+ đến 9+.`;
      } else if (focusArea === "foundation") {
        generatedText = `Giáo viên chuyên môn môn ${subjectNamesText} (${levelsText}) với ${exp} kinh nghiệm sư phạm. Phương pháp giảng dạy khoa học: khảo sát năng lực ban đầu, xây dựng lộ trình lấp lỗ hổng kiến thức cá nhân hóa, rèn nề nếp học tập nghiêm túc và có báo cáo tiến độ chi tiết tới phụ huynh sau từng tuần.`;
      } else {
        generatedText = `Chuyên gia giảng dạy môn ${subjectNamesText} (${levelsText}) với ${exp} kinh nghiệm đào tạo học sinh giỏi và luyện thi chuyển cấp. Triết lý đào tạo hướng đến xây dựng nền tảng tư duy độc lập, chuẩn hóa phương pháp giải toán/khoa học hiện đại và phát triển tối đa năng lực cá nhân của học sinh.`;
      }
    } else {
      // Concise
      if (focusArea === "foundation") {
        generatedText = `Gia sư ${subjectNamesText} (${levelsText}) - ${exp} kinh nghiệm. Chuyên trị mất gốc, củng cố căn bản nhanh sau 4-6 tuần, phương pháp dễ hiểu, tận tâm và trách nhiệm cao.`;
      } else if (focusArea === "exam") {
        generatedText = `Gia sư ${subjectNamesText} (${levelsText}) - ${exp} kinh nghiệm. Bám sát cấu trúc đề thi, rèn mẹo giải nhanh, quản lý thời gian thi hiệu quả, cam kết tiến bộ rõ rệt qua từng tháng.`;
      } else {
        generatedText = `Gia sư ${subjectNamesText} (${levelsText}) (${exp} KN). Đào tạo tư duy logic, đồng hành 1-1 kiên nhẫn, rèn kỹ năng tự học và giải bài tập chuẩn xác. Phản hồi tiến độ định kỳ tới phụ huynh.`;
      }
    }

    setDraftBio(generatedText);
    setIsGenerating(false);
  };

  const handleApply = () => {
    if (draftBio) {
      onApplyBio(draftBio);
      showToast("Đã áp dụng bản thảo Bio vào hồ sơ thành công!", "success");
      onClose();
    }
  };

  const handleCopy = async () => {
    if (draftBio) {
      await navigator.clipboard.writeText(draftBio);
      setCopied(true);
      showToast("Đã sao chép vào bộ nhớ tạm!", "info");
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-900/50 backdrop-blur-xs animate-in fade-in duration-150">
      <div
        className="relative w-full max-w-xl bg-white rounded-3xl shadow-2xl border border-stone-200 overflow-hidden flex flex-col max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 1. Header Modal */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-stone-100 bg-gradient-to-r from-amber-500/10 via-orange-500/5 to-transparent">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-amber-500 text-white rounded-xl shadow-xs">
              <Sparkles className="size-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-neutral-900 flex items-center gap-1.5">
                <span>Trợ lý AI viết Bio gia sư</span>
                <span className="text-[10px] font-semibold uppercase px-2 py-0.5 bg-amber-100 text-amber-800 rounded-full">
                  EduFit AI
                </span>
              </h3>
              <p className="text-[11px] text-neutral-500">
                Tạo bản giới thiệu thu hút phụ huynh dựa trên thông tin chuyên môn của bạn
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 hover:bg-stone-100 rounded-xl transition-colors cursor-pointer"
          >
            <X className="size-4" />
          </button>
        </div>

        {/* 2. Modal Body */}
        <div className="p-6 overflow-y-auto space-y-5 text-xs">
          {/* Thông tin chuyên môn đầu vào */}
          <div className="p-3 bg-stone-50 rounded-2xl border border-stone-200/70 space-y-1.5">
            <span className="text-[10px] font-bold uppercase tracking-wider text-neutral-400 block">
              Dữ liệu chuyên môn được áp dụng
            </span>
            <div className="flex flex-wrap items-center gap-3 text-neutral-700">
              <div className="flex items-center gap-1">
                <BookOpen className="size-3 text-amber-600" />
                <span>Môn: <strong>{subjectNamesText}</strong></span>
              </div>
              <div className="flex items-center gap-1">
                <GraduationCap className="size-3 text-sky-600" />
                <span>Khối: <strong>{levelsText}</strong></span>
              </div>
              <div className="flex items-center gap-1">
                <Clock className="size-3 text-emerald-600" />
                <span>Kinh nghiệm: <strong>{experienceYears} năm</strong></span>
              </div>
            </div>
          </div>

          {/* Chọn Giọng văn (Tone) */}
          <div className="space-y-2">
            <label className="font-bold text-neutral-800 block">
              1. Chọn phong cách giọng văn (Tone)
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                {
                  id: "friendly" as AiTone,
                  label: "Thân thiện",
                  desc: "Gần gũi, kiên nhẫn",
                  icon: Smile,
                },
                {
                  id: "professional" as AiTone,
                  label: "Chuyên nghiệp",
                  desc: "Chuẩn mực sư phạm",
                  icon: Award,
                },
                {
                  id: "concise" as AiTone,
                  label: "Ngắn gọn",
                  desc: "Trọng tâm, súc tích",
                  icon: Zap,
                },
              ].map((item) => {
                const Icon = item.icon;
                const isSelected = tone === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setTone(item.id)}
                    className={`p-3 rounded-2xl border text-left transition-all cursor-pointer ${
                      isSelected
                        ? "border-amber-500 bg-amber-50/70 text-amber-900 shadow-xs"
                        : "border-stone-200 hover:border-stone-300 bg-white text-neutral-600"
                    }`}
                  >
                    <Icon className={`size-4 mb-1 ${isSelected ? "text-amber-600" : "text-neutral-400"}`} />
                    <span className="font-bold block text-xs">{item.label}</span>
                    <span className="text-[10px] text-neutral-400 block mt-0.5">{item.desc}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Chọn Điểm nhấn trọng tâm */}
          <div className="space-y-2">
            <label className="font-bold text-neutral-800 block">
              2. Điểm nhấn giảng dạy mong muốn
            </label>
            <div className="flex flex-wrap gap-2">
              {[
                { id: "exam", label: "Luyện thi & Bứt phá điểm số" },
                { id: "foundation", label: "Lấy lại căn bản & Xóa mất gốc" },
                { id: "mindset", label: "Rèn luyện tư duy tự học & Logic" },
              ].map((f) => (
                <button
                  key={f.id}
                  type="button"
                  onClick={() => setFocusArea(f.id)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                    focusArea === f.id
                      ? "bg-neutral-800 text-white shadow-xs"
                      : "bg-stone-100 hover:bg-stone-200 text-neutral-700"
                  }`}
                >
                  {focusArea === f.id && <Check className="size-3 inline-block mr-1" />}
                  {f.label}
                </button>
              ))}
            </div>
          </div>

          {/* Điều khoản */}
          <label className="flex items-center gap-2 text-[11px] text-neutral-600 cursor-pointer pt-1">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="rounded text-amber-600 focus:ring-amber-500"
            />
            <span>Đồng ý để EduFit xử lý thông tin để tạo bản thảo bio tùy biến.</span>
          </label>

          {/* Nút Tạo Bio */}
          <div>
            <button
              type="button"
              disabled={isGenerating}
              onClick={handleGenerate}
              className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-amber-500 hover:bg-amber-600 active:scale-[0.99] text-white font-bold rounded-xl shadow-xs transition-all disabled:opacity-50 cursor-pointer"
            >
              {isGenerating ? (
                <>
                  <Loader2 className="size-3.5 animate-spin" />
                  <span>AI đang soạn thảo bio...</span>
                </>
              ) : (
                <>
                  <Sparkles className="size-3.5" />
                  <span>{draftBio ? "Soạn thảo lại với AI" : "Tạo bản thảo Bio bằng AI"}</span>
                </>
              )}
            </button>
          </div>

          {/* Hiển thị Bản thảo AI gợi ý */}
          {draftBio && (
            <div className="p-4 bg-gradient-to-br from-amber-50/50 to-orange-50/30 border border-amber-200/90 rounded-2xl space-y-3 animate-in fade-in">
              <div className="flex items-center justify-between text-[11px]">
                <span className="font-bold text-amber-900 flex items-center gap-1.5">
                  <Sparkles className="size-3 text-amber-600" />
                  BẢN THẢO GỢI Ý
                </span>
                <span className="text-neutral-400 font-medium">{draftBio.length} ký tự</span>
              </div>

              <p className="text-xs text-neutral-800 leading-relaxed italic bg-white p-3.5 rounded-xl border border-stone-200 shadow-2xs">
                &ldquo;{draftBio}&rdquo;
              </p>

              <div className="flex items-center justify-between gap-2 pt-1">
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={handleApply}
                    className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow-xs transition-colors cursor-pointer"
                  >
                    <Check className="size-3.5" />
                    <span>Dùng bản thảo này</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleCopy}
                    className="inline-flex items-center gap-1.5 px-3 py-2 bg-white hover:bg-stone-50 border border-stone-200 text-neutral-700 rounded-xl transition-colors cursor-pointer"
                    title="Sao chép văn bản"
                  >
                    {copied ? <Check className="size-3.5 text-emerald-600" /> : <Copy className="size-3.5" />}
                    <span>{copied ? "Đã chép" : "Sao chép"}</span>
                  </button>
                </div>

                <button
                  type="button"
                  onClick={handleGenerate}
                  className="inline-flex items-center gap-1 text-[11px] text-neutral-500 hover:text-neutral-900 cursor-pointer"
                >
                  <RotateCcw className="size-3" />
                  <span>Đổi bản khác</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 3. Footer Modal */}
        <div className="px-6 py-3 border-t border-stone-100 bg-stone-50/60 flex items-center justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 text-xs font-semibold text-neutral-500 hover:text-neutral-800 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
