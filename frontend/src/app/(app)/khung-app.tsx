"use client";

import type { ReactNode } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { nhomMenu } from "@/lib/phan-quyen/menu";
import { ChanTrang } from "./chan-trang";
import { MenuNguoiDung } from "./menu-nguoi-dung";

/**
 * Khung app phía client (spec 0006): menu theo vai trò từ query `["ho-so"]`, và chặn trang không đủ quyền.
 * Lỗi khi tải hồ sơ thì menu chỉ còn nhóm đầu (AC-13).
 */
export function KhungApp({ children }: { children: ReactNode }) {
  const hoSo = useHoSo();
  return (
    <AppShell nav={nhomMenu(hoSo.data?.vaiTro ?? [])} navPending={hoSo.isPending} userSlot={<MenuNguoiDung />}>
      <ChanTrang hoSo={hoSo}>{children}</ChanTrang>
    </AppShell>
  );
}
