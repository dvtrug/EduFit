import type { Metadata } from "next";
import { Bricolage_Grotesque, Inter } from "next/font/google";
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
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={`${bricolage.variable} ${inter.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col font-sans bg-stone-50 text-neutral-900">
        {children}
      </body>
    </html>
  );
}
