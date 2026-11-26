"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CircleAlert, Plus } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ChonLoc, OTimKiem } from "@/components/data/bo-loc";
import { DataTable } from "@/components/data/data-table";
import { dataTableColumns } from "@/components/data/data-table-columns";
import { StatusBadge } from "@/components/data/status-badge";
import { parseTableParams } from "@/components/data/table-params";
import { useCapNhatUrl } from "@/components/data/use-cap-nhat-url";
import { EmptyState } from "@/components/feedback/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { EMPTY_VALUE, formatNgayGio } from "@/lib/format";
import { TONE_TRANG_THAI, TRANG_THAI, type TaiKhoanTomTat, type TrangThaiTaiKhoan } from "@/lib/tai-khoan";
import { NHAN_VAI_TRO, THU_TU_VAI_TRO, type VaiTro } from "@/lib/vai-tro";

/** Bộ lọc nằm trên URL (spec 0008, AC-17); thiếu là không lọc. */
const THAM_SO_LOC = ["q", "vaiTro", "trangThai"] as const;
type BoLoc = Partial<Record<(typeof THAM_SO_LOC)[number], string>>;

const col = dataTableColumns<TaiKhoanTomTat>();
const columns = col.columns([
  col.accessor("hoTen", {
    header: "Họ tên",
    cell: (info) => (
      <span className="flex flex-col">
        <Link href={`/tai-khoan/${info.row.original.maNguoiDung}`}>{info.getValue()}</Link>
        <span className="text-xs text-muted-foreground">{info.row.original.maNguoiDung}</span>
      </span>
    ),
  }),
  col.accessor("soDienThoai", { header: "Số điện thoại" }),
  col.accessor("email", { header: "Email", cell: (info) => <span className="break-all">{info.getValue()}</span> }),
  col.accessor("vaiTro", {
    header: "Vai trò",
    cell: (info) =>
      info
        .getValue()
        .map((v) => NHAN_VAI_TRO[v as VaiTro])
        .join(", ") || EMPTY_VALUE,
  }),
  col.accessor("trangThaiTaiKhoan", {
    header: "Trạng thái",
    cell: (info) => (
      <span className="flex flex-wrap gap-1">
        <StatusBadge tone={TONE_TRANG_THAI[info.getValue()]}>{info.getValue()}</StatusBadge>
        {info.row.original.phaiDoiMatKhau && <StatusBadge tone="info">Chờ đổi mật khẩu</StatusBadge>}
      </span>
    ),
  }),
  col.accessor("dangNhapCuoiLuc", {
    header: "Đăng nhập gần nhất",
    cell: (info) => <span className="whitespace-nowrap">{formatNgayGio(info.getValue())}</span>,
  }),
]);

function docBoLoc(searchParams: URLSearchParams): BoLoc {
  const boLoc: BoLoc = {};
  for (const ten of THAM_SO_LOC) {
    const giaTri = searchParams.get(ten);
    if (giaTri) boLoc[ten] = giaTri;
  }
  return boLoc;
}

/**
 * Danh sách tài khoản (spec 0008, AC-5, AC-17) theo mẫu Bảng danh sách của `design.md`. Ô tìm gửi sau 300 ms ngừng
 * gõ hoặc khi nhấn Enter; mọi bộ lọc và trang nằm trên URL. BQL không có ô lọc vai trò.
 */
export function TaiKhoanView() {
  const searchParams = useSearchParams();
  const { page, size } = parseTableParams(searchParams);
  const boLoc = docBoLoc(searchParams);
  const capNhatUrl = useCapNhatUrl(searchParams);
  const laAdmin = useHoSo().data?.vaiTro.includes("QUAN_TRI_VIEN") ?? false;

  const query = useQuery<PageResponse<TaiKhoanTomTat>, ApiError>({
    queryKey: ["tai-khoan", "danh-sach", page, size, boLoc],
    queryFn: () =>
      api<PageResponse<TaiKhoanTomTat>>(
        client.GET("/api/v1/accounts", { params: { query: { page, size, ...boLoc } } }),
      ),
    placeholderData: keepPreviousData,
  });

  function loc(ten: (typeof THAM_SO_LOC)[number], giaTri: string | undefined) {
    capNhatUrl({ [ten]: giaTri || undefined, page: undefined }, { themLichSu: true });
  }

  const dangLoc = THAM_SO_LOC.some((ten) => boLoc[ten]);
  const loiBoLoc = query.error?.errorCode === "VALIDATION_FAILED" ? query.error : null;

  return (
    <>
      <PageHeader
        title="Tài khoản"
        description={
          laAdmin
            ? "Tạo, tìm và quản lý tài khoản của nhân viên và cư dân."
            : "Tạo, tìm và quản lý tài khoản của cư dân."
        }
        actions={
          <Button asChild>
            <Link href="/tai-khoan/moi">
              <Plus aria-hidden="true" />
              Tạo tài khoản
            </Link>
          </Button>
        }
      />
      <div className="flex flex-col gap-4">
        <div
          role="search"
          aria-label="Lọc tài khoản"
          className="grid gap-4 rounded-md border bg-card p-4 sm:grid-cols-2 lg:grid-cols-4"
        >
          <OTimKiem
            nhan="Tìm tài khoản"
            goiY="Họ tên (gõ không dấu được), số điện thoại, email hoặc mã"
            className="sm:col-span-2 lg:col-span-1"
            giaTri={boLoc.q ?? ""}
            onTim={(q) => loc("q", q.trim())}
          />
          {laAdmin && (
            <ChonLoc
              nhan="Vai trò"
              giaTri={boLoc.vaiTro}
              tatCa="Tất cả vai trò"
              luaChon={THU_TU_VAI_TRO.map((v) => ({ value: v, label: NHAN_VAI_TRO[v] }))}
              onChon={(v) => loc("vaiTro", v)}
            />
          )}
          <ChonLoc
            nhan="Trạng thái"
            giaTri={boLoc.trangThai}
            tatCa="Tất cả trạng thái"
            luaChon={TRANG_THAI.map((t: TrangThaiTaiKhoan) => ({ value: t, label: t }))}
            onChon={(v) => loc("trangThai", v)}
          />
          {dangLoc && (
            <div className="flex items-end">
              <Button
                type="button"
                variant="outline"
                onClick={() =>
                  capNhatUrl(
                    { q: undefined, vaiTro: undefined, trangThai: undefined, page: undefined },
                    { themLichSu: true },
                  )
                }
              >
                Xóa bộ lọc
              </Button>
            </div>
          )}
        </div>
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
            </AlertDescription>
          </Alert>
        ) : (
          <DataTable
            caption="Danh sách tài khoản"
            hideCaption
            columns={columns}
            query={query}
            page={page}
            size={size}
            onPageChange={(next) => capNhatUrl({ page: next === 1 ? undefined : String(next) })}
            onSizeChange={(next) => capNhatUrl({ size: String(next) })}
            // Danh sách chỉ sắp theo mới tạo trước (AC-5), không cột nào bấm sắp xếp được
            onSortChange={() => undefined}
            emptyState={
              dangLoc ? (
                <EmptyState
                  title="Không có tài khoản nào khớp bộ lọc"
                  description="Thử từ khóa khác hoặc bỏ bớt điều kiện lọc."
                />
              ) : (
                <EmptyState title="Chưa có tài khoản nào" />
              )
            }
            getRowId={(row) => row.maNguoiDung}
          />
        )}
      </div>
    </>
  );
}
