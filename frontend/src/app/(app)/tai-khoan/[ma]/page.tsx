import type { Metadata } from "next";
import { ChiTietTaiKhoanView } from "./chi-tiet-tai-khoan-view";

export const metadata: Metadata = { title: "Chi tiết tài khoản" };

/** Trang chi tiết tài khoản (spec 0008, AC-17). Quyền vào trang theo mục `/tai-khoan` của `ChanTrang`. */
export default function ChiTietTaiKhoanPage() {
  return <ChiTietTaiKhoanView />;
}
