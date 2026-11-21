import type { Metadata } from "next";
import { TaoTaiKhoanForm } from "./tao-tai-khoan-form";

export const metadata: Metadata = { title: "Tạo tài khoản" };

/** Trang tạo tài khoản (spec 0008, AC-17). Quyền vào trang theo mục `/tai-khoan` của `ChanTrang`. */
export default function TaoTaiKhoanPage() {
  return <TaoTaiKhoanForm />;
}
