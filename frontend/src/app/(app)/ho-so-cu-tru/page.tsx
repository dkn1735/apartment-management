import type { Metadata } from "next";
import { Suspense } from "react";
import { HoSoCuTruView } from "./ho-so-cu-tru-view";

export const metadata: Metadata = { title: "Hồ sơ cư trú" };

/** Trang Hồ sơ cư trú toàn tòa cho BQL (spec 0015, AC-16, AC-17, AC-18). Quyền vào trang do `ChanTrang` của layout lo. */
export default function HoSoCuTruPage() {
  return (
    <Suspense>
      <HoSoCuTruView />
    </Suspense>
  );
}
