import type { Metadata } from "next";
import { Suspense } from "react";
import { TaiKhoanView } from "./tai-khoan-view";

export const metadata: Metadata = { title: "Tài khoản" };

/** Trang Tài khoản (spec 0008, tính năng 11). Quyền vào trang do `ChanTrang` của layout lo. */
export default function TaiKhoanPage() {
  return (
    <Suspense>
      <TaiKhoanView />
    </Suspense>
  );
}
