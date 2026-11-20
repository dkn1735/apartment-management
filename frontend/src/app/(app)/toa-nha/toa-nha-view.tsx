"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useRef } from "react";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { parseTableParams, toSortParam, type TableSort } from "@/components/data/table-params";
import { EmptyState } from "@/components/feedback/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import type { components } from "@/lib/api/schema";

export type ToaNha = components["schemas"]["ToaNhaResponse"];

const SO = new Intl.NumberFormat("vi-VN");

const col = dataTableColumns<ToaNha>();
const columns = col.columns([
  col.accessor("maToaNha", { header: "Mã tòa nhà", meta: { sortField: "maToaNha" } }),
  col.accessor("tenToaNha", { header: "Tên tòa nhà", meta: { sortField: "tenToaNha" } }),
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

/** Danh sách tòa nhà theo mẫu Bảng danh sách của `design.md`; `page`, `size`, `sort` nằm trên URL. */
export function ToaNhaView() {
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);

  const query = useQuery<PageResponse<ToaNha>, ApiError>({
    queryKey: ["toa-nha", page, size, sort],
    queryFn: () =>
      api<PageResponse<ToaNha>>(
        // Không có sort trên URL thì không gửi, để backend dùng mặc định
        client.GET("/api/v1/buildings", { params: { query: { page, size, sort: toSortParam(sort) } } }),
      ),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <PageHeader title="Tòa nhà" description="Danh mục tòa nhà trong phạm vi bạn phụ trách." />
      <DataTable
        caption="Danh sách tòa nhà"
        hideCaption
        columns={columns}
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

/**
 * Ghi tham số lên URL. DataTable gọi hai lần liền nhau (đổi sort rồi về trang 1), nên gộp vào bản mới nhất
 * thay vì đọc lại `searchParams` cũ của lần render này.
 */
function useCapNhatUrl(searchParams: URLSearchParams) {
  const router = useRouter();
  const pathname = usePathname();
  const goc = searchParams.toString();
  const moiNhat = useRef<{ goc: string; params: URLSearchParams } | null>(null);
  return useCallback(
    (doi: Record<string, string | undefined>) => {
      // Các lần gọi trong cùng một lượt render có cùng `goc`, nên cộng dồn; URL đã đổi thì bắt đầu lại
      if (moiNhat.current?.goc !== goc) {
        moiNhat.current = { goc, params: new URLSearchParams(goc) };
      }
      const params = moiNhat.current.params;
      for (const [ten, giaTri] of Object.entries(doi)) {
        if (giaTri === undefined) params.delete(ten);
        else params.set(ten, giaTri);
      }
      const qs = params.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [router, pathname, goc],
  );
}
