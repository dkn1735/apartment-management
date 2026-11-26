import type { Metadata } from "next";
import { CanHoCuaToiView } from "./can-ho-cua-toi-view";

export const metadata: Metadata = { title: "Căn hộ của tôi" };

/** Căn hộ cư dân đang cư trú (spec 0012, AC-20). Quyền vào trang do `ChanTrang` của layout lo. */
export default function CanHoCuaToiPage() {
  return <CanHoCuaToiView />;
}
