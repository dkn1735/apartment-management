"use client";

import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { ErrorState } from "@/components/feedback/error-state";
import { KhongCoQuyen } from "@/components/feedback/khong-co-quyen";
import { RetryButton } from "@/components/feedback/retry-button";
import { Skeleton } from "@/components/ui/skeleton";
import { laLoiChuyenVeDangNhap, type useHoSo } from "@/lib/auth/use-ho-so";
import { duocVao, mucCuaDuongDan } from "@/lib/phan-quyen/menu";

type ChanTrangProps = { hoSo: ReturnType<typeof useHoSo>; children: ReactNode };

/**
 * Chặn trang theo ma trận chức năng (spec 0006, AC-11, AC-13). Trang thuộc ma trận chỉ render khi đã biết vai trò
 * và vai trò giao với vai trò của mục; đường dẫn ngoài ma trận (`/`, `/ho-so`) luôn render.
 */
export function ChanTrang({ hoSo, children }: ChanTrangProps) {
  const muc = mucCuaDuongDan(usePathname());
  if (!muc) {
    return children;
  }
  if (hoSo.data) {
    return duocVao(muc, hoSo.data.vaiTro) ? children : <KhongCoQuyen />;
  }
  if (hoSo.isError && !laLoiChuyenVeDangNhap(hoSo.error)) {
    return (
      <ErrorState
        headingLevel="h1"
        title="Không tải được quyền truy cập"
        actions={<RetryButton onRetry={hoSo.refetch} loading={hoSo.isRefetching} />}
      />
    );
  }
  return (
    <div role="status" className="flex flex-col gap-4">
      <span className="sr-only">Đang tải</span>
      <Skeleton className="h-9 w-64" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}
