import type { Metadata } from "next";
import { Suspense } from "react";
import { DoiMatKhauForm } from "./doi-mat-khau-form";

export const metadata: Metadata = { title: "Đổi mật khẩu" };

/** Đọc `next` từ URL (spec 0008, AC-14) nên form nằm trong Suspense. */
export default function DoiMatKhauPage() {
  return (
    <Suspense>
      <DoiMatKhauForm />
    </Suspense>
  );
}
