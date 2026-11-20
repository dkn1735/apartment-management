import type { Metadata } from "next";
import { KhongTimThay } from "@/components/feedback/khong-tim-thay";

export const metadata: Metadata = { title: "Không tìm thấy trang" };

/** 404 trong khung app: `AppShell` đã có `<main>`, nên không bọc thêm như `app/not-found.tsx`. */
export default function AppNotFound() {
  return <KhongTimThay />;
}
