"use client";

import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { KeyValueList } from "@/components/data/key-value-list";
import { StatusBadge } from "@/components/data/status-badge";
import { ErrorState } from "@/components/feedback/error-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { PageHeader } from "@/components/layout/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { api, client, type ApiError } from "@/lib/api/api";
import { useHoSo } from "@/lib/auth/use-ho-so";
import {
  duocDoiTienIch,
  duocQuanLyCanHo,
  formatDienTich,
  khoaChiTietCanHo,
  toneTrangThai,
  type CanHo,
} from "@/lib/can-ho";
import { formatNgayGio, formatSo } from "@/lib/format";
import { hienKhoiHoSoCuTru } from "@/lib/cu-tru";
import { coTheDangKyPhuongTien, hienKhoiPhuongTien } from "@/lib/phuong-tien";
import { DoiTienIchDialog } from "./doi-tien-ich-dialog";
import { KhoiHoSoCuTru } from "./khoi-ho-so-cu-tru";
import { KhoiPhuongTien } from "./khoi-phuong-tien";
import { SuaCanHoDialog } from "./sua-can-ho-dialog";

export type DichQuayLai = { href: string; label: string };

type ChiTietCanHoProps = {
  maCanHo: string;
  /** Đích của link quay lại; bỏ trống khi trang này đã là trang gốc. */
  quayLai?: DichQuayLai;
  /** Trang nhân viên cho BQL sửa và đổi tiện ích; trang cư dân thì không. */
  choPhepSua?: boolean;
};

export function useChiTietCanHo(maCanHo: string) {
  return useQuery<CanHo, ApiError>({
    queryKey: khoaChiTietCanHo(maCanHo),
    queryFn: () =>
      api<CanHo>(client.GET("/api/v1/apartments/{apartmentId}", { params: { path: { apartmentId: maCanHo } } })),
  });
}

/**
 * Chi tiết một căn hộ (spec 0012, AC-19), dùng chung cho `/can-ho/<maCanHo>` (nhân viên) và
 * `/can-ho-cua-toi/<maCanHo>` (cư dân). 404 hiện "Không tìm thấy căn hộ" cùng link quay lại.
 */
export function ChiTietCanHo({ maCanHo, quayLai, choPhepSua = false }: ChiTietCanHoProps) {
  const ma = maCanHo.toUpperCase();
  const vaiTro = useHoSo().data?.vaiTro;
  const query = useChiTietCanHo(ma);

  const nutQuayLai = quayLai && (
    <Button asChild variant="outline">
      <Link href={quayLai.href}>
        <ArrowLeft aria-hidden="true" />
        {quayLai.label}
      </Link>
    </Button>
  );

  if (query.isPending) {
    return (
      <div aria-busy="true" className="flex flex-col gap-6">
        <span className="sr-only">Đang tải</span>
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }
  if (query.isError) {
    return query.error.errorCode === "NOT_FOUND" ? (
      <ErrorState
        headingLevel="h1"
        title="Không tìm thấy căn hộ"
        description="Căn hộ không tồn tại hoặc bạn không có quyền xem căn này."
        actions={nutQuayLai}
      />
    ) : (
      <QueryErrorState
        headingLevel="h1"
        title="Không tải được căn hộ"
        error={query.error}
        onRetry={() => query.refetch()}
        retrying={query.isFetching}
      />
    );
  }

  const canHo = query.data;
  const laBql = choPhepSua && duocQuanLyCanHo(vaiTro);
  const hienCuTru = hienKhoiHoSoCuTru(vaiTro, choPhepSua);
  const hienPhuongTien = hienKhoiPhuongTien(vaiTro, choPhepSua);
  const choPhepDangKyXe = coTheDangKyPhuongTien(vaiTro, choPhepSua);
  return (
    <>
      <PageHeader
        title={`Căn hộ ${canHo.maCanHo}`}
        description={`${canHo.tenToaNha}, tầng ${formatSo(canHo.soTang)}`}
        actions={
          <div className="flex flex-wrap items-start gap-3">
            {nutQuayLai}
            {choPhepSua && duocDoiTienIch(vaiTro) && <DoiTienIchDialog canHo={canHo} />}
            {laBql && <SuaCanHoDialog canHo={canHo} />}
          </div>
        }
      />
      <Card className="max-w-3xl">
        <CardContent>
          <KeyValueList variant="divided" items={thongTin(canHo)} />
        </CardContent>
      </Card>
      {hienCuTru && <KhoiHoSoCuTru maCanHo={canHo.maCanHo} laBql={laBql} />}
      {hienPhuongTien && (
        <KhoiPhuongTien
          maCanHo={canHo.maCanHo}
          laBql={laBql}
          laGocNhinNhanVien={choPhepSua}
          choPhepDangKy={choPhepDangKyXe}
        />
      )}
    </>
  );
}

/** Các dòng thông tin căn; có lần đổi tiện ích thì thêm lý do và thời điểm (spec 0013, AC-14). */
export function thongTin(canHo: CanHo) {
  const lanDoi = canHo.lanDoiTienIch;
  const nguoiDoi = lanDoi?.maNguoiDoi
    ? ` bởi ${lanDoi.hoTenNguoiDoi ? `${lanDoi.hoTenNguoiDoi} (${lanDoi.maNguoiDoi})` : lanDoi.maNguoiDoi}`
    : "";
  return [
    { label: "Mã căn hộ", value: canHo.maCanHo },
    { label: "Tòa nhà", value: `${canHo.tenToaNha} (${canHo.maToaNha})` },
    { label: "Tầng", value: `Tầng ${formatSo(canHo.soTang)} (${canHo.maTang})` },
    { label: "Diện tích", value: formatDienTich(canHo.dienTich) },
    {
      label: "Trạng thái sử dụng",
      value: <StatusBadge tone={toneTrangThai(canHo.trangThaiSuDung)}>{canHo.trangThaiSuDung}</StatusBadge>,
    },
    {
      label: "Trạng thái tiện ích",
      value: canHo.biTamDungTienIch ? (
        <StatusBadge tone="danger">Tạm dừng tiện ích</StatusBadge>
      ) : (
        "Đang dùng bình thường"
      ),
    },
    ...(lanDoi
      ? [
          {
            label: canHo.biTamDungTienIch ? "Lý do tạm dừng" : "Lý do mở khóa",
            value: <span className="whitespace-pre-line">{lanDoi.lyDo}</span>,
          },
          { label: "Cập nhật lúc", value: `${formatNgayGio(lanDoi.doiLuc)}${nguoiDoi}` },
        ]
      : []),
  ];
}
