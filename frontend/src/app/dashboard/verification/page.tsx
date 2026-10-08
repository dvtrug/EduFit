"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { verificationService, VerificationRequestDetail, VerificationStatus } from "@/services/verification";
import { TutorVerificationBanner } from "@/features/verification/components/TutorVerificationBanner";
import { UploadCredentialModal } from "@/features/verification/components/UploadCredentialModal";
import { ShieldCheck, Calendar, FileText, Upload, Loader2 } from "lucide-react";

/**
 * =========================================================================
 * SPRING BOOT API INTEGRATION:
 * =========================================================================
 * - Get My Request: GET /api/v1/verifications/my-request
 * - Submit Request: POST /api/v1/verifications (Multipart payload + files)
 * - Security: @PreAuthorize("hasRole('TUTOR')")
 * =========================================================================
 */
export default function TutorVerificationPage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  const [requestDetail, setRequestDetail] = useState<VerificationRequestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  const effectiveUser = user || {
    id: "demo-tutor-id",
    email: "tutor.demo@edufit.vn",
    fullName: "Nguyễn Hoàng Nam (Gia sư Demo)",
    role: "TUTOR",
    status: "ACTIVE",
  };

  const loadMyRequest = async () => {
    setLoading(true);
    try {
      const data = await verificationService.getMyRequest();
      setRequestDetail(data);
    } catch (err) {
      console.error("Lỗi khi tải hồ sơ KYC gia sư:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMyRequest();
  }, []);

  const handleLogout = async () => {
    if (user) {
      await logout();
    }
    router.push("/login");
  };

  const status: VerificationStatus = requestDetail?.status || "DRAFT";

  if (isLoading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="size-8 border-3 border-sky-500/30 border-t-sky-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <DashboardShell user={effectiveUser} role="TUTOR" onLogout={handleLogout}>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Banner trạng thái */}
        <TutorVerificationBanner status={status} onOpenUploadModal={() => setIsUploadOpen(true)} />

        {/* Khung chi tiết hồ sơ xác minh */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-stone-200/90 shadow-xs space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-100">
            <div>
              <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-sky-50 text-sky-900 rounded-full text-xs font-bold mb-2">
                <ShieldCheck className="size-3.5 text-sky-600" />
                <span>Hồ Sơ Chứng Minh Năng Lực (KYC)</span>
              </div>
              <h3 className="font-heading text-xl font-bold text-neutral-900">
                Bằng cấp & Giấy tờ đã nộp
              </h3>
              <p className="text-xs text-neutral-500 mt-0.5">
                Các tài liệu được mã hóa và bảo mật, dùng để kiểm duyệt huy hiệu Gia Sư Uy Tín
              </p>
            </div>

            <button
              type="button"
              onClick={() => setIsUploadOpen(true)}
              className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors shrink-0"
            >
              <Upload className="size-3.5" />
              <span>Nộp hồ sơ / Cập nhật</span>
            </button>
          </div>

          {loading ? (
            <div className="p-8 flex flex-col items-center justify-center gap-2">
              <Loader2 className="size-5 text-sky-600 animate-spin" />
              <span className="text-xs text-neutral-400">Đang kiểm tra trạng thái hồ sơ...</span>
            </div>
          ) : requestDetail && requestDetail.credentials && requestDetail.credentials.length > 0 ? (
            <div className="space-y-3">
              {requestDetail.credentials.map((cred) => (
                <div
                  key={cred.credentialId}
                  className="p-4 rounded-2xl border border-stone-200/80 bg-stone-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-sky-100 text-sky-900">
                        {cred.type}
                      </span>
                      <h4 className="text-xs font-bold text-neutral-900">{cred.institution}</h4>
                    </div>
                    {cred.year && (
                      <p className="text-[11px] text-neutral-500">Năm cấp bằng: {cred.year}</p>
                    )}
                    {cred.note && (
                      <p className="text-xs text-neutral-600 italic font-sans">{`"${cred.note}"`}</p>
                    )}
                  </div>

                  <div className="text-xs text-neutral-400 flex items-center gap-1.5 shrink-0">
                    <FileText className="size-4 text-sky-600" />
                    <span>{cred.files?.length || 0} tệp đính kèm</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="py-8 text-center text-xs text-neutral-400 border border-dashed border-stone-200 rounded-2xl">
              Bạn chưa nộp hồ sơ bằng cấp nào. Bấm nút phía trên để bắt đầu xác minh.
            </div>
          )}
        </div>
      </div>

      <UploadCredentialModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onSuccess={() => {
          loadMyRequest();
        }}
      />
    </DashboardShell>
  );
}
