import type { Metadata } from "next";
import { Suspense } from "react";
import { Brand } from "@/components/layout/brand";
import { Card } from "@/components/ui/card";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Đăng nhập" };

/** Trang đăng nhập, nằm ngoài khung app (spec 0004, mục Giao diện). Không gọi query nào. */
export default function DangNhapPage() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-8 px-4 py-10 sm:px-6">
      <Brand />
      <Card className="w-full max-w-[420px] gap-6 p-5 sm:p-8">
        <div className="flex flex-col gap-1">
          <h1 className="type-page-title">Đăng nhập</h1>
          <p className="type-ui text-muted-foreground">Dùng số điện thoại hoặc email đã đăng ký với ban quản lý.</p>
        </div>
        <Suspense>
          <LoginForm />
        </Suspense>
      </Card>
      <p className="max-w-[420px] text-center type-caption text-muted-foreground">
        Chưa có tài khoản hoặc quên mật khẩu? Liên hệ ban quản lý tòa nhà để được hỗ trợ.
      </p>
    </main>
  );
}
