import type { Metadata } from "next";
import { TongQuanView } from "./tong-quan-view";

export const metadata: Metadata = { title: "Tổng quan" };

export default function TongQuanPage() {
  return <TongQuanView />;
}
