import type { Metadata } from "next";
import { DoiMatKhauForm } from "./doi-mat-khau-form";

export const metadata: Metadata = { title: "Đổi mật khẩu" };

export default function DoiMatKhauPage() {
  return <DoiMatKhauForm />;
}
