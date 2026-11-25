import type { Metadata } from "next";
import { ChiTietCanHoCuDan } from "./chi-tiet-can-ho-cu-dan";

export const metadata: Metadata = { title: "Chi tiết căn hộ" };

/** Chi tiết một căn của cư dân (spec 0012, AC-19). Quyền vào trang theo mục `/can-ho-cua-toi` của `ChanTrang`. */
export default function ChiTietCanHoCuDanPage() {
  return <ChiTietCanHoCuDan />;
}
