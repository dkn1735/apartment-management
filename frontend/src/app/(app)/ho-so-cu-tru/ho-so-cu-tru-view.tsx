"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CircleAlert, ClipboardCheck } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { cn } from "cn";
import { DuyetDeNghiDialog } from "@/app/(app)/can-ho/duyet-de-nghi-dialog";
import { KetThucCuTruDialog } from "@/app/(app)/can-ho/ket-thuc-cu-tru-dialog";
import { SuaHoSoCuTruDialog } from "@/app/(app)/can-ho/sua-ho-so-cu-tru-dialog";
import { ThemHoSoCuTruDialog } from "@/app/(app)/can-ho/them-ho-so-cu-tru-dialog";
import { TuChoiDeNghiDialog } from "@/app/(app)/can-ho/tu-choi-de-nghi-dialog";
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
import {
  coTheSuaHoSoCuTru,
  HO_SO_CU_TRU_QUERY_KEY,
  laHoSoChuaBatDau,
  toneTrangThaiHoSo,
  toneVaiTroCuTru,
  type HoSoCuTru,
  type TinhTrangCuTru,
  type TrangThaiHoSo,
  type VaiTroCuTru,
} from "@/lib/cu-tru";
import { EMPTY_VALUE, formatNgay, formatNgayGio, formatSo, ngayHomNay } from "@/lib/format";
import { TOA_NHA_QUERY_KEY, type ToaNha } from "@/lib/toa-nha";
import {
  BoLocHoSoCuTru,
  dangLocHoSoCuTru,
  docBoLocHoSoCuTru,
  THAM_SO_LOC_CU_TRU,
  type BoLocHoSoCuTruValues,
} from "./bo-loc-ho-so-cu-tru";

const SORT_MAC_DINH: TableSort = { field: "taoLuc", dir: "desc" };

const col = dataTableColumns<HoSoCuTru>();
const columns = col.columns([
  col.accessor("maHoSo", {
    header: "Mã hồ sơ",
    cell: (info) => <span className="font-medium whitespace-nowrap">{info.getValue()}</span>,
  }),
  col.accessor("maCanHo", {
    header: "Căn hộ",
    meta: { sortField: "maCanHo" },
    cell: (info) => (
      <Link href={`/can-ho/${info.getValue()}`} className="whitespace-nowrap">
        {info.getValue()}
      </Link>
    ),
  }),
  col.accessor("hoTen", {
    header: "Cư dân",
    cell: (info) => {
      const h = info.row.original;
      return (
        <div className="flex flex-col gap-0.5">
          <span className="font-medium text-foreground">{h.hoTen ?? h.maCuDan}</span>
          <span className="type-caption text-muted-foreground">
            {h.maCuDan} · SĐT: {h.soDienThoai ?? EMPTY_VALUE} · CCCD: {h.soCccd ?? EMPTY_VALUE}
          </span>
        </div>
      );
    },
  }),
  col.accessor("vaiTroCuTru", {
    header: "Vai trò và tình trạng",
    cell: (info) => {
      const h = info.row.original;
      return (
        <div className="flex flex-col items-start gap-1">
          <StatusBadge tone={toneVaiTroCuTru(h.vaiTroCuTru)}>{h.vaiTroCuTru}</StatusBadge>
          <span className="type-caption text-muted-foreground">{h.tinhTrangCuTru}</span>
        </div>
      );
    },
  }),
  col.accessor("ngayBatDau", {
    header: "Thời gian cư trú",
    meta: {
      sortOptions: [
        { field: "ngayBatDau", label: "Ngày bắt đầu" },
        { field: "ngayKetThuc", label: "Ngày kết thúc" },
      ],
    },
    cell: (info) => {
      const h = info.row.original;
      return (
        <span className="whitespace-nowrap">
          {formatNgay(h.ngayBatDau)} đến {h.ngayKetThuc ? formatNgay(h.ngayKetThuc) : "Không thời hạn"}
        </span>
      );
    },
  }),
  col.accessor("trangThaiHoSo", {
    header: "Trạng thái",
    cell: (info) => {
      const h = info.row.original;
      const nguoiGui = h.hoTenNguoiDeNghi ?? h.maCuDanDeNghi;
      const nguoiXacMinh = h.hoTenNguoiXacMinh ?? h.maNvbqlXacMinh;
      return (
        <div className="flex min-w-48 flex-col items-start gap-1">
          <StatusBadge tone={toneTrangThaiHoSo(h.trangThaiHoSo)}>{h.trangThaiHoSo}</StatusBadge>
          {nguoiGui && <span className="type-caption text-muted-foreground">Người gửi: {nguoiGui}</span>}
          {h.ghiChuDeNghi && <span className="type-caption text-muted-foreground">Ghi chú: {h.ghiChuDeNghi}</span>}
          {h.trangThaiHoSo === "Từ chối" && h.lyDoTuChoi && (
            <span className="type-caption text-destructive">Lý do từ chối: {h.lyDoTuChoi}</span>
          )}
          {nguoiXacMinh && <span className="type-caption text-muted-foreground">Xác minh: {nguoiXacMinh}</span>}
        </div>
      );
    },
  }),
  col.accessor("taoLuc", {
    header: "Tạo lúc",
    meta: { sortField: "taoLuc" },
    cell: (info) => <span className="whitespace-nowrap">{formatNgayGio(info.getValue())}</span>,
  }),
  col.accessor("capNhatLuc", {
    header: "Cập nhật lúc",
    meta: { sortField: "capNhatLuc" },
    cell: (info) => <span className="whitespace-nowrap">{formatNgayGio(info.getValue())}</span>,
  }),
  col.display({
    id: "hanhDong",
    header: "Hành động",
    cell: (info) => <HanhDongDongHoSo hoSo={info.row.original} />,
  }),
]);

