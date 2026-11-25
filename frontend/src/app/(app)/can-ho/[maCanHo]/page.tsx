import type { Metadata } from "next";
import { ChiTietCanHoNhanVien } from "./chi-tiet-can-ho-nhan-vien";

export const metadata: Metadata = { title: "Chi tiết căn hộ" };

/** Chi tiết căn hộ cho nhân viên (spec 0012, AC-19). Quyền vào trang theo mục `/can-ho` của `ChanTrang`. */
export default function ChiTietCanHoPage() {
  return <ChiTietCanHoNhanVien />;
}
