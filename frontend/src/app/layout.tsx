import type { Metadata } from "next";
import { Bricolage_Grotesque, Inter } from "next/font/google";
import { AuthProvider } from "@/context/AuthContext";
import { ToastProvider } from "@/shared/ui/Toast";
import "./globals.css";

const bricolage = Bricolage_Grotesque({
  variable: "--font-heading",
  subsets: ["latin", "vietnamese"],
  display: "swap",
});

const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin", "vietnamese"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "EduFit - Học đúng cách, Tiến bộ rõ ràng",
  description: "EduFit giúp bạn tìm gia sư hợp mục tiêu, học theo kế hoạch riêng và nhìn thấy mình tốt lên sau mỗi buổi học.",
  icons: {
    icon: [
      { url: "/icon.png", sizes: "32x32", type: "image/png" },
      { url: "/edufit-mark.png", sizes: "192x192", type: "image/png" },
    ],
    shortcut: "/favicon.ico",
    apple: [
      { url: "/apple-icon.png", sizes: "180x180", type: "image/png" },
    ],
  },
};

/**
 * RootLayout: Khung xương giao diện chung của toàn bộ ứng dụng EduFit
 * 
 * Tích hợp:
 * - Font typography: Bricolage Grotesque (Tiêu đề) và Inter (Nội dung) theo chuẩn thiết kế Figma.
 * - <AuthProvider>: Cung cấp trạng thái phiên đăng nhập (Stateful Session) xuyên suốt mọi trang.
 */
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="vi"
      data-scroll-behavior="smooth"
      className={`${bricolage.variable} ${inter.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col font-sans bg-stone-50 text-neutral-900">
        <AuthProvider>
          <ToastProvider>
            {children}
          </ToastProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
