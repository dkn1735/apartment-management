import type { Metadata } from "next";
import { Suspense } from "react";
import { NhatKyView } from "./nhat-ky-view";

export const metadata: Metadata = { title: "Nhật ký hệ thống" };

/** Trang Nhật ký hệ thống (spec 0007, tính năng 9). Quyền vào trang do `ChanTrang` của layout lo. */
export default function NhatKyPage() {
  return (
    <Suspense>
      <NhatKyView />
    </Suspense>
  );
}
