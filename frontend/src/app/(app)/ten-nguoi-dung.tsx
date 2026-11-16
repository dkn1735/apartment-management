"use client";

import { UserRound } from "lucide-react";
import { useHoSo } from "@/lib/auth/use-ho-so";

/** Họ tên ở góc phải thanh trên; không hiện gì khi đang tải hoặc lỗi. */
export function TenNguoiDung() {
  const { data } = useHoSo();
  if (!data) {
    return null;
  }
  return (
    <span className="flex min-w-0 items-center gap-2 type-ui font-medium">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-soft">
        <UserRound aria-hidden="true" className="size-4 text-primary" />
      </span>
      <span className="truncate">{data.hoTen}</span>
    </span>
  );
}
