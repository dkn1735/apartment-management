import type { Metadata } from "next";
import { Suspense } from "react";
import { ToaNhaView } from "./toa-nha-view";

export const metadata: Metadata = { title: "Tòa nhà" };

/** Trang Tòa nhà (spec 0006, AC-15). Quyền vào trang do `ChanTrang` của layout lo. */
export default function ToaNhaPage() {
  return (
    <Suspense>
      <ToaNhaView />
    </Suspense>
  );
}
