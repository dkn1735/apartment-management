import type { Metadata } from "next";
import { Brand } from "@/components/layout/brand";
import { Card } from "@/components/ui/card";
import { QuenMatKhauForm } from "./quen-mat-khau-form";

export const metadata: Metadata = { title: "Quên mật khẩu" };

/** Trang quên mật khẩu, công khai, cùng bố cục với `/dang-nhap` (spec 0010, AC-13). */
export default function QuenMatKhauPage() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-8 px-4 py-10 sm:px-6">
      <Brand />
      <Card className="w-full max-w-[420px] gap-6 p-5 sm:p-8">
        <QuenMatKhauForm />
      </Card>
    </main>
  );
}
