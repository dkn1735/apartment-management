import type { Metadata } from "next";
import { Suspense } from "react";
import { CanHoView } from "./can-ho-view";

export const metadata: Metadata = { title: "Căn hộ" };

/** Trang Căn hộ (spec 0012, AC-17). Quyền vào trang do `ChanTrang` của layout lo. */
export default function CanHoPage() {
  return (
    <Suspense>
      <CanHoView />
    </Suspense>
  );
}
