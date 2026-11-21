"use client";

import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { StatusBadge } from "@/components/data/status-badge";
import { ErrorState } from "@/components/feedback/error-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { PageHeader } from "@/components/layout/page-header";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { api, client, type ApiError } from "@/lib/api/api";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { khoaChiTiet, TONE_TRANG_THAI, type TaiKhoanChiTiet } from "@/lib/tai-khoan";
import { TheThongTinChung } from "./the-thong-tin-chung";
import { TheTrangThai } from "./the-trang-thai";
import { TheVaiTro } from "./the-vai-tro";

/** Trang chi tiết một tài khoản (spec 0008, AC-6, AC-17): thẻ Thông tin chung, Vai trò, Trạng thái và bảo mật. */
export function ChiTietTaiKhoanView() {
  const { ma } = useParams<{ ma: string }>();
  const hoSo = useHoSo().data;
  const laAdmin = hoSo?.vaiTro.includes("QUAN_TRI_VIEN") ?? false;
  const query = useQuery<TaiKhoanChiTiet, ApiError>({
    queryKey: khoaChiTiet(ma),
    queryFn: () =>
      api<TaiKhoanChiTiet>(client.GET("/api/v1/accounts/{maNguoiDung}", { params: { path: { maNguoiDung: ma } } })),
  });

  const quayLai = (
    <Button asChild variant="outline">
      <Link href="/tai-khoan">
        <ArrowLeft aria-hidden="true" />
        Danh sách tài khoản
      </Link>
    </Button>
  );

  if (query.isPending) {
    return (
      <div aria-busy="true" className="flex flex-col gap-6">
        <span className="sr-only">Đang tải</span>
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-56 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }
  if (query.isError) {
    return query.error.errorCode === "NOT_FOUND" ? (
      <ErrorState
        headingLevel="h1"
        title="Không tìm thấy tài khoản"
        description="Tài khoản không tồn tại hoặc nằm ngoài phạm vi bạn quản lý."
        actions={quayLai}
      />
    ) : (
      <QueryErrorState
        headingLevel="h1"
        title="Không tải được tài khoản"
        error={query.error}
        onRetry={() => query.refetch()}
        retrying={query.isFetching}
      />
    );
  }

  const tk = query.data;
  return (
    <>
      <PageHeader
        title={tk.hoTen}
        description={
          <span className="flex flex-wrap items-center gap-2">
            <StatusBadge tone={TONE_TRANG_THAI[tk.trangThaiTaiKhoan]}>{tk.trangThaiTaiKhoan}</StatusBadge>
            {tk.phaiDoiMatKhau && <StatusBadge tone="info">Chờ đổi mật khẩu</StatusBadge>}
          </span>
        }
        actions={quayLai}
      />
      <div className="grid gap-6 lg:grid-cols-2">
        <TheThongTinChung key={`ttc-${tk.phienBan}`} taiKhoan={tk} />
        <TheVaiTro taiKhoan={tk} laAdmin={laAdmin} laChinhMinh={hoSo?.maNguoiDung === tk.maNguoiDung} />
        <TheTrangThai taiKhoan={tk} laAdmin={laAdmin} laChinhMinh={hoSo?.maNguoiDung === tk.maNguoiDung} />
      </div>
    </>
  );
}
