"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CircleAlert } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { parseTableParams, toSortParam, type TableSort } from "@/components/data/table-params";
import { useCapNhatUrl } from "@/components/data/use-cap-nhat-url";
import { EmptyState } from "@/components/feedback/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import type { components } from "@/lib/api/schema";
import { EMPTY_VALUE, formatNgayGio } from "@/lib/format";
import { BoLocNhatKy, docBoLoc, THAM_SO_LOC, type BoLoc } from "./bo-loc-nhat-ky";

export type NhatKy = components["schemas"]["NhatKyResponse"];
type LoaiThaoTac = components["schemas"]["LoaiThaoTacResponse"];

const col = dataTableColumns<NhatKy>();
const columns = col.columns([
  col.accessor("thoiGianGhi", {
    header: "Thời gian",
    meta: { sortField: "thoiGianGhi" },
    cell: (info) => <span className="whitespace-nowrap tabular-nums">{formatNgayGio(info.getValue())}</span>,
  }),
  col.accessor("hoTen", {
    header: "Người thực hiện",
    cell: (info) => <NguoiThucHien hoTen={info.getValue()} ma={info.row.original.maNguoiDung} />,
  }),
  col.accessor("tenThaoTac", { header: "Thao tác", meta: { sortField: "loaiThaoTac" } }),
  col.accessor("doiTuongTacDong", {
    header: "Đối tượng",
    cell: (info) => <span className="break-all">{info.getValue()}</span>,
  }),
  col.accessor("chiTietThayDoi", {
    header: "Chi tiết",
    meta: { description: true },
    cell: (info) => tomTatChiTiet(info.getValue()),
  }),
  col.accessor("diaChiIp", {
    header: "Địa chỉ IP",
    cell: (info) => <span className="tabular-nums">{info.getValue() ?? EMPTY_VALUE}</span>,
  }),
  col.accessor("requestId", {
    header: "Mã yêu cầu",
    cell: (info) => <span className="font-mono text-xs break-all">{info.getValue() ?? EMPTY_VALUE}</span>,
  }),
]);

/**
 * Tra cứu nhật ký kiểm toán (spec 0007, tính năng 9) theo mẫu Bảng danh sách của `design.md`. Phân trang, sắp
 * xếp và bộ lọc đều nằm trên URL.
 */
export function NhatKyView() {
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const boLoc = docBoLoc(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);

  const loaiQuery = useQuery<LoaiThaoTac[], ApiError>({
    queryKey: ["nhat-ky", "loai-thao-tac"],
    queryFn: () => api<LoaiThaoTac[]>(client.GET("/api/v1/system/audit-logs/action-types")),
    staleTime: Infinity,
  });

  const query = useQuery<PageResponse<NhatKy>, ApiError>({
    queryKey: ["nhat-ky", page, size, sort, boLoc],
    queryFn: () =>
      api<PageResponse<NhatKy>>(
        client.GET("/api/v1/system/audit-logs", {
          params: { query: { page, size, sort: toSortParam(sort), ...boLoc } },
        }),
      ),
    placeholderData: keepPreviousData,
  });

  function apDung(moi: BoLoc) {
    capNhatUrl(
      { ...Object.fromEntries(THAM_SO_LOC.map((ten) => [ten, moi[ten]])), page: undefined },
      { themLichSu: true },
    );
  }

  const dangLoc = THAM_SO_LOC.some((ten) => boLoc[ten]);
  // Bộ lọc sửa tay trên URL bị backend từ chối: nêu lỗi từng tham số, "Thử lại" không giúp được gì
  const loiBoLoc = query.error?.errorCode === "VALIDATION_FAILED" ? query.error : null;

  return (
    <>
      <PageHeader title="Nhật ký hệ thống" description="Lịch sử đăng nhập, đăng xuất và các thao tác cần kiểm toán." />
      <div className="flex flex-col gap-4">
        <BoLocNhatKy
          key={THAM_SO_LOC.map((ten) => boLoc[ten] ?? "").join("|")}
          boLoc={boLoc}
          loaiThaoTac={loaiQuery.data ?? []}
          onApDung={apDung}
        />
        {loiBoLoc ? (
          <Alert tone="danger">
            <CircleAlert aria-hidden />
            <AlertTitle>Bộ lọc trên địa chỉ trang không hợp lệ</AlertTitle>
            <AlertDescription>
              <ul className="list-disc pl-5">
                {loiBoLoc.errors.map((loi) => (
                  <li key={`${loi.field}-${loi.message}`}>{loi.message}</li>
                ))}
              </ul>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-3"
                onClick={() => capNhatUrl(xoaHet(), { themLichSu: true })}
              >
                Xóa bộ lọc
              </Button>
            </AlertDescription>
          </Alert>
        ) : (
          <DataTable
            caption="Danh sách nhật ký hệ thống"
            hideCaption
            columns={columns}
            query={query}
            page={page}
            size={size}
            sort={sort}
            onPageChange={(next) => capNhatUrl({ page: next === 1 ? undefined : String(next) })}
            onSizeChange={(next) => capNhatUrl({ size: String(next) })}
            onSortChange={(next?: TableSort) => capNhatUrl({ sort: toSortParam(next) })}
            emptyState={
              dangLoc ? (
                <EmptyState
                  title="Không có dòng nhật ký nào khớp bộ lọc"
                  description="Thử nới khoảng ngày hoặc bỏ bớt điều kiện lọc."
                />
              ) : (
                <EmptyState title="Chưa có dòng nhật ký nào" />
              )
            }
            getRowId={(row) => row.maLog}
          />
        )}
      </div>
    </>
  );
}

function xoaHet(): Record<string, undefined> {
  return { ...Object.fromEntries(THAM_SO_LOC.map((ten) => [ten, undefined])), page: undefined, sort: undefined };
}

function NguoiThucHien({ hoTen, ma }: { hoTen?: string | null; ma?: string | null }) {
  if (!ma) return <span className="text-muted-foreground">Không xác định</span>;
  return (
    <span className="flex flex-col">
      <span>{hoTen ?? EMPTY_VALUE}</span>
      <span className="text-xs text-muted-foreground tabular-nums">{ma}</span>
    </span>
  );
}

/** `{"lyDo": "SAI_THONG_TIN"}` thành `lyDo: SAI_THONG_TIN`; giá trị lồng nhau giữ dạng JSON. */
export function tomTatChiTiet(chiTiet: Record<string, unknown> | null | undefined): string {
  if (!chiTiet) return EMPTY_VALUE;
  const cap = Object.entries(chiTiet);
  if (cap.length === 0) return EMPTY_VALUE;
  return cap
    .map(
      ([khoa, giaTri]) =>
        `${khoa}: ${typeof giaTri === "object" && giaTri !== null ? JSON.stringify(giaTri) : String(giaTri)}`,
    )
    .join(", ");
}
