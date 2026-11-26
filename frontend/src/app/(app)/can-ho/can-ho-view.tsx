"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CircleAlert } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { StatusBadge } from "@/components/data/status-badge";
import { parseTableParams, toSortParam, type TableSort } from "@/components/data/table-params";
import { useCapNhatUrl } from "@/components/data/use-cap-nhat-url";
import { EmptyState } from "@/components/feedback/empty-state";
import { ErrorState } from "@/components/feedback/error-state";
import { PageHeader } from "@/components/layout/page-header";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { CAN_HO_QUERY_KEY, duocQuanLyCanHo, formatDienTich, toneTrangThai, type CanHo } from "@/lib/can-ho";
import { formatSo } from "@/lib/format";
import { TOA_NHA_QUERY_KEY, type ToaNha } from "@/lib/toa-nha";
import { BoLocCanHo, dangLoc, docBoLoc, THAM_SO_LOC, type BoLoc } from "./bo-loc-can-ho";
import { ThemCanHoDialog } from "./them-can-ho-dialog";

const col = dataTableColumns<CanHo>();
const columns = col.columns([
  col.accessor("maCanHo", {
    header: "Mã căn hộ",
    meta: { sortField: "maCanHo" },
    cell: (info) => <Link href={`/can-ho/${info.getValue()}`}>{info.getValue()}</Link>,
  }),
  col.accessor("tenToaNha", { header: "Tòa nhà" }),
  col.accessor("soTang", {
    header: "Tầng",
    meta: { align: "right" },
    cell: (info) => formatSo(info.getValue()),
  }),
  col.accessor("dienTich", {
    header: "Diện tích",
    meta: { sortField: "dienTich", align: "right" },
    cell: (info) => <span className="whitespace-nowrap tabular-nums">{formatDienTich(info.getValue())}</span>,
  }),
  col.accessor("trangThaiSuDung", {
    header: "Trạng thái",
    meta: { sortField: "trangThaiSuDung" },
    cell: (info) => <TrangThaiCanHo canHo={info.row.original} />,
  }),
]);

/** Badge trạng thái ghi đúng chuỗi DB, kèm nhãn "Tạm dừng tiện ích" khi cờ bật (AC-17). */
export function TrangThaiCanHo({ canHo }: { canHo: CanHo }) {
  return (
    <span className="flex flex-wrap gap-1.5">
      <StatusBadge tone={toneTrangThai(canHo.trangThaiSuDung)}>{canHo.trangThaiSuDung}</StatusBadge>
      {canHo.biTamDungTienIch && <StatusBadge tone="danger">Tạm dừng tiện ích</StatusBadge>}
    </span>
  );
}

/**
 * Tìm căn hộ (spec 0012, AC-17, AC-18) theo mẫu Bảng danh sách của `design.md`. Bộ lọc, `page`, `size`, `sort` nằm
 * trên URL với đúng tên tham số của API; giá trị mặc định không ghi lên URL; đổi bộ lọc thì về trang 1.
 */
