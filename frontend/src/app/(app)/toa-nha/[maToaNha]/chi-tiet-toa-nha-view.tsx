"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { ArrowLeft, Building2, Layers, ShieldX } from "lucide-react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { KeyValueList } from "@/components/data/key-value-list";
import { StatTile } from "@/components/data/stat-tile";
import { parseTableParams, toSortParam, type TableSort } from "@/components/data/table-params";
import { useCapNhatUrl } from "@/components/data/use-cap-nhat-url";
import { EmptyState } from "@/components/feedback/empty-state";
import { ErrorState } from "@/components/feedback/error-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { PageHeader } from "@/components/layout/page-header";
import { SectionHeader } from "@/components/layout/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { formatSo } from "@/lib/format";
import { duocThemToaNha, khoaChiTietToaNha, TOA_NHA_QUERY_KEY, type Tang, type ToaNha } from "@/lib/toa-nha";
import { ThemTangButton } from "./them-tang-button";

const col = dataTableColumns<Tang>();
const columns = col.columns([
  col.accessor("soTang", {
    header: "Tầng",
    meta: { sortField: "soTang", align: "right" },
    cell: (info) => formatSo(info.getValue()),
  }),
  col.accessor("maTang", { header: "Mã tầng", meta: { sortField: "maTang" } }),
  col.accessor("soCanHo", {
    header: "Số căn hộ",
    meta: { align: "right" },
    cell: (info) => formatSo(info.getValue()),
  }),
]);

const quayLai = (
  <Button asChild variant="outline">
    <Link href="/toa-nha">
      <ArrowLeft aria-hidden="true" />
      Danh mục tòa nhà
    </Link>
  </Button>
);

/**
 * Chi tiết một tòa (spec 0011, AC-9, AC-16, AC-17): thông tin tòa, nút "Thêm tầng" cho BQL và bảng tầng có phân
 * trang; `page`, `size`, `sort` của bảng tầng nằm trên URL.
 */
export function ChiTietToaNhaView() {
  const { maToaNha } = useParams<{ maToaNha: string }>();
  const ma = decodeURIComponent(maToaNha).toUpperCase();
  const vaiTro = useHoSo().data?.vaiTro;
  const query = useQuery<ToaNha, ApiError>({
    queryKey: khoaChiTietToaNha(ma),
    queryFn: () => api<ToaNha>(client.GET("/api/v1/buildings/{buildingId}", { params: { path: { buildingId: ma } } })),
  });

  if (query.isPending) {
    return (
      <div aria-busy="true" className="flex flex-col gap-6">
        <span className="sr-only">Đang tải</span>
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }
  if (query.isError) {
    return query.error.errorCode === "NOT_FOUND" ? (
      <ErrorState
        headingLevel="h1"
        title="Không tìm thấy tòa nhà"
        description="Tòa nhà không tồn tại hoặc nằm ngoài phạm vi bạn phụ trách."
        actions={quayLai}
      />
    ) : (
      <QueryErrorState
        headingLevel="h1"
        title="Không tải được tòa nhà"
        error={query.error}
        onRetry={() => query.refetch()}
        retrying={query.isFetching}
      />
    );
  }

  const toa = query.data;
  return (
    <>
      <PageHeader
        title={toa.tenToaNha}
        description={`Mã tòa ${toa.maToaNha}`}
        actions={
          <div className="flex flex-wrap items-start gap-3">
            {quayLai}
            {duocThemToaNha(vaiTro) && <ThemTangButton toaNha={toa} />}
          </div>
        }
      />
      <div className="flex flex-col gap-10">
        <div className="grid gap-4 lg:grid-cols-[2fr_1fr_1fr]">
          <Card>
            <CardContent>
              <KeyValueList
                variant="divided"
                items={[
                  { label: "Mã tòa nhà", value: toa.maToaNha },
                  { label: "Tên tòa nhà", value: toa.tenToaNha },
                  { label: "Địa chỉ khu vực", value: toa.diaChiKhuVuc },
                ]}
              />
            </CardContent>
          </Card>
          <StatTile label="Số tầng" value={formatSo(toa.soTang)} icon={Layers} />
          <StatTile label="Số căn hộ" value={formatSo(toa.soCanHo)} icon={Building2} />
        </div>
        <BangTang ma={toa.maToaNha} />
      </div>
    </>
  );
}

/** Bảng tầng (AC-8, AC-16). Không có quyền xem tầng (403) thì hiện câu giải thích thay cho bảng. */
function BangTang({ ma }: { ma: string }) {
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);
  const query = useQuery<PageResponse<Tang>, ApiError>({
    queryKey: [...TOA_NHA_QUERY_KEY, "tang", ma, page, size, sort],
    queryFn: () =>
      api<PageResponse<Tang>>(
        client.GET("/api/v1/buildings/{buildingId}/floors", {
          params: { path: { buildingId: ma }, query: { page, size, sort: toSortParam(sort) } },
        }),
      ),
    placeholderData: keepPreviousData,
  });

  return (
    <section className="flex flex-col gap-4">
      <SectionHeader as="h2" title="Danh sách tầng" />
      {query.error?.errorCode === "FORBIDDEN" ? (
        <ErrorState headingLevel="h3" icon={ShieldX} title="Bạn không có quyền xem danh sách tầng" />
      ) : (
        <DataTable
          caption="Danh sách tầng"
          hideCaption
          columns={columns}
          query={query}
          page={page}
          size={size}
          sort={sort}
          onPageChange={(next) => capNhatUrl({ page: next === 1 ? undefined : String(next) })}
          onSizeChange={(next) => capNhatUrl({ size: String(next) })}
          onSortChange={(next?: TableSort) => capNhatUrl({ sort: toSortParam(next) })}
          emptyState={<EmptyState title="Tòa chưa có tầng nào" />}
          getRowId={(row) => row.maTang}
        />
      )}
    </section>
  );
}
