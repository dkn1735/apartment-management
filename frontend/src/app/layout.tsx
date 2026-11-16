import type { Metadata } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import { APP_NAME } from "@/lib/brand";
import "./globals.css";
import { Providers } from "./providers";

// Be Vietnam Pro có đủ dấu tiếng Việt; gắn vào --font-sans để Tailwind dùng làm font mặc định
const beVietnamPro = Be_Vietnam_Pro({
  subsets: ["vietnamese", "latin"],
  weight: ["200", "400", "500", "600"],
  display: "swap",
  variable: "--font-sans",
});

export const metadata: Metadata = {
  title: APP_NAME,
  description: "Hệ thống quản lý vận hành chung cư",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="vi" className={beVietnamPro.variable}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