export function CanHoView() {
  const vaiTro = useHoSo().data?.vaiTro;
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const boLoc = docBoLoc(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);

  const toaNha = useQuery<PageResponse<ToaNha>, ApiError>({
    queryKey: [...TOA_NHA_QUERY_KEY, "lua-chon"],
    queryFn: () =>
      api<PageResponse<ToaNha>>(
        client.GET("/api/v1/buildings", { params: { query: { size: 100, sort: "tenToaNha,asc" } } }),
      ),
  });
  const query = useQuery<PageResponse<CanHo>, ApiError>({
    queryKey: [...CAN_HO_QUERY_KEY, "danh-sach", page, size, sort, boLoc],
    queryFn: () =>
      api<PageResponse<CanHo>>(
        client.GET("/api/v1/apartments", {
          params: {
            query: {
              page,
              size,
              sort: toSortParam(sort),
              maToaNha: boLoc.maToaNha,
              // Giữ nguyên chữ trên URL (kể cả khi sai) để server trả lỗi đúng ô; kiểu sinh ra ghi số
              soTang: boLoc.soTang as unknown as number | undefined,
              dienTichTu: boLoc.dienTichTu as unknown as number | undefined,
              dienTichDen: boLoc.dienTichDen as unknown as number | undefined,
              trangThaiSuDung: boLoc.trangThaiSuDung.length
                ? (boLoc.trangThaiSuDung as CanHo["trangThaiSuDung"][])
                : undefined,
              q: boLoc.q,
              biTamDungTienIch: boLoc.biTamDungTienIch ? boLoc.biTamDungTienIch === "true" : undefined,
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });

  function apDung(moi: BoLoc) {
    capNhatUrl(
      {
        ...Object.fromEntries(THAM_SO_LOC.map((ten) => [ten, moi[ten]])),
        page: undefined,
      },
      { themLichSu: true },
    );
  }
  const xoaHet = () =>
    capNhatUrl(
      { ...Object.fromEntries(THAM_SO_LOC.map((ten) => [ten, undefined])), page: undefined, sort: undefined },
      { themLichSu: true },
    );

  const loi = query.error;
  const loiBoLoc = loi?.errorCode === "VALIDATION_FAILED" ? loi : null;

  let noiDung;
  if (loi?.errorCode === "NOT_FOUND") {
    noiDung = (
      <ErrorState
        headingLevel="h2"
        title="Không tìm thấy tòa nhà"
        description="Tòa nhà trong bộ lọc không tồn tại hoặc nằm ngoài phạm vi bạn phụ trách."
        actions={
          <Button type="button" variant="outline" onClick={xoaHet}>
            Xóa bộ lọc
          </Button>
        }
      />
    );
  } else if (loiBoLoc) {
    noiDung = (
      <Alert tone="danger">
        <CircleAlert aria-hidden />
        <AlertTitle>Bộ lọc trên địa chỉ trang không hợp lệ</AlertTitle>
        <AlertDescription>
          <p>Sửa các ô được đánh dấu ở trên rồi bấm Lọc, hoặc xóa bộ lọc.</p>
          <ul className="list-disc pl-5">
            {loiBoLoc.errors
              .filter((e) => !e.field || !(THAM_SO_LOC as readonly string[]).includes(e.field))
              .map((e) => (
                <li key={`${e.field}-${e.message}`}>{e.message}</li>
              ))}
          </ul>
          <Button type="button" variant="outline" size="sm" className="mt-3" onClick={xoaHet}>
            Xóa bộ lọc
          </Button>
        </AlertDescription>
      </Alert>
    );
  } else {
    noiDung = (
      <DataTable
        caption="Danh sách căn hộ"
        hideCaption
        columns={columns}
        query={query}
        page={page}
        size={size}
        sort={sort}
        onPageChange={(next) => capNhatUrl({ page: next === 1 ? undefined : String(next) })}
        onSizeChange={(next) => capNhatUrl({ size: next === 20 ? undefined : String(next) })}
        onSortChange={(next?: TableSort) =>
          capNhatUrl({ sort: next?.field === "maCanHo" && next.dir === "asc" ? undefined : toSortParam(next) })
        }
        emptyState={
          dangLoc(boLoc) ? (
            <EmptyState
              title="Không có căn hộ nào khớp bộ lọc"
              description="Thử nới khoảng diện tích hoặc bỏ bớt điều kiện lọc."
            />
          ) : (
            <EmptyState title="Chưa có căn hộ nào" />
          )
        }
        getRowId={(row) => row.maCanHo}
      />
    );
  }

  return (
    <>
      <PageHeader
        title="Căn hộ"
        description="Tìm căn hộ theo tòa, tầng, diện tích và trạng thái sử dụng."
        actions={duocQuanLyCanHo(vaiTro) ? <ThemCanHoDialog toaNha={toaNha.data?.items ?? []} /> : undefined}
      />
      <div className="flex flex-col gap-4">
        <BoLocCanHo
          key={THAM_SO_LOC.map((ten) => String(boLoc[ten] ?? "")).join("|")}
          boLoc={boLoc}
          toaNha={toaNha.data?.items ?? []}
          loiServer={loiBoLoc}
          onApDung={apDung}
        />
        {noiDung}
      </div>
    </>
  );
}
