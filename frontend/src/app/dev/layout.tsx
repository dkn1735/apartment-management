import { notFound } from "next/navigation";

/** Mọi trang dưới /dev/ chỉ có khi chạy `pnpm dev`; bản production trả 404 (AC-14). */
export default function DevLayout({ children }: LayoutProps<"/dev">) {
  if (process.env.NODE_ENV === "production") {
    notFound();
  }
  return children;
}
