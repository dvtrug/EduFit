import { Metadata } from "next";
import { ParentOnboardingWizard } from "@/features/onboarding/components/ParentOnboardingWizard";

export const metadata: Metadata = {
  title: "Liên kết hồ sơ con | EduFit",
  description: "Ghép nối tài khoản học sinh của con để theo dõi tiến độ học tập và thông báo lịch học trên EduFit.",
};

export default function ParentOnboardingPage() {
  return <ParentOnboardingWizard />;
}
