"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { parseTableParams, toSortParam, type TableSort } from "@/components/data/table-params";
import { useCapNhatUrl } from "@/components/data/use-cap-nhat-url";
import { EmptyState } from "@/components/feedback/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { duocThemToaNha, duocXemTang, TOA_NHA_QUERY_KEY, type ToaNha } from "@/lib/toa-nha";
import { ThemToaNhaDialog } from "./them-toa-nha-dialog";

export type { ToaNha } from "@/lib/toa-nha";

const SO = new Intl.NumberFormat("vi-VN");

const col = dataTableColumns<ToaNha>();

/** Cột của danh mục; `coLink` thì tên tòa dẫn tới trang chi tiết (spec 0011, AC-16). */
const taoCot = (coLink: boolean) =>
  col.columns([
    col.accessor("maToaNha", { header: "Mã tòa nhà", meta: { sortField: "maToaNha" } }),
    col.accessor("tenToaNha", {
      header: "Tên tòa nhà",
      meta: { sortField: "tenToaNha" },
      cell: (info) =>
        coLink ? <Link href={`/toa-nha/${info.row.original.maToaNha}`}>{info.getValue()}</Link> : info.getValue(),
    }),
    col.accessor("soTang", {
      header: "Số tầng",
      meta: { sortField: "soTang", align: "right" },
      cell: (info) => SO.format(info.getValue()),
    }),
    col.accessor("diaChiKhuVuc", { header: "Địa chỉ khu vực", meta: { description: true } }),
    col.accessor("soCanHo", {
      header: "Số căn hộ",
      meta: { align: "right" },
      cell: (info) => SO.format(info.getValue()),
    }),
  ]);

const COT_CO_LINK = taoCot(true);
const COT_KHONG_LINK = taoCot(false);

/**
 * Danh sách tòa nhà theo mẫu Bảng danh sách của `design.md`; `page`, `size`, `sort` nằm trên URL. BQL có nút "Thêm
 * tòa nhà" (spec 0011, AC-15); BQL và Kỹ thuật bấm tên tòa để xem tầng (AC-16).
 */
export function ToaNhaView() {
  const vaiTro = useHoSo().data?.vaiTro;
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);

  const query = useQuery<PageResponse<ToaNha>, ApiError>({
    queryKey: [...TOA_NHA_QUERY_KEY, "danh-muc", page, size, sort],
    queryFn: () =>
      api<PageResponse<ToaNha>>(
        // Không có sort trên URL thì không gửi, để backend dùng mặc định
        client.GET("/api/v1/buildings", { params: { query: { page, size, sort: toSortParam(sort) } } }),
      ),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <PageHeader
        title="Tòa nhà"
        description="Danh mục tòa nhà trong phạm vi bạn phụ trách."
        actions={duocThemToaNha(vaiTro) ? <ThemToaNhaDialog /> : undefined}
      />
      <DataTable
        caption="Danh sách tòa nhà"
        hideCaption
        columns={duocXemTang(vaiTro) ? COT_CO_LINK : COT_KHONG_LINK}
        query={query}
        page={page}
        size={size}
        sort={sort}
        onPageChange={(next) => capNhatUrl({ page: next === 1 ? undefined : String(next) })}
        onSizeChange={(next) => capNhatUrl({ size: String(next) })}
        onSortChange={(next?: TableSort) => capNhatUrl({ sort: toSortParam(next) })}
        emptyState={<EmptyState title="Chưa có tòa nhà nào" />}
        getRowId={(row) => row.maToaNha}
      />
    </>
  );
}
