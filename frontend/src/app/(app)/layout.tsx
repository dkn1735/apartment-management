import type { ReactNode } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { APP_NAV } from "./app-nav";
import { MenuNguoiDung } from "./menu-nguoi-dung";

/** Khung app cho mọi trang sau đăng nhập (spec 0004, mục Giao diện). `proxy.ts` đã chặn khi chưa có phiên. */
export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <AppShell nav={APP_NAV} userSlot={<MenuNguoiDung />}>
      {children}
    </AppShell>
  );
}
