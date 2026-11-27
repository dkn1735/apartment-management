"use client";

import { useTable, type RowData } from "@tanstack/react-table";
import type { UseQueryResult } from "@tanstack/react-query";
import { CircleAlert } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { cn } from "cn";
import { EmptyState } from "@/components/feedback/empty-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { RetryButton } from "@/components/feedback/retry-button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCaption, TableCell, TableHeader, TableRow } from "@/components/ui/table";
import type { ApiError } from "@/lib/api/api";
import { messageFor } from "@/lib/api/error-messages";
import type { PageResponse } from "@/lib/api/page";
import { dataTableFeatures, type DataTableColumn } from "./data-table-columns";
import { DataTablePagination } from "./data-table-pagination";
import { DataTableSortHeader } from "./data-table-sort-header";
import type { PageSize, TableSort } from "./table-params";

export type DataTableProps<T extends RowData> = {
  /** Tên bảng cho trình đọc màn hình; `hideCaption` để ẩn khỏi mắt. */
  caption: string;
  hideCaption?: boolean;
  /** Cột; giữ ổn định (khai báo ngoài component hoặc useMemo). */
  columns: DataTableColumn<T>[];
  /** Truy vấn trang hiện tại, gọi với `placeholderData: keepPreviousData`. */
  query: UseQueryResult<PageResponse<T>, ApiError>;
  page: number;
  size: PageSize;
  sort?: TableSort;
  onPageChange(page: number): void;
  onSizeChange(size: number): void;
  onSortChange(sort?: TableSort): void;
  emptyState?: ReactNode;
  getRowId?: (row: T) => string;
};

const SKELETON_ROWS = 5;

/** Bảng phân trang và sắp xếp phía server (spec 0003, Giao diện DataTable; AC-9, AC-10). */
export function DataTable<T extends RowData>(props: DataTableProps<T>) {
  const { caption, hideCaption, columns, query, page, size, sort, onPageChange, onSizeChange, onSortChange } = props;
  const data = query.data;

  const table = useTable({
    features: dataTableFeatures,
    columns,
    data: data?.items ?? EMPTY,
    getRowId: props.getRowId ? (row: T) => props.getRowId!(row) : undefined,
    manualSorting: true,
    manualPagination: true,
    rowCount: data?.totalItems ?? 0,
    state: { pagination: { pageIndex: page - 1, pageSize: size } },
  });

  // Trang hiện tại vượt quá tổng số trang (ví dụ sau khi xóa) thì về trang cuối
  useEffect(() => {
    if (data && data.totalPages > 0 && page > data.totalPages) {
      onPageChange(data.totalPages);
    }
  }, [data, page, onPageChange]);

  if (query.isError && !data) {
    return (
      <div className="rounded-md border bg-card">
        <QueryErrorState title="Không tải được dữ liệu" error={query.error} onRetry={query.refetch} />
      </div>
    );
  }

  const isInitialLoad = query.isPending;
  const isRefetching = query.isFetching && !isInitialLoad;
  const isEmpty = data?.totalItems === 0;
  const columnCount = columns.length;

  return (
    <div className="flex flex-col gap-3">
      {query.isError && data && (
        <Alert tone="danger" role="alert">
          <CircleAlert aria-hidden="true" />
          <AlertTitle>{messageFor(query.error)}</AlertTitle>
          {/* 403 thì thử lại cũng vô ích (spec 0006, AC-12) */}
          {query.error.errorCode !== "FORBIDDEN" && (
            <AlertDescription>
              <RetryButton onRetry={query.refetch} className="w-fit" />
            </AlertDescription>
          )}
        </Alert>
      )}
      {/* Bảng rộng hơn màn hình cuộn ngang trong khung này; khung nhận focus để cuộn bằng phím mũi tên (AC-10) */}
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex -- vùng cuộn phải nhận focus bằng bàn phím */}
      <div role="region" aria-label={caption} tabIndex={0} className="overflow-x-auto rounded-md border bg-card">
        <Table aria-busy={isInitialLoad || isRefetching || undefined}>
          <TableCaption className={cn(hideCaption && "sr-only")}>{caption}</TableCaption>
          <TableHeader>
            {table.getHeaderGroups().map((group) => (
              <TableRow key={group.id} className="border-b">
                {group.headers.map((header) => (
                  <DataTableSortHeader
                    key={header.id}
                    label={<table.FlexRender header={header} />}
                    sortField={header.column.columnDef.meta?.sortField}
                    sortOptions={header.column.columnDef.meta?.sortOptions}
                    align={header.column.columnDef.meta?.align}
                    sort={sort}
                    onSortChange={(next) => {
                      onSortChange(next);
                      onPageChange(1);
                    }}
                  />
                ))}
              </TableRow>
            ))}
            {isRefetching && (
              <tr aria-hidden="true">
                <td colSpan={columnCount} className="h-0.5 p-0">
                  <div className="h-0.5 animate-pulse bg-primary" />
                </td>
              </tr>
            )}
          </TableHeader>
          <TableBody className={cn(isRefetching && "opacity-60")}>
            {isInitialLoad && <SkeletonRows columnCount={columnCount} />}
            {isEmpty && (
              <TableRow>
                <TableCell colSpan={columnCount} className="p-0">
                  {props.emptyState ?? <EmptyState />}
                </TableCell>
              </TableRow>
            )}
            {table.getRowModel().rows.map((row) => (
              <TableRow key={row.id} className="hover:bg-accent">
                {row.getAllCells().map((cell) => {
                  const meta = cell.column.columnDef.meta;
                  return (
                    <TableCell key={cell.id} className={cn(meta?.align === "right" && "text-right whitespace-nowrap")}>
                      <div className={cn(meta?.description && "line-clamp-2 min-w-48")}>
                        <table.FlexRender cell={cell} />
                      </div>
                    </TableCell>
                  );
                })}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      {data && !isEmpty && (
        <DataTablePagination
          page={page}
          size={size}
          totalItems={data.totalItems}
          totalPages={data.totalPages}
          onPageChange={onPageChange}
          onSizeChange={(next) => {
            onSizeChange(next);
            onPageChange(1);
          }}
        />
      )}
    </div>
  );
}

const EMPTY: never[] = [];

function SkeletonRows({ columnCount }: { columnCount: number }) {
  return Array.from({ length: SKELETON_ROWS }, (_, index) => (
    <TableRow key={index}>
      {Array.from({ length: columnCount }, (_, cell) => (
        <TableCell key={cell}>
          {index === 0 && cell === 0 && <span className="sr-only">Đang tải</span>}
          <Skeleton className="h-4 w-3/4" />
        </TableCell>
      ))}
    </TableRow>
  ));
}
