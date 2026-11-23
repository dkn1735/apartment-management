import type { Metadata } from "next";
import { Suspense } from "react";
import { KhuVucChungView } from "./khu-vuc-chung-view";

export const metadata: Metadata = { title: "Khu vực chung" };

/** Trang danh mục khu vực dùng chung (spec 0011, AC-18). Quyền vào trang do `ChanTrang` của layout lo. */
export default function KhuVucChungPage() {
  return (
    <Suspense>
      <KhuVucChungView />
    </Suspense>
  );
}
