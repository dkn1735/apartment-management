"use client";

import { useParams } from "next/navigation";
import { ChiTietCanHo } from "../../can-ho/chi-tiet-can-ho";

export function ChiTietCanHoCuDan() {
  const { maCanHo } = useParams<{ maCanHo: string }>();
  return (
    <ChiTietCanHo
      maCanHo={decodeURIComponent(maCanHo)}
      quayLai={{ href: "/can-ho-cua-toi", label: "Căn hộ của tôi" }}
    />
  );
}
