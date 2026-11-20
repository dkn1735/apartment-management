import type { Metadata } from "next";
import { KhongTimThay } from "@/components/feedback/khong-tim-thay";

export const metadata: Metadata = { title: "Không tìm thấy trang" };

/** Trang 404 theo design system (AC-13). */
export default function NotFound() {
  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-10">
      <KhongTimThay />
    </main>
  );
}
