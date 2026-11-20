import type { ReactNode } from "react";
import { KhungApp } from "./khung-app";

/** Khung app cho mọi trang sau đăng nhập (spec 0004, 0006). `proxy.ts` đã chặn khi chưa có phiên. */
export default function AppLayout({ children }: { children: ReactNode }) {
  return <KhungApp>{children}</KhungApp>;
}
