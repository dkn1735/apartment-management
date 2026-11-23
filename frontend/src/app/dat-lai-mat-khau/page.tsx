import type { Metadata } from "next";
import { Brand } from "@/components/layout/brand";
import { Card } from "@/components/ui/card";
import { DatLaiMatKhauForm } from "./dat-lai-mat-khau-form";

export const metadata: Metadata = { title: "Đặt lại mật khẩu" };

/**
 * Trang mở từ link trong email (spec 0010, AC-13). Công khai, cùng bố cục với `/dang-nhap`. Token nằm trong fragment
 * nên không bao giờ tới máy chủ; `next.config.ts` đặt `Referrer-Policy: no-referrer` cho trang này (AC-15).
 */
export default function DatLaiMatKhauPage() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-8 px-4 py-10 sm:px-6">
      <Brand />
      <Card className="w-full max-w-[420px] gap-6 p-5 sm:p-8">
        <DatLaiMatKhauForm />
      </Card>
    </main>
  );
}