function HanhDongDongHoSo({ hoSo }: { hoSo: HoSoCuTru }) {
  const homNay = ngayHomNay();
  if (hoSo.trangThaiHoSo === "Chờ xác minh") {
    return (
      <div className="flex flex-wrap items-center gap-2">
        <DuyetDeNghiDialog hoSo={hoSo} />
        <TuChoiDeNghiDialog hoSo={hoSo} />
      </div>
    );
  }
  if (hoSo.trangThaiHoSo === "Hợp lệ") {
    const choSua = coTheSuaHoSoCuTru(hoSo, homNay);
    const laHuySom = laHoSoChuaBatDau(hoSo, homNay);
    return (
      <div className="flex flex-wrap items-center gap-2">
        {choSua && <SuaHoSoCuTruDialog hoSo={hoSo} />}
        <KetThucCuTruDialog hoSo={hoSo} laHuySom={laHuySom} />
      </div>
    );
  }
  return <span className="text-muted-foreground">{EMPTY_VALUE}</span>;
}

/**
 * Màn hình tập trung tra cứu hồ sơ cư trú toàn tòa và xử lý đề nghị chờ xác minh cho BQL (spec 0015, AC-16, AC-17, AC-18).
 */
export function HoSoCuTruView() {
  const searchParams = useSearchParams();
  const { page, size, sort } = parseTableParams(searchParams);
  const sortHieuLuc = sort ?? SORT_MAC_DINH;
  const boLoc = docBoLocHoSoCuTru(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);

  const toaNha = useQuery<PageResponse<ToaNha>, ApiError>({
    queryKey: [...TOA_NHA_QUERY_KEY, "lua-chon"],
    queryFn: () =>
      api<PageResponse<ToaNha>>(
        client.GET("/api/v1/buildings", { params: { query: { size: 100, sort: "tenToaNha,asc" } } }),
      ),
  });

  const demChoXacMinh = useQuery<PageResponse<HoSoCuTru>, ApiError>({
    queryKey: [...HO_SO_CU_TRU_QUERY_KEY, "dem-cho-xac-minh"],
    queryFn: () =>
      api<PageResponse<HoSoCuTru>>(
        client.GET("/api/v1/residency-records", {
          params: { query: { trangThaiHoSo: ["Chờ xác minh"], size: 1 } },
        }),
      ),
  });

  const query = useQuery<PageResponse<HoSoCuTru>, ApiError>({
    queryKey: [...HO_SO_CU_TRU_QUERY_KEY, "danh-sach", page, size, sort, boLoc],
    queryFn: () =>
      api<PageResponse<HoSoCuTru>>(
        client.GET("/api/v1/residency-records", {
          params: {
            query: {
              page,
              size,
              sort: toSortParam(sort),
              maToaNha: boLoc.maToaNha,
              // Giữ nguyên chuỗi trên URL (kể cả khi sai) để server trả lỗi 400 đúng trường
              soTang: boLoc.soTang as unknown as number | undefined,
              trangThaiHoSo: boLoc.trangThaiHoSo.length ? (boLoc.trangThaiHoSo as TrangThaiHoSo[]) : undefined,
              vaiTroCuTru: boLoc.vaiTroCuTru as VaiTroCuTru | undefined,
              tinhTrangCuTru: boLoc.tinhTrangCuTru as TinhTrangCuTru | undefined,
              q: boLoc.q,
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });

  function apDung(moi: BoLocHoSoCuTruValues) {
    capNhatUrl(
      {
        ...Object.fromEntries(
          THAM_SO_LOC_CU_TRU.map((ten) => [
            ten,
            ten === "trangThaiHoSo" ? (moi.trangThaiHoSo.length > 0 ? moi.trangThaiHoSo : undefined) : moi[ten],
          ]),
        ),
        page: undefined,
      },
      { themLichSu: true },
    );
  }

  function chonLocNhanh(cheDo: "tat-ca" | "cho-xac-minh") {
    capNhatUrl(
      {
        trangThaiHoSo: cheDo === "cho-xac-minh" ? ["Chờ xác minh"] : undefined,
        page: undefined,
      },
      { themLichSu: true },
    );
  }

  function doiSapXep(next?: TableSort) {
    if (!next) {
      // Bấm lại cột mặc định đang giảm (`taoLuc,desc`) thì đảo sang tăng (`taoLuc,asc`); cột khác thì về mặc định
      if (sortHieuLuc.field === SORT_MAC_DINH.field && sortHieuLuc.dir === SORT_MAC_DINH.dir) {
        capNhatUrl({ sort: "taoLuc,asc" });
      } else {
        capNhatUrl({ sort: undefined });
      }
      return;
    }
    const laMacDinh = next.field === SORT_MAC_DINH.field && next.dir === SORT_MAC_DINH.dir;
    capNhatUrl({ sort: laMacDinh ? undefined : toSortParam(next) });
  }

  const xoaHet = () =>
    capNhatUrl(
      { ...Object.fromEntries(THAM_SO_LOC_CU_TRU.map((ten) => [ten, undefined])), page: undefined, sort: undefined },
      { themLichSu: true },
    );

  const soChoXacMinh = demChoXacMinh.data?.totalItems ?? 0;
  const sangTatCa = boLoc.trangThaiHoSo.length === 0;
  const sangChoXacMinh = boLoc.trangThaiHoSo.length === 1 && boLoc.trangThaiHoSo[0] === "Chờ xác minh";

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
              .filter((e) => !e.field || !(THAM_SO_LOC_CU_TRU as readonly string[]).includes(e.field))
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
        caption="Danh sách hồ sơ cư trú"
        hideCaption
        columns={columns}
        query={query}
        page={page}
        size={size}
        sort={sortHieuLuc}
        onPageChange={(next) => capNhatUrl({ page: next === 1 ? undefined : String(next) })}
        onSizeChange={(next) => capNhatUrl({ size: next === 20 ? undefined : String(next) })}
        onSortChange={doiSapXep}
        emptyState={
          dangLocHoSoCuTru(boLoc) ? (
            <EmptyState
              icon={ClipboardCheck}
              title="Không có hồ sơ cư trú nào khớp bộ lọc"
              description="Thử từ khóa khác hoặc bỏ bớt điều kiện lọc."
            />
          ) : (
            <EmptyState
              icon={ClipboardCheck}
              title="Chưa có hồ sơ cư trú nào"
              description="Bấm Thêm hồ sơ cư trú ở góc trên để ghi nhận cư dân vào căn hộ."
            />
          )
        }
        getRowId={(row) => row.maHoSo}
      />
    );
  }

  const khoaBoLoc = THAM_SO_LOC_CU_TRU.map((ten) =>
    ten === "trangThaiHoSo" ? boLoc.trangThaiHoSo.join(",") : String(boLoc[ten] ?? ""),
  ).join("|");

  return (
    <>
      <PageHeader
        title="Hồ sơ cư trú"
        description="Tra cứu hồ sơ cư trú toàn tòa và xử lý đề nghị chờ xác minh."
        actions={<ThemHoSoCuTruDialog toaNha={toaNha.data?.items} />}
      />
      <div className="flex flex-col gap-4">
        <div
          role="group"
          aria-label="Lọc nhanh trạng thái hồ sơ"
          className="inline-flex w-fit flex-wrap items-center gap-1 rounded-md border bg-card p-1"
        >
          <button
            type="button"
            aria-pressed={sangTatCa}
            onClick={() => chonLocNhanh("tat-ca")}
            className={cn(
              "inline-flex h-9 cursor-pointer items-center rounded-sm px-3.5 type-ui transition-colors duration-150 ease-out",
              sangTatCa ? "bg-primary font-medium text-primary-foreground" : "text-foreground hover:bg-accent",
            )}
          >
            Tất cả hồ sơ
          </button>
          <button
            type="button"
            aria-pressed={sangChoXacMinh}
            onClick={() => chonLocNhanh("cho-xac-minh")}
            className={cn(
              "inline-flex h-9 cursor-pointer items-center rounded-sm px-3.5 type-ui transition-colors duration-150 ease-out",
              sangChoXacMinh ? "bg-primary font-medium text-primary-foreground" : "text-foreground hover:bg-accent",
            )}
          >
            {soChoXacMinh > 0 ? `Chờ xác minh (${formatSo(soChoXacMinh)})` : "Chờ xác minh"}
          </button>
        </div>
        <BoLocHoSoCuTru
          key={khoaBoLoc}
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
