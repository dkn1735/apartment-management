"use client";

import { useParams } from "next/navigation";
import { ChiTietCanHo } from "../chi-tiet-can-ho";

export function ChiTietCanHoNhanVien() {
  const { maCanHo } = useParams<{ maCanHo: string }>();
  return (
    <ChiTietCanHo
      maCanHo={decodeURIComponent(maCanHo)}
      quayLai={{ href: "/can-ho", label: "Danh sách căn hộ" }}
      choPhepSua
    />
  );
}
