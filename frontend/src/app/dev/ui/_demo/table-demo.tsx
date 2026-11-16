"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { DataTable } from "@/components/data/data-table";
import { StatusBadge, type StatusTone } from "@/components/data/status-badge";
import type { PageSize, TableSort } from "@/components/data/table-params";
import { Button } from "@/components/ui/button";
import type { ApiError } from "@/lib/api/api";
import { formatNgay, formatTien } from "@/lib/format";
import { fetchCanHoMau, type CanHoMau, type DemoTableMode } from "./demo-apartments";
import { DemoGroup } from "./demo-section";

const TRANG_THAI: Record<CanHoMau["trangThai"], { label: string; tone: StatusTone }> = {
  "dang-o": { label: "Đang ở", tone: "success" },
  trong: { label: "Còn trống", tone: "neutral" },
  "sua-chua": { label: "Đang sửa chữa", tone: "warning" },
};

const col = dataTableColumns<CanHoMau>();
const columns = col.columns([
  col.accessor("maCanHo", { header: "Mã căn hộ", meta: { sortField: "maCanHo" } }),
  col.accessor("chuHo", { header: "Chủ hộ", meta: { sortField: "chuHo" } }),
  col.accessor("dienTich", {
    header: "Diện tích (m²)",
    meta: { sortField: "dienTich", align: "right" },
    cell: (info) => info.getValue().toLocaleString("vi-VN"),
  }),
  col.accessor("phiThang", {
    header: "Phí tháng",
    meta: { align: "right" },
    cell: (info) => formatTien(info.getValue()),
  }),
  col.accessor("trangThai", {
    header: "Trạng thái",
    cell: (info) => {
      const status = TRANG_THAI[info.getValue()];
      return <StatusBadge tone={status.tone}>{status.label}</StatusBadge>;
    },
  }),
  col.accessor("ngayBanGiao", {
    header: "Ngày bàn giao",
    meta: { sortField: "ngayBanGiao" },
    cell: (info) => formatNgay(info.getValue()),
  }),
  col.accessor("ghiChu", { header: "Ghi chú", meta: { description: true }, cell: (info) => info.getValue() || "—" }),
]);

const MODES: { value: DemoTableMode; label: string }[] = [
  { value: "data", label: "Có dữ liệu" },
  { value: "empty", label: "Rỗng" },
  { value: "error", label: "Lỗi" },
];

/** DataTable trên dữ liệu giả có độ trễ 600ms, chuyển được sang rỗng và lỗi. */
export function TableDemo() {
  const [page, setPage] = useState(1);
  const [size, setSize] = useState<PageSize>(10);
  const [sort, setSort] = useState<TableSort>();
  const [mode, setMode] = useState<DemoTableMode>("data");

  const query = useQuery<Awaited<ReturnType<typeof fetchCanHoMau>>, ApiError>({
    queryKey: ["dev-can-ho", page, size, sort, mode],
    queryFn: () => fetchCanHoMau({ page, size, sort }, mode),
    placeholderData: keepPreviousData,
  });

  return (
    <DemoGroup label="DataTable">
      <div role="group" aria-label="Dữ liệu giả" className="mb-4 flex flex-wrap gap-2">
        {MODES.map((item) => (
          <Button
            key={item.value}
            size="sm"
            variant={mode === item.value ? "primary" : "outline"}
            aria-pressed={mode === item.value}
            onClick={() => setMode(item.value)}
          >
            {item.label}
          </Button>
        ))}
      </div>
      <DataTable
        caption="Danh sách căn hộ mẫu"
        columns={columns}
        query={query}
        page={page}
        size={size}
        sort={sort}
        onPageChange={setPage}
        onSizeChange={(next) => setSize(next as PageSize)}
        onSortChange={setSort}
        getRowId={(row) => row.maCanHo}
      />
    </DemoGroup>
  );
}
