"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CircleAlert, MapPinned } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { ChonLoc, OTimKiem } from "@/components/data/bo-loc";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { parseTableParams, toSortParam, type TableSort } from "@/components/data/table-params";
import { useCapNhatUrl } from "@/components/data/use-cap-nhat-url";
import { EmptyState } from "@/components/feedback/empty-state";
import { ErrorState } from "@/components/feedback/error-state";
import { PageHeader } from "@/components/layout/page-header";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import type { KhuVucDungChung, ToaNha } from "@/lib/toa-nha";

const col = dataTableColumns<KhuVucDungChung>();
const columns = col.columns([
  col.accessor("maKhuVuc", { header: "Mã khu vực", meta: { sortField: "maKhuVuc" } }),
  col.accessor("tenKhuVuc", { header: "Tên khu vực", meta: { sortField: "tenKhuVuc" } }),
  col.accessor("viTri", { header: "Vị trí" }),
  col.accessor("tenToaNha", { header: "Tòa nhà", meta: { sortField: "tenToaNha" } }),
]);

/** Bộ lọc nằm trên URL; thiếu là không lọc. */
type BoLoc = { maToaNha?: string; q?: string };

function docBoLoc(searchParams: URLSearchParams): BoLoc {
  return { maToaNha: searchParams.get("maToaNha") || undefined, q: searchParams.get("q") || undefined };
}

/**
 * Danh mục khu vực dùng chung (spec 0011, AC-11, AC-18) theo mẫu Bảng danh sách của `design.md`. Bộ lọc tòa và ô
 * tìm theo tên nằm trên URL; đổi bộ lọc thì về trang 1; ô tìm gửi sau 300 ms ngừng gõ.
 */
export function KhuVucChungView() {
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const boLoc = docBoLoc(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);

  const toaNha = useQuery<PageResponse<ToaNha>, ApiError>({
    queryKey: ["toa-nha", "lua-chon"],
    queryFn: () =>
      api<PageResponse<ToaNha>>(
        client.GET("/api/v1/buildings", { params: { query: { size: 100, sort: "tenToaNha,asc" } } }),
      ),
  });
  const query = useQuery<PageResponse<KhuVucDungChung>, ApiError>({
    queryKey: ["khu-vuc-chung", page, size, sort, boLoc],
    queryFn: () =>
      api<PageResponse<KhuVucDungChung>>(
        client.GET("/api/v1/common-areas", {
          params: { query: { page, size, sort: toSortParam(sort), ...boLoc } },
        }),
      ),
    placeholderData: keepPreviousData,
  });

  function loc(ten: keyof BoLoc, giaTri: string | undefined) {
    capNhatUrl({ [ten]: giaTri || undefined, page: undefined }, { themLichSu: true });
  }
  const xoaBoLoc = () => capNhatUrl({ maToaNha: undefined, q: undefined, page: undefined }, { themLichSu: true });

  const dangLoc = Boolean(boLoc.maToaNha || boLoc.q);
  const loi = query.error;

  // Tòa trên URL không có trong danh sách thì vẫn hiện mã, để người dùng thấy đang lọc theo gì
  const luaChonToa = (toaNha.data?.items ?? []).map((t) => ({ value: t.maToaNha, label: t.tenToaNha }));
  if (boLoc.maToaNha && !luaChonToa.some((lc) => lc.value === boLoc.maToaNha)) {
    luaChonToa.push({ value: boLoc.maToaNha, label: boLoc.maToaNha });
  }

  let noiDung;
  if (loi?.errorCode === "NOT_FOUND") {
    noiDung = (
      <ErrorState
        headingLevel="h2"
        title="Không tìm thấy tòa nhà"
        description="Tòa nhà trong bộ lọc không tồn tại hoặc nằm ngoài phạm vi bạn phụ trách."
        actions={
          <Button type="button" variant="outline" onClick={() => loc("maToaNha", undefined)}>
            Xóa bộ lọc
          </Button>
        }
      />
    );
  } else if (loi?.errorCode === "VALIDATION_FAILED") {
    noiDung = (
      <Alert tone="danger">
        <CircleAlert aria-hidden />
        <AlertTitle>Bộ lọc trên địa chỉ trang không hợp lệ</AlertTitle>
        <AlertDescription>
          <ul className="list-disc pl-5">
            {loi.errors.map((e) => (
              <li key={`${e.field}-${e.message}`}>{e.message}</li>
            ))}
          </ul>
        </AlertDescription>
      </Alert>
    );
  } else {
    noiDung = (
      <DataTable
        caption="Danh sách khu vực dùng chung"
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
              title="Không có khu vực nào khớp bộ lọc"
              description="Thử từ khóa khác hoặc chọn tất cả tòa nhà. Tìm theo tên có dấu, ví dụ “Sảnh”."
            />
          ) : (
            <EmptyState icon={MapPinned} title="Chưa có khu vực dùng chung nào" />
          )
        }
        getRowId={(row) => row.maKhuVuc}
      />
    );
  }

  return (
    <>
      <PageHeader
        title="Khu vực chung"
        description="Sảnh, hành lang, phòng kỹ thuật của từng tòa trong phạm vi bạn phụ trách."
      />
      <div className="flex flex-col gap-4">
        <div
          role="search"
          aria-label="Lọc khu vực dùng chung"
          className="grid gap-4 rounded-md border bg-card p-4 sm:grid-cols-2 lg:grid-cols-3"
        >
          <ChonLoc
            nhan="Tòa nhà"
            giaTri={boLoc.maToaNha}
            tatCa="Tất cả tòa nhà"
            luaChon={luaChonToa}
            onChon={(v) => loc("maToaNha", v)}
          />
          <OTimKiem
            nhan="Tìm theo tên"
            goiY="Gõ có dấu, ví dụ “Sảnh” hoặc “hành lang”"
            giaTri={boLoc.q ?? ""}
            onTim={(q) => loc("q", q.trim())}
          />
          {dangLoc && (
            <div className="flex items-end">
              <Button type="button" variant="outline" onClick={xoaBoLoc}>
                Xóa bộ lọc
              </Button>
            </div>
          )}
        </div>
        {noiDung}
      </div>
    </>
  );
}
