"use client";

import { StatusBadge } from "@/components/data/status-badge";
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { toneTrangThaiHoSo, toneVaiTroCuTru, type HoSoCuTru } from "@/lib/cu-tru";
import { EMPTY_VALUE, formatNgay } from "@/lib/format";
import { DuyetDeNghiDialog } from "./duyet-de-nghi-dialog";
import { HuyDeNghiDialog } from "./huy-de-nghi-dialog";
import { TuChoiDeNghiDialog } from "./tu-choi-de-nghi-dialog";

type BangDeNghiCuTruProps = {
  danhSach: readonly HoSoCuTru[];
  /** BQL xem đủ PII, người xác minh và nút Duyệt/Từ chối; cư dân chỉ thấy người gửi, lý do từ chối và nút Hủy đề nghị của chính mình (spec 0015, AC-8, AC-15). */
  laBql: boolean;
};

/** Bảng danh sách đề nghị cư trú của căn hộ trong tab "Đề nghị cư trú" (spec 0015, AC-15, AC-18). */
export function BangDeNghiCuTru({ danhSach, laBql }: BangDeNghiCuTruProps) {
  const hienThaoTac = laBql
    ? danhSach.some((d) => d.trangThaiHoSo === "Chờ xác minh")
    : danhSach.some((d) => d.coTheHuy);

  return (
    <div className="overflow-x-auto rounded-md border bg-card">
      <Table>
        <TableCaption className="sr-only">Danh sách đề nghị cư trú của căn hộ</TableCaption>
        <TableHeader>
          <TableRow>
            <TableHead scope="col">Người được đề nghị</TableHead>
            <TableHead scope="col">Vai trò cư trú</TableHead>
            <TableHead scope="col">Tình trạng</TableHead>
            <TableHead scope="col">Thời gian cư trú</TableHead>
            <TableHead scope="col">Người gửi và ghi chú</TableHead>
            <TableHead scope="col">Trạng thái</TableHead>
            {laBql && <TableHead scope="col">Người xác minh</TableHead>}
            {hienThaoTac && <TableHead scope="col">Thao tác</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {danhSach.map((hoSo) => {
            const tenCuDan = hoSo.hoTen ?? hoSo.maCuDan;
            const nguoiGui = hoSo.hoTenNguoiDeNghi ?? hoSo.maCuDanDeNghi ?? EMPTY_VALUE;
            const nguoiXacMinh = hoSo.hoTenNguoiXacMinh ?? hoSo.maNvbqlXacMinh ?? EMPTY_VALUE;
            const choXuLyBql = laBql && hoSo.trangThaiHoSo === "Chờ xác minh";
            const choHuyCuDan = !laBql && hoSo.coTheHuy;

            return (
              <TableRow key={hoSo.maHoSo}>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium text-foreground">{tenCuDan}</span>
                    {laBql && (
                      <span className="type-caption text-muted-foreground">
                        {hoSo.maCuDan} · SĐT: {hoSo.soDienThoai ?? EMPTY_VALUE} · CCCD: {hoSo.soCccd ?? EMPTY_VALUE}
                      </span>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <StatusBadge tone={toneVaiTroCuTru(hoSo.vaiTroCuTru)}>{hoSo.vaiTroCuTru}</StatusBadge>
                </TableCell>
                <TableCell>{hoSo.tinhTrangCuTru}</TableCell>
                <TableCell className="whitespace-nowrap">
                  {formatNgay(hoSo.ngayBatDau)} đến {hoSo.ngayKetThuc ? formatNgay(hoSo.ngayKetThuc) : "Không thời hạn"}
                </TableCell>
                <TableCell>
                  <div className="flex flex-col gap-0.5">
                    <span>{nguoiGui}</span>
                    {hoSo.ghiChuDeNghi && (
                      <span className="type-caption text-muted-foreground">Ghi chú: {hoSo.ghiChuDeNghi}</span>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col items-start gap-1">
                    <StatusBadge tone={toneTrangThaiHoSo(hoSo.trangThaiHoSo)}>{hoSo.trangThaiHoSo}</StatusBadge>
                    {hoSo.trangThaiHoSo === "Từ chối" && hoSo.lyDoTuChoi && (
                      <span className="type-caption text-destructive">Lý do từ chối: {hoSo.lyDoTuChoi}</span>
                    )}
                  </div>
                </TableCell>
                {laBql && <TableCell>{nguoiXacMinh}</TableCell>}
                {hienThaoTac && (
                  <TableCell>
                    <div className="flex flex-wrap items-center gap-2">
                      {choXuLyBql && (
                        <>
                          <DuyetDeNghiDialog hoSo={hoSo} />
                          <TuChoiDeNghiDialog hoSo={hoSo} />
                        </>
                      )}
                      {choHuyCuDan && <HuyDeNghiDialog hoSo={hoSo} />}
                    </div>
                  </TableCell>
                )}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
