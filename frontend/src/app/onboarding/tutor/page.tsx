import { Metadata } from "next";
import { TutorOnboardingWizard } from "@/features/onboarding/components/TutorOnboardingWizard";

export const metadata: Metadata = {
  title: "Thiết lập hồ sơ gia sư | EduFit",
  description: "Hoàn thiện thông tin chuyên môn, học phí, CV và giới thiệu bản thân trước khi vào Bảng điều khiển.",
};

export default function TutorOnboardingPage() {
  return <TutorOnboardingWizard />;
}
