"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { duongDoiMatKhau, TRANG_DOI_MAT_KHAU } from "@/lib/auth/bat-doi-mat-khau";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { nhomMenu } from "@/lib/phan-quyen/menu";
import { ChanTrang } from "./chan-trang";
import { MenuNguoiDung } from "./menu-nguoi-dung";

/**
 * Khung app phía client (spec 0006): menu theo vai trò từ query `["ho-so"]`, và chặn trang không đủ quyền.
 * Lỗi khi tải hồ sơ thì menu chỉ còn nhóm đầu (AC-13). Đang dùng mật khẩu tạm thì ẩn menu điều hướng (vẫn giữ menu
 * người dùng để đăng xuất) và chuyển mọi trang khác về `/doi-mat-khau` (spec 0008, AC-14).
 */
export function KhungApp({ children }: { children: ReactNode }) {
  const hoSo = useHoSo();
  const pathname = usePathname();
  const router = useRouter();
  const phaiDoi = hoSo.data?.phaiDoiMatKhau === true;
  const oTrangDoi = pathname === TRANG_DOI_MAT_KHAU;

  useEffect(() => {
    if (phaiDoi && !oTrangDoi) {
      // Đọc query trong effect, không dùng useSearchParams để layout không cần Suspense
      router.replace(duongDoiMatKhau(pathname + window.location.search));
    }
  }, [phaiDoi, oTrangDoi, pathname, router]);

  return (
    <AppShell
      nav={phaiDoi ? [] : nhomMenu(hoSo.data?.vaiTro ?? [])}
      navPending={hoSo.isPending}
      userSlot={<MenuNguoiDung />}
    >
      {phaiDoi && !oTrangDoi ? null : <ChanTrang hoSo={hoSo}>{children}</ChanTrang>}
    </AppShell>
  );
}
