import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { KhongTimThay } from "@/components/feedback/khong-tim-thay";
import { mucCuaDuongDan } from "@/lib/phan-quyen/menu";

export const metadata: Metadata = { title: "Không tìm thấy trang" };

/**
 * Đường dẫn chưa có trang (spec 0006, AC-11). Không có route này thì Next.js trả `app/not-found.tsx` ngoài layout
 * `(app)`, nên `ChanTrang` không chạy và `/doi-soat`, `/toa-nha/abc` hiện 404 thay vì "Không có quyền truy cập".
 * Đường dẫn thuộc ma trận render trong khung app để `ChanTrang` quyết định; không gọi `notFound()` ở đây vì
 * component server luôn render, kể cả khi `ChanTrang` chặn. Đường dẫn ngoài ma trận vẫn là 404 thật.
 */
export default async function DuongDanChuaCoTrang({ params }: PageProps<"/[...duongDan]">) {
  const { duongDan } = await params;
  if (!mucCuaDuongDan(`/${duongDan.join("/")}`)) {
    notFound();
  }
  return <KhongTimThay />;
}
