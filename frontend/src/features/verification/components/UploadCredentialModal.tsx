"use client";

import React, { useState } from "react";
import { X, UploadCloud, FileText, CheckCircle2 } from "lucide-react";
import { verificationService } from "@/services/verification";

interface UploadCredentialModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function UploadCredentialModal({ isOpen, onClose, onSuccess }: UploadCredentialModalProps) {
  const [institutionName, setInstitutionName] = useState("");
  const [graduationYear, setGraduationYear] = useState<number>(2024);
  const [documentType, setDocumentType] = useState("DEGREE");
  const [notes, setNotes] = useState("");
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files);
      setSelectedFiles(files);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!institutionName.trim()) {
      setError("Vui lòng nhập tên trường / đơn vị cấp bằng.");
      return;
    }
    if (selectedFiles.length === 0) {
      setError("Vui lòng tải lên ít nhất 1 ảnh chụp bằng cấp hoặc tài liệu chứng minh.");
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      await verificationService.submitVerification(
        {
          credentials: [
            {
              type: documentType === "CERTIFICATE" ? "CERTIFICATE" : documentType === "IDENTITY_CARD" ? "IDENTITY_CARD" : "DEGREE",
              institution: institutionName.trim(),
              year: graduationYear,
              note: notes.trim() || undefined,
              fileIndexes: selectedFiles.map((_, idx) => idx),
            },
          ],
        },
        selectedFiles
      );
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const e = err as { message?: string };
      setError(e.message || "Không thể gửi hồ sơ xác minh. Vui lòng thử lại.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-neutral-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between pb-3 border-b border-stone-100">
          <div>
            <h3 className="font-heading text-lg font-bold text-neutral-900">
              Nộp hồ sơ xác minh gia sư (KYC)
            </h3>
            <p className="text-xs text-neutral-500 mt-0.5">
              Hồ sơ của bạn sẽ được bảo mật và chỉ dùng cho mục đích xác thực danh tính.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl hover:bg-stone-100 text-neutral-400 hover:text-neutral-700 transition-colors"
          >
            <X className="size-5" />
          </button>
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs font-semibold">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1">Loại giấy tờ xác minh</label>
            <select
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 bg-stone-50 text-xs font-medium focus:bg-white focus:outline-sky-500"
            >
              <option value="DEGREE">Bằng Cử nhân / Thạc sĩ / Tiến sĩ</option>
              <option value="STUDENT_CARD">Thẻ Sinh viên (Dành cho gia sư đang là SV)</option>
              <option value="TEACHING_LICENCE">Chứng chỉ Sư phạm / Nghiệp vụ</option>
              <option value="CERTIFICATE">Chứng chỉ Ngoại ngữ (IELTS, TOEIC...) / Giải thưởng</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1">Trường đại học / Tổ chức cấp</label>
            <input
              type="text"
              value={institutionName}
              onChange={(e) => setInstitutionName(e.target.value)}
              placeholder="Ví dụ: Đại học Sư phạm Hà Nội"
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 bg-stone-50 text-xs font-medium focus:bg-white focus:outline-sky-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-700 mb-1">Năm tốt nghiệp (hoặc dự kiến)</label>
              <input
                type="number"
                value={graduationYear}
                onChange={(e) => setGraduationYear(Number(e.target.value))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 bg-stone-50 text-xs font-medium focus:bg-white focus:outline-sky-500"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-neutral-700 mb-1">Ghi chú thêm</label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Tùy chọn..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 bg-stone-50 text-xs font-medium focus:bg-white focus:outline-sky-500"
              />
            </div>
          </div>

          {/* File Upload Box */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 mb-1">Tải ảnh bằng cấp / chứng chỉ</label>
            <label className="border-2 border-dashed border-stone-200 hover:border-sky-400 rounded-2xl p-4 flex flex-col items-center justify-center cursor-pointer transition-colors bg-stone-50/50">
              <UploadCloud className="size-6 text-sky-600 mb-1" />
              <span className="text-xs font-semibold text-neutral-700">Chọn tệp (JPG, PNG, PDF)</span>
              <span className="text-[10px] text-neutral-400 mt-0.5">Tối đa 10MB/tệp</span>
              <input type="file" onChange={handleFileChange} multiple accept="image/*,.pdf" className="hidden" />
            </label>
            {selectedFiles.length > 0 && (
              <div className="mt-2 space-y-1">
                {selectedFiles.map((f, i) => (
                  <div key={i} className="text-xs text-neutral-600 flex items-center gap-1.5 bg-stone-100 px-2.5 py-1 rounded-lg">
                    <FileText className="size-3 text-sky-600" />
                    <span className="truncate">{f.name}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="pt-2 flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-stone-200 hover:bg-stone-50 text-neutral-700 text-xs font-bold transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors flex items-center gap-1.5 disabled:opacity-50"
            >
              {isSubmitting ? (
                <span>Đang gửi hồ sơ...</span>
              ) : (
                <>
                  <CheckCircle2 className="size-4" />
                  <span>Xác nhận nộp</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
