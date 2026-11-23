import type { Metadata } from "next";
import { Suspense } from "react";
import { ChiTietToaNhaView } from "./chi-tiet-toa-nha-view";

export const metadata: Metadata = { title: "Chi tiết tòa nhà" };

/** Trang chi tiết tòa nhà và danh sách tầng (spec 0011, AC-16). Quyền vào trang theo mục `/toa-nha` của `ChanTrang`. */
export default function ChiTietToaNhaPage() {
  return (
    <Suspense>
      <ChiTietToaNhaView />
    </Suspense>
  );
}
